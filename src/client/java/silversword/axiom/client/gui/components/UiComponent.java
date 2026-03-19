package silversword.axiom.client.gui.components;

import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;

/**
 * Base contract for all UI elements.
 * Bounds are the single source of truth for hit-testing and layout.
 */
public interface UiComponent {

    Rect getBounds();
    void setBounds(Rect bounds);

    /** Used by parent layouts (e.g., list, window content). */
    int getPreferredHeight();

    void render(UiContext ui, int mouseX, int mouseY, float delta);

    /** Return true if the event was consumed. */
    default boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) { return false; }
    default void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {}

    default boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) { return false; }
    default boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) { return false; }

    default boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) { return false; }
    default boolean charTyped(UiContext ui, char chr, int modifiers) { return false; }
}
