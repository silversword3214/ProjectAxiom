package silversword.axiom.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import silversword.axiom.client.hud.core.HudContext;

public interface HudElement {
    String id();
    boolean enabled();
    void setEnabled(boolean v);
    int x();
    int y();
    void setPos(int x, int y);
    int width(Minecraft mc);
    int height(Minecraft mc);
    void render(HudContext ctx, DeltaTracker tickCounter);
    void renderEdit(HudContext ctx);

    default boolean isModuleControlled() {
        return false;
    }
}