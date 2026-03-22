package silversword.axiom.client.mixininterface;

import com.mojang.blaze3d.pipeline.RenderTarget;

public interface IMinecraftClient {
    void axiom$rightClick();

    void axiom$setFramebuffer(RenderTarget framebuffer);
}