package silversword.axiom.mixin.client.render.rendersystem;

import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.opengl.GlDevice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import silversword.axiom.client.mixininterface.IGpuDevice;

@Mixin(GlDevice.class)
public abstract class GlDeviceMixin implements IGpuDevice {
    @Unique
    private int x, y, width, height;

    @Unique
    private boolean set;

    @Override
    public void axiom$pushScissor(int x, int y, int width, int height) {
        if (set)
            throw new IllegalStateException("Currently there can only be one global scissor pushed");

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        set = true;
    }

    @Override
    public void axiom$popScissor() {
        if (!set)
            throw new IllegalStateException("No scissor pushed");

        set = false;
    }

    @Deprecated
    @Override
    public void axiom$onCreateRenderPass(RenderPass pass) {
        if (set) {
            pass.enableScissor(x, y, width, height);
        }
    }
}