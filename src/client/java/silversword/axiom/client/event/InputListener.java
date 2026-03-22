package silversword.axiom.client.event;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.lwjgl.glfw.GLFW;
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
            int clickGuiKey = GLFW.GLFW_KEY_TAB; // oletus
            if (keybinds != null) {
                clickGuiKey = keybinds.clickGuiKey.get();
            }

            long handle = client.getWindow().handle();
            if (handle != 0) {
                // Hiiren klikkaukset
                boolean leftPressed = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS;
                boolean rightPressed = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_2) == GLFW.GLFW_PRESS;

                if (leftPressed && !wasLeftPressed) ClickCounter.onLeftClick();
                if (rightPressed && !wasRightPressed) ClickCounter.onRightClick();

                wasLeftPressed = leftPressed;
                wasRightPressed = rightPressed;

                // ClickGui-avaimen käsittely
                boolean isPressed = GLFW.glfwGetKey(handle, clickGuiKey) == GLFW.GLFW_PRESS;

                if (isPressed && !wasKeyPressed) {
                    if (client.screen == null) {
                        client.setScreen(new ClickGuiScreen());
                    } else if (client.screen instanceof ClickGuiScreen) {
                        client.setScreen(null);
                    }
                }
                wasKeyPressed = isPressed;
            }
        });
    }
}