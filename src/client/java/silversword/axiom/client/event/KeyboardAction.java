package silversword.axiom.client.event;

import com.mojang.blaze3d.platform.InputConstants;

public enum KeyboardAction {
    PRESS, RELEASE, REPEAT;

    public static KeyboardAction get(int glfwAction) {
        return switch (glfwAction) {
            case InputConstants.PRESS   -> PRESS;
            case InputConstants.RELEASE -> RELEASE;
            default                     -> REPEAT;
        };
    }
}