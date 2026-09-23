package silversword.axiom.client.render.font;

import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.GpuFormat;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import org.joml.Matrix3x2f;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.axiomrenderer.core.RenderCore;
import silversword.axiom.client.render.rendersystem.utils.texture.Texture;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.*;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;


public class Font {
    public final Texture texture;
    private final int height;
    private final float scale;
    private final float ascent;
    private final Int2ObjectOpenHashMap<CharData> charMap = new Int2ObjectOpenHashMap<>();
    private static final int size = 2048;

    private static final float LETTER_SPACING = 1f;

    public Font(ByteBuffer buffer, int height) {
        this.height = height;

        STBTTFontinfo fontInfo = STBTTFontinfo.create();
        STBTruetype.stbtt_InitFont(fontInfo, buffer);

        ByteBuffer bitmap = BufferUtils.createByteBuffer(size * size);
        STBTTPackedchar.Buffer[] cdata = {
                STBTTPackedchar.create(95),
                STBTTPackedchar.create(96),
                STBTTPackedchar.create(128),
                STBTTPackedchar.create(144),
                STBTTPackedchar.create(256),
                STBTTPackedchar.create(1)
        };

        STBTTPackContext packContext = STBTTPackContext.create();
        STBTruetype.stbtt_PackBegin(packContext, bitmap, size, size, 0 ,1);

        STBTTPackRange.Buffer packRange = STBTTPackRange.create(cdata.length);
        packRange.put(STBTTPackRange.create().set(height, 32, null, 95, cdata[0], (byte) 2, (byte) 2));
        packRange.put(STBTTPackRange.create().set(height, 160, null, 96, cdata[1], (byte) 2, (byte) 2));
        packRange.put(STBTTPackRange.create().set(height, 256, null, 128, cdata[2], (byte) 2, (byte) 2));
        packRange.put(STBTTPackRange.create().set(height, 880, null, 144, cdata[3], (byte) 2, (byte) 2));
        packRange.put(STBTTPackRange.create().set(height, 1024, null, 256, cdata[4], (byte) 2, (byte) 2));
        packRange.put(STBTTPackRange.create().set(height, 8734, null, 1, cdata[5], (byte) 2, (byte) 2));
        packRange.flip();

        STBTruetype.stbtt_PackFontRanges(packContext, buffer, 0, packRange);
        STBTruetype.stbtt_PackEnd(packContext);

        texture = new Texture(size, size, GpuFormat.R8_UNORM, FilterMode.LINEAR, FilterMode.LINEAR);
        texture.upload(bitmap);
        scale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, height);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer ascent = stack.mallocInt(1);
            STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascent, null, null);
            this.ascent = ascent.get(0);
        }

        for (int i = 0; i < cdata.length; i++) {
            STBTTPackedchar.Buffer cbuf = cdata[i];
            int offset = packRange.get(i).first_unicode_codepoint_in_range();

            for (int j = 0; j < cbuf.capacity(); j++) {
                STBTTPackedchar packedChar = cbuf.get(j);

                float ipw = 1f / size; // pixel width and height
                float iph = 1f / size;

                charMap.put(j + offset, new CharData(
                        packedChar.xoff(),
                        packedChar.yoff(),
                        packedChar.xoff2(),
                        packedChar.yoff2(),
                        packedChar.x0() * ipw,
                        packedChar.y0() * iph,
                        packedChar.x1() * ipw,
                        packedChar.y1() * iph,
                        packedChar.xadvance()
                ));
            }
        }
    }

    public double getWidth(String string, int length) {
        if (length <= 0) return 0;
        double width = 0;
        for (int i = 0; i < length; i++) {
            int cp = string.charAt(i);
            CharData c = charMap.get(cp);
            if (c == null) c = charMap.get(32);
            width += c.xAdvance + LETTER_SPACING;
        }
        // Poistetaan viimeisen merkin jälkeinen ylimääräinen väli,
        // koska sitä ei piirretä näkyviin.
        return width - LETTER_SPACING;
    }

    public double getCharAdvance(int codepoint) {
        CharData c = charMap.get(codepoint);
        if (c == null) c = charMap.get(32);
        return c.xAdvance + LETTER_SPACING;
    }

    public double getAscent() {
        return ascent;
    }

    public double getAscent;

    public int getHeight() {
        return height;
    }

    public double render(RenderCore core, String string, double x, double y, Color color, double scale) {
        y -= 4.0;
        double baselineY = Math.round(y + ascent * this.scale * scale);

        for (int i = 0; i < string.length(); i++) {
            int cp = string.charAt(i);
            CharData c = charMap.get(cp);
            if (c == null) c = charMap.get(32);

            float x0 = (float) Math.round(x + c.x0 * scale);
            float y0 = (float) Math.round(baselineY + c.y0 * scale);
            float x1 = (float) Math.round(x + c.x1 * scale);
            float y1 = (float) Math.round(baselineY + c.y1 * scale);

            core.addTextQuadMesh(
                    texture,
                    x0, y0, x1, y1,
                    c.u0, c.v0, c.u1, c.v1,
                    color.r / 255f, color.g / 255f, color.b / 255f, color.a / 255f
            );

            // KORJAUS: lisää letter-spacing
            x += c.xAdvance * scale + LETTER_SPACING * scale;
        }
        return x;
    }

    public double render(GuiGraphicsExtractor graphics, String string, double x, double y, Color color, double scale) {
        y -= 4.0;
        double baselineY = Math.round(y + ascent * this.scale * scale);

        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        TextureSetup textureSetup = TextureSetup.singleTexture(texture.textureView(), texture.sampler());
        int argb = color.getARGB();

        for (int i = 0; i < string.length(); i++) {
            CharData c = charMap.get(string.charAt(i));
            if (c == null) c = charMap.get(32);

            int x0 = (int) Math.round(x + c.x0 * scale);
            int x1 = (int) Math.round(x + c.x1 * scale);
            int y0 = (int) Math.round(baselineY + c.y0 * scale);
            int y1 = (int) Math.round(baselineY + c.y1 * scale);

            if (x1 > x0 && y1 > y0) {
                graphics.guiRenderState.addGuiElement(new BlitRenderState(
                        RenderPipelines.GUI_TEXT_GRAYSCALE,
                        textureSetup,
                        pose,
                        x0, y0, x1, y1,
                        c.u0, c.u1, c.v0, c.v1,
                        argb,
                        graphics.scissorStack.peek()
                ));
            }
            // KORJAUS: sama letter-spacing
            x += c.xAdvance * scale + LETTER_SPACING * scale;
        }
        return x;
    }

    private record CharData(float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1, float xAdvance) {}
}
