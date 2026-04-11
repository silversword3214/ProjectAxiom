package silversword.axiom.client.render.rendersystem.axiomrenderer.postprocess;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;

public final class ShaderFramebufferManager implements AutoCloseable {
    private RenderTarget maskTarget;
    private RenderTarget postTarget;

    public void ensureTargets(int width, int height) {
        if (width <= 0 || height <= 0) return;

        if (maskTarget == null || maskTarget.width != width || maskTarget.height != height) {
            if (maskTarget != null) maskTarget.destroyBuffers();
            maskTarget = new TextureTarget("ShaderESP Mask", width, height, true);
        }

        if (postTarget == null || postTarget.width != width || postTarget.height != height) {
            if (postTarget != null) postTarget.destroyBuffers();
            postTarget = new TextureTarget("ShaderESP Post", width, height, false);
        }
    }

    public RenderTarget getMaskTarget() {
        return maskTarget;
    }

    public RenderTarget getPostTarget() {
        return postTarget;
    }

    @Override
    public void close() {
        if (maskTarget != null) {
            maskTarget.destroyBuffers();
            maskTarget = null;
        }

        if (postTarget != null) {
            postTarget.destroyBuffers();
            postTarget = null;
        }
    }
}
