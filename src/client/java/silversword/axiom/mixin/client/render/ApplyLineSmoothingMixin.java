package silversword.axiom.mixin.client.render;

import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.mixininterface.ILineSmoothing;

import static org.lwjgl.opengl.GL11C.*;

@Mixin(GlCommandEncoder.class)
public class ApplyLineSmoothingMixin {
    @Inject(method = "applyPipelineState", at = @At("HEAD"))
    private void onSetPipeline(RenderPipeline pipeline, CallbackInfo ci) {
        if (pipeline instanceof ILineSmoothing) {
            boolean smooth = ((ILineSmoothing) pipeline).axiom_getLineSmooth();
            if (smooth) {
                glEnable(GL_LINE_SMOOTH);
                glLineWidth(1.0f);
            } else {
                glDisable(GL_LINE_SMOOTH);
            }
        }
    }
}