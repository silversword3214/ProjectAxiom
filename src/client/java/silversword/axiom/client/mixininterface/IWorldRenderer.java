package silversword.axiom.client.mixininterface;

import com.mojang.blaze3d.pipeline.RenderTarget;

public interface IWorldRenderer {
    void axiom$pushEntityOutlineFramebuffer(RenderTarget fb);
    void axiom$popEntityOutlineFramebuffer();
}