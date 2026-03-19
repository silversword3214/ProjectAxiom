package silversword.axiom.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
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
import silversword.axiom.mixin.client.accessors.KeyBindingAccessor;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {

    @Shadow @Final private GameOptions settings;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        // Freecam takes highest priority
        Freecam freecam = ModuleManager.getInstance().getModule(Freecam.class);
        if (freecam != null && freecam.isEnabled()) {
            // Cancel the original tick – player input will not be updated
            // We set playerInput to zero to be safe
            this.playerInput = new PlayerInput(false, false, false, false, false, false, false);
            this.movementVector = Vec2f.ZERO;
            ci.cancel();
            return;
        }

        // InvWalk handling
        InvWalk invWalk = ModuleManager.getInstance().getModule(InvWalk.class);
        if (invWalk == null || !invWalk.isEnabled()) return;

        Screen screen = mc.currentScreen;
        if (screen == null) return;

        boolean isHandledScreen = screen instanceof HandledScreen;
        boolean isOwnClickGui = screen instanceof ClickGuiScreen; // tarkista oikea luokka

        if (!isHandledScreen && !isOwnClickGui) return;

        // Jos kyseessä on HandledScreen, mutta minecraftGui on false, älä salli
        if (isHandledScreen && !invWalk.minecraftGui.get()) return;
        // Jos kyseessä on oma ClickGui, mutta ownClickGui on false, älä salli
        if (isOwnClickGui && !invWalk.ownClickGui.get()) return;

        boolean forward = isKeyPressed(mc, settings.forwardKey);
        boolean back = isKeyPressed(mc, settings.backKey);
        boolean left = isKeyPressed(mc, settings.leftKey);
        boolean right = isKeyPressed(mc, settings.rightKey);
        boolean jump = isKeyPressed(mc, settings.jumpKey);
        boolean sneak = isKeyPressed(mc, settings.sneakKey);
        boolean sprint = isKeyPressed(mc, settings.sprintKey);

        this.playerInput = new PlayerInput(forward, back, left, right, jump, sneak, sprint);

        float f = getMovementFactor(forward, back);
        float g = getMovementFactor(left, right);
        this.movementVector = new Vec2f(g, f).normalize();

        ci.cancel();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onFreecamSneak(CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        Freecam freecam = ModuleManager.getInstance().getModule(Freecam.class);
        if (freecam == null || !freecam.isEnabled()) return;

        if (freecam.staySneaking()) {
            this.playerInput = new PlayerInput(
                    this.playerInput.forward(),
                    this.playerInput.backward(),
                    this.playerInput.left(),
                    this.playerInput.right(),
                    this.playerInput.jump(),
                    true,
                    this.playerInput.sprint()
            );
        }
    }

    private boolean isKeyPressed(MinecraftClient mc, KeyBinding keyBinding) {
        InputUtil.Key key = ((KeyBindingAccessor) keyBinding).getBoundKey();
        Window window = mc.getWindow();
        return InputUtil.isKeyPressed(window, key.getCode());
    }

    private static float getMovementFactor(boolean positive, boolean negative) {
        if (positive == negative) return 0.0F;
        return positive ? 1.0F : -1.0F;
    }
}