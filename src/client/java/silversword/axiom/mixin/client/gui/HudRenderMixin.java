package silversword.axiom.mixin.client.gui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.render.rendersystem.axiomrenderer.integration.AxiomHudBlocker;

@Mixin(Hud.class)
public class HudRenderMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void axiom_hideHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (AxiomHudBlocker.isScreenOpen()) {
            ci.cancel();
        }
    }
}