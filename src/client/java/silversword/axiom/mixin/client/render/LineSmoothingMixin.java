package silversword.axiom.mixin.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import silversword.axiom.client.mixininterface.ILineSmoothing;

@Mixin(RenderPipeline.class)
public class LineSmoothingMixin implements ILineSmoothing {
    @Unique
    private boolean lineSmooth;

    @Override
    public void axiom_setLineSmooth(boolean smooth) {
        this.lineSmooth = smooth;
    }

    @Override
    public boolean axiom_getLineSmooth() {
        return lineSmooth;
    }
}