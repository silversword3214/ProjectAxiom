package silversword.axiom.mixin.client.gui;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.render.rendersystem.axiomrenderer.integration.AxiomHudBlocker;

@Mixin(Gui.class)
public class GuiScreenMixin {

    @Inject(method = "setScreen", at = @At("TAIL"))
    private void axiom_trackScreenState(Screen screen, CallbackInfo ci) {
        AxiomHudBlocker.setScreenOpen(screen != null);
    }
}