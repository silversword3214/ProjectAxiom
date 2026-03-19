package silversword.axiom.client.mixininterface;

import com.mojang.blaze3d.systems.RenderPass;

public interface IGpuDevice {
    /**
     * Currently there can only be a single scissor pushed at once.
     */
    void axiom$pushScissor(int x, int y, int width, int height);

    void axiom$popScissor();

    /**
     * This is an *INTERNAL* method, it shouldn't be called.
     */
    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    void axiom$onCreateRenderPass(RenderPass pass);
}
