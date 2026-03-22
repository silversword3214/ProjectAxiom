package silversword.axiom.mixin.client;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.event.KeyboardAction;
import silversword.axiom.client.event.KeyboardEvent;
import silversword.axiom.client.main.AxiomInitialize;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void onKey(long window, int action, KeyEvent input, CallbackInfo ci) {
        KeyboardEvent event = KeyboardEvent.get(input.key(), KeyboardAction.get(action), input.modifiers());
        AxiomInitialize.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}