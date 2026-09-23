package silversword.axiom.client.event;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import silversword.axiom.client.gui.screen.ClickGuiScreen;
import silversword.axiom.client.hud.util.ClickCounter;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.hidden.Keybinds;

public class InputListener {
    private static boolean wasKeyPressed = false;
    private static boolean wasLeftPressed = false;
    private static boolean wasRightPressed = false;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            ModuleManager.getInstance().updateAllModules();

            // Haetaan Keybinds-moduuli
            Keybinds keybinds = ModuleManager.getInstance().getModule(Keybinds.class);
            int clickGuiKey = InputConstants.KEY_RSHIFT;   // oli GLFW_KEY_RIGHT_SHIFT
            if (keybinds != null) {
                clickGuiKey = keybinds.clickGuiKey.get();
            }

            long handle = client.getWindow().handle();
            if (handle == 0) return;

            // --- Hiiren napit: luetaan MouseHandler-tilasta (glfw ei enää käytettävissä) ---
            boolean leftPressed  = client.mouseHandler.isLeftPressed();
            boolean rightPressed = client.mouseHandler.isRightPressed();

            if (leftPressed  && !wasLeftPressed)  ClickCounter.onLeftClick();
            if (rightPressed && !wasRightPressed) ClickCounter.onRightClick();

            wasLeftPressed  = leftPressed;
            wasRightPressed = rightPressed;

            // --- Näppäin: InputConstants (oli GLFW.glfwGetKey) ---
            boolean isPressed = InputConstants.isKeyDown(clickGuiKey);

            if (isPressed && !wasKeyPressed) {
                if (client.gui.screen() == null) {
                    client.gui.setScreen(new ClickGuiScreen());
                } else if (client.gui.screen() instanceof ClickGuiScreen) {
                    client.gui.setScreen(null);
                }
            }
            wasKeyPressed = isPressed;
        });
    }
}