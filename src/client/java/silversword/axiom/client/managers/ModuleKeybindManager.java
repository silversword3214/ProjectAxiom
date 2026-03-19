package silversword.axiom.client.managers;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.lwjgl.glfw.GLFW;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.setting.SettingKeybind;

import java.util.HashMap;
import java.util.Map;

public class ModuleKeybindManager {
    private static final Map<Integer, Boolean> wasPressed = new HashMap<>();

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (client.currentScreen != null) return;
            long handle = client.getWindow().getHandle();
            if (handle == 0) return;

            for (AxiomMod mod : ModuleManager.getInstance().getModules()) {
                // Etsi moduulin ensimmäinen toggle-tyyppinen keybind
                SettingKeybind keybind = null;
                for (var setting : mod.getAllSettings()) {
                    if (setting instanceof SettingKeybind kb && kb.isToggle()) {
                        keybind = kb;
                        break;
                    }
                }
                if (keybind == null) continue;

                int keyCode = keybind.get();
                if (keyCode <= 0 || keyCode >= 512) continue;

                boolean pressed = GLFW.glfwGetKey(handle, keyCode) == GLFW.GLFW_PRESS;
                boolean prev = wasPressed.getOrDefault(keyCode, false);

                if (pressed && !prev) {
                    mod.toggle();
                }

                wasPressed.put(keyCode, pressed);
            }
        });
    }
}