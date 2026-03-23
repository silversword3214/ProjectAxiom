package silversword.axiom.client.hud.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import silversword.axiom.client.hud.BaseHudElement;
import silversword.axiom.client.hud.core.HudContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.clamp;

public final class PotionEffectsHud extends BaseHudElement {
    private String mode = "SELF";
    private int maxEffects = 8;
    private boolean showDurations = true;
    private boolean compact = false;
    private int backgroundColor = 0x90000000;
    private int borderColor = 0xFF6A00FF;
    private int textColor = 0xFFFFFFFF;

    // Skaalaukset
    private float textScale = 1.0f;
    private float backgroundScale = 1.0f;
    private float outlineScale = 1.0f;

    // Kiinteät perusmitat
    private static final int BASE_ICON_SIZE = 18;
    private static final int BASE_GAP = 3;
    private static final int BASE_PADDING = 6;
    private static final int CORNER_RADIUS = 4;

    private int lockedId = -1;
    private long lastSeenAtMs = 0L;
    private long lingerMs = 1500;
    private boolean fadeOut = true;
    private long fadeMs = 250;
    private double maxRange = 64.0;

    public PotionEffectsHud() {
        super("PotionEffects", 10, 220);
        this.enabled = false;
    }

    public void setMode(String v) { mode = v; }
    public void setMaxEffects(int v) { maxEffects = Math.max(1, Math.min(32, v)); }
    public void setShowDurations(boolean v) { showDurations = v; }
    public void setCompact(boolean v) { compact = v; }
    public void setBackgroundColor(int c) { backgroundColor = c; }
    public void setBorderColor(int c) { borderColor = c; }
    public void setTextColor(int c) { textColor = c; }
    public void setLingerMs(long v) { lingerMs = Math.max(0, v); }
    public void setFadeOut(boolean v) { fadeOut = v; }
    public void setFadeMs(long v) { fadeMs = Math.max(0, v); }
    public void setMaxRange(double v) { maxRange = Math.max(0, v); }
    public void setTextScale(float v) { textScale = Math.max(0.5f, Math.min(2.0f, v)); }
    public void setBackgroundScale(float v) { backgroundScale = Math.max(0.5f, Math.min(2.0f, v)); }
    public void setOutlineScale(float v) { outlineScale = Math.max(0.5f, Math.min(2.0f, v)); }

    @Override public boolean isModuleControlled() { return true; }
    @Override public int width(Minecraft mc) {
        return (int) ((maxEffects * (BASE_ICON_SIZE + BASE_GAP) - BASE_GAP + BASE_PADDING * 2) * backgroundScale);
    }
    @Override public int height(Minecraft mc) {
        int base = (int) ((BASE_ICON_SIZE + BASE_PADDING * 2) * backgroundScale);
        if (showDurations && !compact) base += (int) (mc.font.lineHeight * textScale) + 2;
        return base;
    }

    @Override
    public void render(HudContext ctx, DeltaTracker tickCounter) {
        if (!enabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Player target = resolveTarget(mc);
        if (target == null) return;

        if (maxRange > 0 && mode.equals("TARGET") && mc.player.distanceTo(target) > maxRange) {
            if (lockedId >= 0) lockedId = -1;
            return;
        }

        List<MobEffectInstance> effects = getEffects(target, maxEffects);
        if (effects.isEmpty()) return;

        float alpha = 1.0f;
        if (mode.equals("TARGET") && fadeOut && lingerMs > 0) {
            long now = System.currentTimeMillis();
            long elapsed = now - lastSeenAtMs;
            if (elapsed > lingerMs) return;
            long remaining = lingerMs - elapsed;
            if (remaining < fadeMs) alpha = (float) remaining / (float) fadeMs;
        }

        renderEffects(ctx, effects, alpha);
    }

    @Override
    public void renderEdit(HudContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            List<MobEffectInstance> effects = getEffects(mc.player, maxEffects);
            if (!effects.isEmpty()) renderEffects(ctx, effects, 1.0f);
        }
    }

    // ---------- sisäinen logiikka ----------
    private Player resolveTarget(Minecraft mc) {
        if (mode.equals("SELF")) return mc.player;
        long now = System.currentTimeMillis();
        Player aimed = getAimedPlayer(mc);
        if (aimed != null && aimed.isAlive()) {
            lockedId = aimed.getId();
            lastSeenAtMs = now;
        }
        Player locked = getLockedPlayer(mc);
        if (locked == null || !locked.isAlive()) {
            lockedId = -1;
            return null;
        }
        long elapsed = now - lastSeenAtMs;
        if (aimed == null && elapsed > lingerMs) {
            lockedId = -1;
            return null;
        }
        return locked;
    }

    private Player getAimedPlayer(Minecraft mc) {
        if (mc.hitResult instanceof EntityHitResult ehr && ehr.getEntity() instanceof Player p) return p;
        return null;
    }

    private Player getLockedPlayer(Minecraft mc) {
        if (lockedId < 0) return null;
        Entity e = mc.level.getEntity(lockedId);
        return (e instanceof Player p) ? p : null;
    }

    private List<MobEffectInstance> getEffects(Player player, int max) {
        List<MobEffectInstance> list = new ArrayList<>();
        for (MobEffectInstance inst : player.getActiveEffects()) {
            if (inst != null && inst.getDuration() > 0 && inst.showIcon()) list.add(inst);
        }
        list.sort(Comparator.comparingInt(MobEffectInstance::getDuration).reversed()
                .thenComparingInt(MobEffectInstance::getAmplifier).reversed());
        return list.size() > max ? list.subList(0, max) : list;
    }

    private void renderEffects(HudContext ctx, List<MobEffectInstance> effects, float alpha) {
        int count = effects.size();

        // Skaalatut mitat
        int iconSize = (int) (BASE_ICON_SIZE * backgroundScale);
        int gap = (int) (BASE_GAP * backgroundScale);
        int padding = (int) (BASE_PADDING * backgroundScale);

        int totalWidth = count * (iconSize + gap) - gap;
        int boxW = totalWidth + padding * 2;
        int boxH = iconSize + padding * 2;
        if (showDurations && !compact) boxH += (int) (ctx.fontHeight() * textScale) + 2;

        int x0 = x;
        int y0 = y;

        // Värit alpha-kerroin huomioiden
        int bgArgb = applyAlpha(backgroundColor, alpha);
        int borderArgb = applyAlpha(borderColor, alpha);
        int txtArgb = applyAlpha(textColor, alpha);

        // Tausta pyöristettynä
        ctx.fillRounded(x0, y0, boxW, boxH, CORNER_RADIUS, bgArgb);

        // Reunus (paksuus skaalautuu, vähintään 1px)
        double thickness = Math.max(1.0, outlineScale);
        ctx.drawRoundedOutline(x0, y0, boxW, boxH, CORNER_RADIUS, borderArgb, thickness);

        int startX = x0 + padding;
        int startY = y0 + padding;

        for (int i = 0; i < count; i++) {
            MobEffectInstance inst = effects.get(i);
            int ix = startX + i * (iconSize + gap);
            int iy = startY;
            ctx.drawVanillaEffectIcon(inst, ix, iy, iconSize, alpha);

            if (showDurations && !compact) {
                int seconds = inst.getDuration() / 20;
                String timeStr = Integer.toString(seconds);
                int tw = (int) (ctx.textWidth(timeStr) * textScale);
                int tx = ix + (iconSize - tw) / 2;
                int ty = iy + iconSize + 2;
                ctx.drawScaledText(timeStr, tx, ty, txtArgb, true, textScale);
            }
        }
    }

    private static int applyAlpha(int argb, float mul) {
        int a = (argb >>> 24) & 0xFF;
        int na = clamp((int) (a * mul), 0, 255);
        return (na << 24) | (argb & 0x00FFFFFF);
    }
}