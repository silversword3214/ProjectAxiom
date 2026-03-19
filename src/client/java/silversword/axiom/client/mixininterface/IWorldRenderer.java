package silversword.axiom.client.mixininterface;

import net.minecraft.client.gl.Framebuffer;

public interface IWorldRenderer {
    void axiom$pushEntityOutlineFramebuffer(Framebuffer fb);
    void axiom$popEntityOutlineFramebuffer();
}