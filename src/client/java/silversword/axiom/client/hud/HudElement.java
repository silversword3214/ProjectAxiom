package silversword.axiom.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import silversword.axiom.client.hud.core.HudContext;

public interface HudElement {
    String id();
    boolean enabled();
    void setEnabled(boolean v);
    int x();
    int y();
    void setPos(int x, int y);
    int width(MinecraftClient mc);
    int height(MinecraftClient mc);
    void render(HudContext ctx, RenderTickCounter tickCounter);
    void renderEdit(HudContext ctx);

    default boolean isModuleControlled() {
        return false;
    }
}