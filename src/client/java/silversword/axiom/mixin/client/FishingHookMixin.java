package silversword.axiom.mixin.client;

import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.misc.AutoFish;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin {

    @Shadow private int nibble;

    @Inject(method = "tick", at = @At("RETURN"))
    private void onTick(CallbackInfo ci) {
        if (nibble > 0) {
            AutoFish module = ModuleManager.getInstance().getModule(AutoFish.class);
            if (module != null && module.isEnabled()) {
                module.onFishBite();
            }
        }
    }
}