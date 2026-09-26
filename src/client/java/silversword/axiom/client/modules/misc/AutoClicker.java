package silversword.axiom.client.modules.misc;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import silversword.axiom.client.event.KeyboardAction;
import silversword.axiom.client.event.MouseClickEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.*;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class AutoClicker extends AxiomMod implements KeybindConfigurable {

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    private final SettingMode button = new SettingMode(
            "Button",
            new String[]{"Left", "Right", "Both"},
            "Left"
    );

    private final SettingTime delay = new SettingTime(
            "Delay (s)",
            0.05, 2.0, 0.05, 0.2
    );

    private final SettingNumber random = new SettingNumber(
            "Random (ms)",
            0, 100, 10, 0
    );

    private final SettingBoolean onlyWhileHolding = new SettingBoolean("Only While Holding", true);

    private long lastClickTime = 0;

    public AutoClicker() {
        super("Auto Clicker", "Basic configurable auto clicker", ModuleCategory.MISC);
        addSetting(button);
        addSetting(delay);
        addSetting(random);
        addSetting(onlyWhileHolding);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onEnable() {
        lastClickTime = 0;
    }

    @Override
    protected void onDisable() {}

    @Override
    protected void onTick() {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.gameMode == null) return;

        String mode = button.getMode();
        boolean left  = mode.equals("Left")  || mode.equals("Both");
        boolean right = mode.equals("Right") || mode.equals("Both");

        long now = System.currentTimeMillis();
        long actualDelay = computeDelay();

        if (onlyWhileHolding.get()) {
            boolean leftHeld  = mc.mouseHandler.isLeftPressed();
            boolean rightHeld = mc.mouseHandler.isRightPressed();

            if (left && leftHeld && now - lastClickTime >= actualDelay) {
                clickLeft(player);
                lastClickTime = now;
            }
            if (right && rightHeld && now - lastClickTime >= actualDelay) {
                clickRight(player);
                lastClickTime = now;
            }
        } else {
            if (now - lastClickTime >= actualDelay) {
                if (left)  clickLeft(player);
                if (right) clickRight(player);
                lastClickTime = now;
            }
        }
    }

    private long computeDelay() {
        long base = (long) (delay.getValue() * 1000);
        double r = random.getValue();
        if (r <= 0) return base;

        RandomSource rng = mc.level != null ? mc.level.getRandom() : RandomSource.create();
        return base + (long) (rng.nextDouble() * r);
    }

    private void clickLeft(LocalPlayer player) {
        HitResult hit = mc.hitResult;
        if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) {
            mc.gameMode.startDestroyBlock(bhr.getBlockPos(), bhr.getDirection());
            mc.gameMode.continueDestroyBlock(bhr.getBlockPos(), bhr.getDirection());
        } else if (mc.crosshairPickEntity != null) {
            mc.gameMode.attack(player, mc.crosshairPickEntity);
        }
        player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
    }

    private void clickRight(LocalPlayer player) {
        HitResult hit = mc.hitResult;
        if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) {
            mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, bhr);
        } else {
            mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        }
    }

    @Subscribe
    public void onMouseClick(MouseClickEvent event) {
        if (!isEnabled()) return;

        String mode = button.getMode();
        boolean left  = mode.equals("Left")  || mode.equals("Both");
        boolean right = mode.equals("Right") || mode.equals("Both");

        if (event.action == KeyboardAction.PRESS) {
            int btn = event.click.button();
            // 26.3 / SDL: 1 = vasen, 2 = keski, 3 = oikea
            if (left  && btn == InputConstants.MOUSE_BUTTON_LEFT)  event.setCancelled(true);
            if (right && btn == InputConstants.MOUSE_BUTTON_RIGHT) event.setCancelled(true);
        }
    }
}