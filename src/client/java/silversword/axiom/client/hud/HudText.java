package silversword.axiom.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.Sheets;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.Renderer2D;

public final class HudText {
    private static boolean drawing = false;

    public static void begin() {
        if (drawing) throw new IllegalStateException("HudText.begin() already called");
        Renderer2D.COLOR.begin();
        Renderer2D.TEXTURE.begin();
        TextRenderer.get().begin(1.0, false, false);
        drawing = true;
    }

    public static void end() {
        if (!drawing) throw new IllegalStateException("HudText.end() without begin()");
        Renderer2D.COLOR.render();

        Minecraft mc = Minecraft.getInstance();
        TextureAtlas guiAtlas = (TextureAtlas) mc.getTextureManager()
                .getTexture(Sheets.GUI_SHEET);
        if (guiAtlas != null) {
            GpuTexture texture = guiAtlas.getTexture();
            GpuTextureView view = RenderSystem.getDevice().createTextureView(texture);
            Renderer2D.TEXTURE.render(view, guiAtlas.getSampler());
        } else {
            Renderer2D.TEXTURE.render();
        }

        TextRenderer.get().end();
        drawing = false;
    }
}