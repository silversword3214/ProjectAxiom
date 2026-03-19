package silversword.axiom.client.hud.components;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.LivingEntity;
import silversword.axiom.client.hud.BaseHudElement;
import silversword.axiom.client.hud.core.HudContext;
import silversword.axiom.client.render.font.TextRenderer;

public final class ReachDisplayHudComponent extends BaseHudElement {
    private LivingEntity target = null;
    private double distance = 0.0;
    private int ping = -1;

    private int backgroundColor = 0x90000000;
    private int textColor = 0xFFFFFFFF;
    private int borderColor = 0xFF00AAFF;
    private int padding = 4;

    public ReachDisplayHudComponent() {
        super("ReachDisplay", 10, 120);
        this.enabled = false;
    }

    public void setData(LivingEntity target, double distance, int ping) {
        this.target = target;
        this.distance = distance;
        this.ping = ping;
    }
    public void clearData() { target = null; distance = 0; ping = -1; }
    public void setBackgroundColor(int c) { backgroundColor = c; }
    public void setTextColor(int c) { textColor = c; }
    public void setBorderColor(int c) { borderColor = c; }

    @Override public boolean isModuleControlled() { return true; }
    @Override public int width(MinecraftClient mc) {
        if (target == null) return 60;
        String name = target.getName().getString();
        String distStr = String.format("%.2f m", distance);
        int w = (int) Math.max(TextRenderer.get().getWidth(name), TextRenderer.get().getWidth(distStr));
        if (ping >= 0) w = Math.max(w, (int) TextRenderer.get().getWidth("Ping: " + ping + " ms"));
        return w + padding * 2;
    }
    @Override public int height(MinecraftClient mc) {
        if (target == null) return (int) TextRenderer.get().getHeight() + padding * 2;
        int lines = ping >= 0 ? 3 : 2;
        return padding * 2 + lines * ((int) TextRenderer.get().getHeight() + 2);
    }

    @Override
    public void render(HudContext ctx, RenderTickCounter tickCounter) {
        if (!enabled || target == null) return;
        int w = width(null);
        int h = height(null);

        ctx.fill(x - 1, y - 1, w + 2, 1, borderColor);
        ctx.fill(x - 1, y + h, w + 2, 1, borderColor);
        ctx.fill(x - 1, y, 1, h, borderColor);
        ctx.fill(x + w, y, 1, h, borderColor);
        ctx.fill(x, y, w, h, backgroundColor);

        int currentY = y + padding;

        String name = target.getName().getString();
        ctx.text(name, x + padding, currentY, textColor, true);
        currentY += ctx.fontHeight() + 2;

        String distStr = String.format("%.2f m", distance);
        ctx.text(distStr, x + padding, currentY, textColor, true);
        currentY += ctx.fontHeight() + 2;

        if (ping >= 0) {
            ctx.text("Ping: " + ping + " ms", x + padding, currentY, textColor, true);
        }
    }

    @Override
    public void renderEdit(HudContext ctx) {
        int w = 80;
        int h = 60;
        ctx.fill(x - 1, y - 1, w + 2, 1, borderColor);
        ctx.fill(x - 1, y + h, w + 2, 1, borderColor);
        ctx.fill(x - 1, y, 1, h, borderColor);
        ctx.fill(x + w, y, 1, h, borderColor);
        ctx.fill(x, y, w, h, backgroundColor);
        ctx.text("ReachDisplay", x + 4, y + 4, textColor, true);
    }
}