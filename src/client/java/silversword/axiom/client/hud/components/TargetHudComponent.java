package silversword.axiom.client.hud.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import silversword.axiom.client.hud.BaseHudElement;
import silversword.axiom.client.hud.core.HudContext;
import silversword.axiom.client.render.rendersystem.Renderer2D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;

public final class TargetHudComponent extends BaseHudElement {
    private long lingerMs = 1500;
    private boolean fadeOut = true;
    private long fadeMs = 250;
    private double maxRange = 48.0;

    private boolean showName = true;
    private boolean showHealthText = true;
    private boolean showDistance = true;
    private boolean showHealthBar = true;

    // Skaalaukset
    private float textScale = 1.0f;
    private float backgroundScale = 1.0f;
    private float outlineScale = 1.0f;

    private int backgroundAlpha = 170;
    private int borderColor = 0xFF6A00FF;
    private int textColor = 0xFFFFFFFF;

    // Kiinteät perusmitat
    private static final int BASE_PADDING = 6;
    private static final int BASE_LINE_HEIGHT = 9; // fontHeight + 2
    private static final int BASE_MIN_WIDTH = 170;
    private static final int BAR_HEIGHT = 8;
    private static final int CORNER_RADIUS = 4;

    private int lockedId = -1;
    private long lastSeenAtMs = 0L;
    private int lastW = 170, lastH = 70;

    public TargetHudComponent() {
        super("TargetHUD", 10, 80);
        this.enabled = false;
    }

    public void setLingerMs(long v) { lingerMs = Math.max(0, v); }
    public void setFadeOut(boolean v) { fadeOut = v; }
    public void setFadeMs(long v) { fadeMs = Math.max(0, v); }
    public void setMaxRange(double v) { maxRange = Math.max(0, v); }
    public void setShowName(boolean v) { showName = v; }
    public void setShowHealthText(boolean v) { showHealthText = v; }
    public void setShowDistance(boolean v) { showDistance = v; }
    public void setShowHealthBar(boolean v) { showHealthBar = v; }
    public void setTextScale(float v) { textScale = Math.max(0.5f, Math.min(2.0f, v)); }
    public void setBackgroundScale(float v) { backgroundScale = Math.max(0.5f, Math.min(2.0f, v)); }
    public void setOutlineScale(float v) { outlineScale = Math.max(0.5f, Math.min(2.0f, v)); }
    public void setBackgroundAlpha(int v) { backgroundAlpha = clamp(v, 0, 255); }
    public void setBorderColor(int v) { borderColor = v; }
    public void setTextColor(int v) { textColor = v; }

    @Override public boolean isModuleControlled() { return true; }
    @Override public int width(Minecraft mc) { return Math.max(1, lastW); }
    @Override public int height(Minecraft mc) { return Math.max(1, lastH); }

    @Override
    public void render(HudContext ctx, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        long now = System.currentTimeMillis();
        LivingEntity aimed = currentAimedLiving(mc);
        if (aimed != null && aimed.isAlive()) {
            lockedId = aimed.getId();
            lastSeenAtMs = now;
        }
        LivingEntity locked = getLockedLiving(mc);
        if (locked == null || !locked.isAlive()) {
            lockedId = -1;
            return;
        }
        if (maxRange > 0 && mc.player.distanceTo(locked) > maxRange) {
            lockedId = -1;
            return;
        }
        long elapsed = now - lastSeenAtMs;
        if (aimed == null && elapsed > lingerMs) {
            lockedId = -1;
            return;
        }
        float alphaMul = 1.0f;
        if (aimed == null && fadeOut && fadeMs > 0 && lingerMs > 0) {
            long remaining = lingerMs - elapsed;
            if (remaining < fadeMs) alphaMul = clamp01((float) remaining / (float) fadeMs);
        }
        drawHud(ctx, mc, locked, alphaMul);
    }

    @Override
    public void renderEdit(HudContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) drawHud(ctx, mc, mc.player, 1.0f);
    }

    private void drawHud(HudContext ctx, Minecraft mc, LivingEntity target, float alphaMul) {
        String name = target.getName().getString();
        float hp = target.getHealth();
        float maxHp = Math.max(1, target.getMaxHealth());
        double dist = mc.player.distanceTo(target);
        String hpText = String.format("HP: %.1f / %.1f", hp, maxHp);
        String distText = String.format("Dist: %.1f m", dist);

        // Skaalatut mitat
        int padding = (int) (BASE_PADDING * backgroundScale);
        int lineH = (int) (BASE_LINE_HEIGHT * backgroundScale);
        int barH = (int) (BAR_HEIGHT * backgroundScale);

        int lines = 0;
        int textW = 0;
        if (showName) { lines++; textW = Math.max(textW, ctx.textWidth(name)); }
        if (showHealthText) { lines++; textW = Math.max(textW, ctx.textWidth(hpText)); }
        if (showDistance) { lines++; textW = Math.max(textW, ctx.textWidth(distText)); }
        if (lines == 0 && !showHealthBar) return;

        int minW = (int) (BASE_MIN_WIDTH * backgroundScale);
        int boxW = Math.max(minW, textW + padding * 2);
        int boxH = padding * 2 + (lines * lineH) + (showHealthBar ? barH + 2 : 0);
        lastW = boxW;
        lastH = boxH;

        int x0 = x;
        int y0 = y;

        // Haetaan värit ja sovelletaan alpha
        int bg = applyAlpha((backgroundAlpha << 24) | 0x000000, alphaMul);
        int border = applyAlpha(borderColor, alphaMul);
        int txt = applyAlpha(textColor, alphaMul);

        Color bgCol = new Color(bg);
        Color borderCol = new Color(border);

        // Tausta pyöristettynä
        Renderer2D.COLOR.drawRoundedRect(x0, y0, boxW, boxH, CORNER_RADIUS, bgCol);

        // Reunus (paksuus skaalautuu, vähintään 1px)
        double thickness = Math.max(1.0, outlineScale);
        Renderer2D.COLOR.drawRoundedRectOutline(x0, y0, boxW, boxH, CORNER_RADIUS, borderCol, thickness);

        int tx = x0 + padding;
        int ty = y0 + padding;

        if (showName) {
            ctx.drawScaledText(name, tx, ty, txt, true, textScale);
            ty += lineH;
        }
        if (showHealthText) {
            ctx.drawScaledText(hpText, tx, ty, txt, true, textScale);
            ty += lineH;
        }
        if (showDistance) {
            ctx.drawScaledText(distText, tx, ty, txt, true, textScale);
            ty += lineH;
        }
        if (showHealthBar) {
            int barX = tx;
            int barY = ty + 2;
            int barW = boxW - padding * 2;
            float pct = clamp01(hp / maxHp);
            int fillW = (int) (barW * pct);
            int barBg = applyAlpha(0xFF222222, alphaMul);
            int barFill = applyAlpha(getHealthColor(pct), alphaMul);

            Renderer2D.COLOR.drawRoundedRect(barX, barY, barW, barH, 2, new Color(barBg));

            Renderer2D.COLOR.quad(barX, barY, fillW, barH, new Color(barFill));

        }
    }

    private static LivingEntity currentAimedLiving(Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le) return le;
        return null;
    }

    private LivingEntity getLockedLiving(Minecraft mc) {
        if (lockedId < 0) return null;
        Entity e = mc.level.getEntity(lockedId);
        return (e instanceof LivingEntity le) ? le : null;
    }

    private static int getHealthColor(float pct) {
        if (pct >= 0.9f) return 0xFF00FF00; // vihreä
        if (pct >= 0.75f) return 0xFFAAFF00; // vaaleanvihreä
        if (pct >= 0.5f) return 0xFFFFFF00; // keltainen
        if (pct >= 0.25f) return 0xFFFFAA00; // oranssi
        return 0xFFFF0000; // punainen
    }

    private static float clamp01(float v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }
    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
    private static int applyAlpha(int argb, float mul) {
        int a = (argb >>> 24) & 0xFF;
        int na = clamp((int) (a * mul), 0, 255);
        return (na << 24) | (argb & 0x00FFFFFF);
    }
}