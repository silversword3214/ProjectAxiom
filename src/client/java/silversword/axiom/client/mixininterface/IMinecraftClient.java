package silversword.axiom.client.mixininterface;

import net.minecraft.client.gl.Framebuffer;

public interface IMinecraftClient {
    void axiom$rightClick();

    void axiom$setFramebuffer(Framebuffer framebuffer);
}