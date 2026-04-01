package silversword.axiom.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.event.mouse.MouseUpdateEvent;
import silversword.axiom.client.main.AxiomInitialize;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.Freecam;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;
    @Shadow private boolean mouseGrabbed;


    @Inject(method = "handleAccumulatedMovement", at = @At("HEAD"))
    private void onHandleAccumulatedMovement(CallbackInfo ci) {
        MouseUpdateEvent event = new MouseUpdateEvent(accumulatedDX, accumulatedDY);
        AxiomInitialize.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            accumulatedDX = 0;
            accumulatedDY = 0;
        } else {
            accumulatedDX = event.getDeltaX();
            accumulatedDY = event.getDeltaY();
        }
    }

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void onUpdateMouse(double timeDelta, CallbackInfo ci) {
        Freecam freecam = ModuleManager.getInstance().getModule(Freecam.class);
        if (freecam == null || !freecam.isEnabled()) return;

        if (!mouseGrabbed) return;

        Minecraft mc = Minecraft.getInstance();
        double sens = mc.options.sensitivity().get() * 0.6 + 0.2;
        double scale = sens * sens * sens * 1.75;

        double dx = accumulatedDX * scale;
        double dy = accumulatedDY * scale;

        freecam.changeLookDirection(dx, dy);

        accumulatedDX = 0.0;
        accumulatedDY = 0.0;

        ci.cancel();
    }
}