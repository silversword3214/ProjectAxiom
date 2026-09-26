package silversword.axiom.client.rendersystem.axiomrenderer.engine;

import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.minecraft.resources.Identifier;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Verteksidata kirjoitetaan suoraan natiivijärjestyksessä olevaan ByteBufferiin,
 * joka vastaa täsmälleen GPU-formaatin layoutia. Näin vältetään float[]-välivaihe
 * ja toinen kopio upload-polulla.
 *
 * Formaatit:
 *   POS2_COLOR / POS3_COLOR : Position(RGB32F=12) + Color(RGBA8=4) = 16 B
 *   POS2_UV_COLOR           : Position(12) + UV0(RG32F=8) + Color(4) = 24 B
 */
public class Batch {

    private static final int INITIAL_CAPACITY_BYTES = 16 * 1024;

    private final VertexFormat format;
    private final PrimitiveTopology mode;
    private final boolean hasUv;
    private final int vertexBytes;

    private ByteBuffer data;
    private int vertexCount = 0;

    private Identifier texture;
    private GpuSampler sampler;

    public Batch(VertexFormat format, PrimitiveTopology mode) {
        this.format = format;
        this.mode = mode;
        this.hasUv = (format == AxiomVertexFormats.POS2_UV_COLOR);
        this.vertexBytes = format.getVertexSize();
        this.data = ByteBuffer.allocateDirect(INITIAL_CAPACITY_BYTES)
                .order(ByteOrder.nativeOrder());
    }

    // ---------- Kyselyt ----------
    public VertexFormat getFormat() { return format; }
    public PrimitiveTopology getMode() { return mode; }
    public int vertexCount() { return vertexCount; }
    public boolean hasUv() { return hasUv; }
    public int byteCount() { return vertexCount * vertexBytes; }
    public int vertexBytes() { return vertexBytes; }

    /** Suora pääsy bufferiin. Kutsu flip() ennen lukua. */
    public ByteBuffer data() { return data; }

    public Identifier getTexture() { return texture; }
    public void setTexture(Identifier texture) { this.texture = texture; }
    public GpuSampler getSampler() { return sampler; }
    public void setSampler(GpuSampler sampler) { this.sampler = sampler; }

    // ---------- Verteksien lisäys ----------
    public void vertex(float x, float y, float z, float r, float g, float b, float a) {
        if (hasUv) throw new IllegalStateException("Wrong vertex format, expected POS2_UV_COLOR");
        ensureCapacity(vertexBytes);
        data.putFloat(x);
        data.putFloat(y);
        data.putFloat(z);
        data.put((byte) (r * 255f));
        data.put((byte) (g * 255f));
        data.put((byte) (b * 255f));
        data.put((byte) (a * 255f));
        vertexCount++;
    }

    public void vertex2D(float x, float y, float r, float g, float b, float a) {
        vertex(x, y, 0f, r, g, b, a);
    }

    public void vertexUV(float x, float y, float u, float v,
                         float r, float g, float b, float a) {
        vertexUV(x, y, 0f, u, v, r, g, b, a);
    }

    public void vertexUV(float x, float y, float z, float u, float v,
                         float r, float g, float b, float a) {
        if (!hasUv) throw new IllegalStateException("Wrong vertex format, expected POS2_UV_COLOR");
        ensureCapacity(vertexBytes);
        data.putFloat(x);
        data.putFloat(y);
        data.putFloat(z);
        data.putFloat(u);
        data.putFloat(v);
        data.put((byte) (r * 255f));
        data.put((byte) (g * 255f));
        data.put((byte) (b * 255f));
        data.put((byte) (a * 255f));
        vertexCount++;
    }

    private void ensureCapacity(int additional) {
        if (data.remaining() >= additional) return;
        int newCap = data.capacity();
        int needed = data.position() + additional;
        while (newCap < needed) newCap <<= 1;

        ByteBuffer newBuf = ByteBuffer.allocateDirect(newCap).order(ByteOrder.nativeOrder());
        data.flip();
        newBuf.put(data);
        this.data = newBuf;
    }

    /** Nollaa positionin, limiitin ja tilan. Kutsutaan ennen pool-palautusta. */
    public void clear() {
        data.clear();
        vertexCount = 0;
        texture = null;
        sampler = null;
    }

    /** @deprecated API säilytetty yhteensopivuuden vuoksi; allokoi joka kutsulla. */
    @Deprecated
    public List<float[]> getVertices() {
        List<float[]> out = new ArrayList<>(vertexCount);
        ByteBuffer buf = data.duplicate();
        buf.flip();
        int stride = hasUv ? 9 : 7;
        for (int i = 0; i < vertexCount; i++) {
            float[] v = new float[stride];
            v[0] = buf.getFloat();
            v[1] = buf.getFloat();
            v[2] = buf.getFloat();
            if (hasUv) {
                v[3] = buf.getFloat();
                v[4] = buf.getFloat();
                v[5] = (buf.get() & 0xFF) / 255f;
                v[6] = (buf.get() & 0xFF) / 255f;
                v[7] = (buf.get() & 0xFF) / 255f;
                v[8] = (buf.get() & 0xFF) / 255f;
            } else {
                v[3] = (buf.get() & 0xFF) / 255f;
                v[4] = (buf.get() & 0xFF) / 255f;
                v[5] = (buf.get() & 0xFF) / 255f;
                v[6] = (buf.get() & 0xFF) / 255f;
            }
            out.add(v);
        }
        return out;
    }
}