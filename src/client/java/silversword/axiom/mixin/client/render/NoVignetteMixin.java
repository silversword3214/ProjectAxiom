package silversword.axiom.mixin.client.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.NoVignette;

@Mixin(Hud.class)
public class NoVignetteMixin {

    @Inject(method = "extractVignette", at = @At("HEAD"), cancellable = true)
    private void axiom$noVignette(GuiGraphicsExtractor graphics,
                                  Entity camera,
                                  CallbackInfo ci) {
        NoVignette mod = ModuleManager.getInstance().getModule(NoVignette.class);
        if (mod != null && mod.isEnabled()) {
            ci.cancel();
        }
    }
}