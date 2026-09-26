package silversword.axiom.mixin.client.render;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.Zoom;

@Mixin(MouseHandler.class)
public class ZoomScrollMixin {

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void axiom$scrollZoom(long window, double xOffset, double yOffset,
                                  CallbackInfo ci) {
        Zoom zoom = ModuleManager.getInstance().getModule(Zoom.class);
        if (zoom == null) return;
        if (!zoom.isZooming()) return;
        if (!zoom.scrollZoom.get()) return;

        zoom.onScroll(yOffset);
        ci.cancel();
    }
}