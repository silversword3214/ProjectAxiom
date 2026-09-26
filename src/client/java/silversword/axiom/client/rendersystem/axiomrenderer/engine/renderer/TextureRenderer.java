package silversword.axiom.client.rendersystem.axiomrenderer.engine.renderer;


import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.resources.Identifier;
import silversword.axiom.client.rendersystem.axiomrenderer.engine.Batch;
import silversword.axiom.client.rendersystem.axiomrenderer.engine.RenderCore;

public final class TextureRenderer {

    private final RenderCore core;

    public TextureRenderer(RenderCore core) {
        this.core = core;
    }

    public void texture(Identifier id, float x, float y, float w, float h, int color) {
        texturePart(id, x, y, w, h, 0f, 0f, 1f, 1f, color);
    }

    public void texturePart(Identifier id, float x, float y, float w, float h,
                            float u1, float v1, float u2, float v2, int color) {
        if (w <= 0 || h <= 0) return;

        Batch batch = core.textureBatchFor(id);
        float[] c = unpack(color);

        float x2 = x + w;
        float y2 = y + h;
        batch.vertexUV(x,  y,  u1, v1, c[0], c[1], c[2], c[3]);
        batch.vertexUV(x2, y,  u2, v1, c[0], c[1], c[2], c[3]);
        batch.vertexUV(x,  y2, u1, v2, c[0], c[1], c[2], c[3]);
        batch.vertexUV(x,  y2, u1, v2, c[0], c[1], c[2], c[3]);
        batch.vertexUV(x2, y,  u2, v1, c[0], c[1], c[2], c[3]);
        batch.vertexUV(x2, y2, u2, v2, c[0], c[1], c[2], c[3]);
    }

    public void textureFlippedY(Identifier id, float x, float y, float w, float h, int color) {
        texturePart(id, x, y, w, h, 0f, 1f, 1f, 0f, color);
    }

    public void rotatedTexture(Identifier id, float x, float y,
                               float w, float h, float angleDeg, int color) {
        Batch batch = core.textureBatchFor(id);
        float[] c = unpack(color);

        float cx = x + w / 2;
        float cy = y + h / 2;
        float rad = (float) Math.toRadians(angleDeg);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);
        float hw = w / 2, hh = h / 2;

        float[] xs = {-hw, hw, hw, -hw};
        float[] ys = {-hh, -hh, hh, hh};
        float[] u  = {0, 1, 1, 0};
        float[] v  = {0, 0, 1, 1};

        float[] xr = new float[4];
        float[] yr = new float[4];
        for (int i = 0; i < 4; i++) {
            xr[i] = cx + xs[i] * cos - ys[i] * sin;
            yr[i] = cy + xs[i] * sin + ys[i] * cos;
        }

        batch.vertexUV(xr[0], yr[0], u[0], v[0], c[0], c[1], c[2], c[3]);
        batch.vertexUV(xr[1], yr[1], u[1], v[1], c[0], c[1], c[2], c[3]);
        batch.vertexUV(xr[2], yr[2], u[2], v[2], c[0], c[1], c[2], c[3]);

        batch.vertexUV(xr[0], yr[0], u[0], v[0], c[0], c[1], c[2], c[3]);
        batch.vertexUV(xr[2], yr[2], u[2], v[2], c[0], c[1], c[2], c[3]);
        batch.vertexUV(xr[3], yr[3], u[3], v[3], c[0], c[1], c[2], c[3]);
    }

    public void textureWithPipeline(Identifier id, CompiledRenderPipeline pipeline,
                                    float x, float y, float w, float h,
                                    float u1, float v1, float u2, float v2,
                                    int color) {
        textureWithPipeline(id, pipeline, x, y, w, h, u1, v1, u2, v2, 0f, color);
    }

    public void textureWithPipeline(Identifier id, CompiledRenderPipeline pipeline,
                                    float x, float y, float w, float h,
                                    float u1, float v1, float u2, float v2,
                                    float param, int color) {
        if (w <= 0 || h <= 0) return;

        Batch batch = core.textureBatchFor(id);
        float[] c = unpack(color);

        float x2 = x + w;
        float y2 = y + h;

        batch.vertexUV(x,  y,  param, u1, v1, c[0], c[1], c[2], c[3]);
        batch.vertexUV(x2, y,  param, u2, v1, c[0], c[1], c[2], c[3]);
        batch.vertexUV(x,  y2, param, u1, v2, c[0], c[1], c[2], c[3]);
        batch.vertexUV(x,  y2, param, u1, v2, c[0], c[1], c[2], c[3]);
        batch.vertexUV(x2, y,  param, u2, v1, c[0], c[1], c[2], c[3]);
        batch.vertexUV(x2, y2, param, u2, v2, c[0], c[1], c[2], c[3]);

        core.registerTexturePipeline(id, pipeline);
    }

    public void gpuTexture(GpuTextureView view, GpuSampler sampler,
                           float x, float y, float w, float h,
                           float u0, float v0, float u1, float v1,
                           int color) {
        gpuTextureWithPipeline(RenderCore.uiTextured(), view, sampler,
                x, y, w, h, u0, v0, u1, v1, color);
    }

    public void gpuTextureWithPipeline(CompiledRenderPipeline pipeline,
                                       GpuTextureView view, GpuSampler sampler,
                                       float x, float y, float w, float h,
                                       float u0, float v0, float u1, float v1,
                                       int color) {
        if (view == null || w <= 0 || h <= 0) return;

        Batch batch = core.gpuViewBatchFor(view);
        batch.setSampler(sampler);
        core.registerGpuViewSampler(view, sampler);
        core.registerGpuViewPipeline(view, pipeline);

        float[] c = unpack(color);

        float x2 = x + w, y2 = y + h;
        batch.vertexUV(x,  y,  u0, v0, c[0], c[1], c[2], c[3]);
        batch.vertexUV(x2, y,  u1, v0, c[0], c[1], c[2], c[3]);
        batch.vertexUV(x,  y2, u0, v1, c[0], c[1], c[2], c[3]);
        batch.vertexUV(x,  y2, u0, v1, c[0], c[1], c[2], c[3]);
        batch.vertexUV(x2, y,  u1, v0, c[0], c[1], c[2], c[3]);
        batch.vertexUV(x2, y2, u1, v1, c[0], c[1], c[2], c[3]);
    }

    private static float[] unpack(int color) {
        return new float[] {
                ((color >> 16) & 0xFF) / 255f,
                ((color >>  8) & 0xFF) / 255f,
                ( color        & 0xFF) / 255f,
                ((color >> 24) & 0xFF) / 255f,
        };
    }
}