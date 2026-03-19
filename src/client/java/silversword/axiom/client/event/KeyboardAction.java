package silversword.axiom.client.event;

import org.lwjgl.glfw.GLFW;

public enum KeyboardAction {
    PRESS, RELEASE, REPEAT;

    public static KeyboardAction get(int glfwAction) {
        return switch (glfwAction) {
            case GLFW.GLFW_PRESS -> PRESS;
            case GLFW.GLFW_RELEASE -> RELEASE;
            default -> REPEAT;
        };
    }
}