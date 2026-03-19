package silversword.axiom.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.Freecam;

@Mixin(Mouse.class)
public class MouseMixin {

    @Shadow private double cursorDeltaX;
    @Shadow private double cursorDeltaY;
    @Shadow private boolean cursorLocked;

    @Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
    private void onUpdateMouse(double timeDelta, CallbackInfo ci) {
        Freecam freecam = ModuleManager.getInstance().getModule(Freecam.class);
        if (freecam == null || !freecam.isEnabled()) return;

        if (!cursorLocked) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        double sens = mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2;
        double scale = sens * sens * sens * 1.75;

        double dx = cursorDeltaX * scale;
        double dy = cursorDeltaY * scale;

        freecam.changeLookDirection(dx, dy);

        cursorDeltaX = 0.0;
        cursorDeltaY = 0.0;

        ci.cancel();
    }
}