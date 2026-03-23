package silversword.axiom.client.modules.misc;

import net.minecraft.world.InteractionHand;
import org.lwjgl.glfw.GLFW;
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
        if (mc.player == null || mc.level == null) return;

        boolean left = button.getMode().equals("Left") || button.getMode().equals("Both");
        boolean right = button.getMode().equals("Right") || button.getMode().equals("Both");

        long now = System.currentTimeMillis();
        long baseDelay = (long) (delay.getValue() * 1000);
        long randomAdd = (long) (Math.random() * random.getValue());
        long actualDelay = baseDelay + randomAdd;

        if (onlyWhileHolding.get()) {
            long handle = mc.getWindow().handle();
            boolean leftHeld = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS;
            boolean rightHeld = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_2) == GLFW.GLFW_PRESS;

            if (left && leftHeld && now - lastClickTime >= actualDelay) {
                clickLeft();
                lastClickTime = now;
            }
            if (right && rightHeld && now - lastClickTime >= actualDelay) {
                clickRight();
                lastClickTime = now;
            }
        } else {
            // Jos ei vaadita pohjassa pitämistä, klikataan jatkuvasti
            if (now - lastClickTime >= actualDelay) {
                if (left) clickLeft();
                if (right) clickRight();
                lastClickTime = now;
            }
        }
    }

    private void clickLeft() {
        if (mc.player == null || mc.gameMode == null) return;
        if (mc.crosshairPickEntity != null) {
            mc.gameMode.attack(mc.player, mc.crosshairPickEntity);
        }
        mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private void clickRight() {
        if (mc.player == null || mc.gameMode == null) return;
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
    }

    @Subscribe
    public void onMouseClick(MouseClickEvent event) {
        if (!isEnabled()) return;

        boolean left = button.getMode().equals("Left") || button.getMode().equals("Both");
        boolean right = button.getMode().equals("Right") || button.getMode().equals("Both");

        int glfwButton = -1;
        if (event.click.button() == 0) glfwButton = GLFW.GLFW_MOUSE_BUTTON_1;
        else if (event.click.button() == 1) glfwButton = GLFW.GLFW_MOUSE_BUTTON_2;

        if (event.action == KeyboardAction.PRESS) {
            if (left && glfwButton == GLFW.GLFW_MOUSE_BUTTON_1) {
                event.setCancelled(true);
            }
            if (right && glfwButton == GLFW.GLFW_MOUSE_BUTTON_2) {
                event.setCancelled(true);
            }
        }
    }
}