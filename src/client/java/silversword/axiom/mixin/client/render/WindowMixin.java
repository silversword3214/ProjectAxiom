package silversword.axiom.mixin.client.render;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public class WindowMixin {

    @Inject(method = "calculateScale", at = @At("RETURN"), cancellable = true)
    private void forceGuiScale(int guiScaleIn, boolean forceUnicode, CallbackInfoReturnable<Integer> cir) {
        int forcedScale = 2;
        cir.setReturnValue(forcedScale);
    }
}