package silversword.axiom.client.mixininterface;

import com.mojang.blaze3d.pipeline.RenderTarget;

/**
 * Mahdollistaa entityOutlineTargetin vaihdon lennossa.
 * Käytetään Shader ESP:n mask-passissa.
 */
public interface ILevelRenderer {
    void axiom$pushEntityOutlineFramebuffer(RenderTarget fb);
    void axiom$popEntityOutlineFramebuffer();
}