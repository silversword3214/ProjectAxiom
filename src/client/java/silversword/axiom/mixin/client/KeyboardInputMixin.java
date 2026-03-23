package silversword.axiom.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.Options;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.gui.screen.ClickGuiScreen;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.misc.InvWalk;
import silversword.axiom.client.modules.render.Freecam;
import silversword.axiom.mixin.client.accessors.KeyMappingAccessor;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {

    @Shadow @Final private Options options;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Freecam takes highest priority
        Freecam freecam = ModuleManager.getInstance().getModule(Freecam.class);
        if (freecam != null && freecam.isEnabled()) {
            // Cancel the original tick – player input will not be updated
            // We set playerInput to zero to be safe
            this.keyPresses = new Input(false, false, false, false, false, false, false);
            this.moveVector = Vec2.ZERO;
            ci.cancel();
            return;
        }

        // InvWalk handling
        InvWalk invWalk = ModuleManager.getInstance().getModule(InvWalk.class);
        if (invWalk == null || !invWalk.isEnabled()) return;

        Screen screen = mc.screen;
        if (screen == null) return;

        boolean isHandledScreen = screen instanceof AbstractContainerScreen;
        boolean isOwnClickGui = screen instanceof ClickGuiScreen; // tarkista oikea luokka

        if (!isHandledScreen && !isOwnClickGui) return;

        // Jos kyseessä on HandledScreen, mutta minecraftGui on false, älä salli
        if (isHandledScreen && !invWalk.minecraftGui.get()) return;
        // Jos kyseessä on oma ClickGui, mutta ownClickGui on false, älä salli
        if (isOwnClickGui && !invWalk.ownClickGui.get()) return;

        boolean forward = isKeyPressed(mc, options.keyUp);
        boolean back = isKeyPressed(mc, options.keyDown);
        boolean left = isKeyPressed(mc, options.keyLeft);
        boolean right = isKeyPressed(mc, options.keyRight);
        boolean jump = isKeyPressed(mc, options.keyJump);
        boolean sneak = isKeyPressed(mc, options.keyShift);
        boolean sprint = isKeyPressed(mc, options.keySprint);

        this.keyPresses = new Input(forward, back, left, right, jump, sneak, sprint);

        float f = getMovementFactor(forward, back);
        float g = getMovementFactor(left, right);
        this.moveVector = new Vec2(g, f).normalized();

        ci.cancel();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onFreecamSneak(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Freecam freecam = ModuleManager.getInstance().getModule(Freecam.class);
        if (freecam == null || !freecam.isEnabled()) return;

        if (freecam.staySneaking()) {
            this.keyPresses = new Input(
                    this.keyPresses.forward(),
                    this.keyPresses.backward(),
                    this.keyPresses.left(),
                    this.keyPresses.right(),
                    this.keyPresses.jump(),
                    true,
                    this.keyPresses.sprint()
            );
        }
    }

    private boolean isKeyPressed(Minecraft mc, KeyMapping keyBinding) {
        InputConstants.Key key = ((KeyMappingAccessor) keyBinding).getKey();
        Window window = mc.getWindow();
        return InputConstants.isKeyDown(window, key.getValue());
    }

    private static float getMovementFactor(boolean positive, boolean negative) {
        if (positive == negative) return 0.0F;
        return positive ? 1.0F : -1.0F;
    }
}