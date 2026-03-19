package silversword.axiom.client.hud.components;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import silversword.axiom.client.hud.BaseHudElement;
import silversword.axiom.client.hud.core.HudContext;
import silversword.axiom.client.render.rendersystem.Renderer2D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;

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
    @Override public int width(MinecraftClient mc) {
        return (int) ((maxEffects * (BASE_ICON_SIZE + BASE_GAP) - BASE_GAP + BASE_PADDING * 2) * backgroundScale);
    }
    @Override public int height(MinecraftClient mc) {
        int base = (int) ((BASE_ICON_SIZE + BASE_PADDING * 2) * backgroundScale);
        if (showDurations && !compact) base += (int) (mc.textRenderer.fontHeight * textScale) + 2;
        return base;
    }

    @Override
    public void render(HudContext ctx, RenderTickCounter tickCounter) {
        if (!enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        PlayerEntity target = resolveTarget(mc);
        if (target == null) return;

        if (maxRange > 0 && mode.equals("TARGET") && mc.player.distanceTo(target) > maxRange) {
            if (lockedId >= 0) lockedId = -1;
            return;
        }

        List<StatusEffectInstance> effects = getEffects(target, maxEffects);
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
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            List<StatusEffectInstance> effects = getEffects(mc.player, maxEffects);
            if (!effects.isEmpty()) renderEffects(ctx, effects, 1.0f);
        }
    }

    // ---------- sisäinen logiikka ----------
    private PlayerEntity resolveTarget(MinecraftClient mc) {
        if (mode.equals("SELF")) return mc.player;
        long now = System.currentTimeMillis();
        PlayerEntity aimed = getAimedPlayer(mc);
        if (aimed != null && aimed.isAlive()) {
            lockedId = aimed.getId();
            lastSeenAtMs = now;
        }
        PlayerEntity locked = getLockedPlayer(mc);
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

    private PlayerEntity getAimedPlayer(MinecraftClient mc) {
        if (mc.crosshairTarget instanceof EntityHitResult ehr && ehr.getEntity() instanceof PlayerEntity p) return p;
        return null;
    }

    private PlayerEntity getLockedPlayer(MinecraftClient mc) {
        if (lockedId < 0) return null;
        Entity e = mc.world.getEntityById(lockedId);
        return (e instanceof PlayerEntity p) ? p : null;
    }

    private List<StatusEffectInstance> getEffects(PlayerEntity player, int max) {
        List<StatusEffectInstance> list = new ArrayList<>();
        for (StatusEffectInstance inst : player.getStatusEffects()) {
            if (inst != null && inst.getDuration() > 0 && inst.shouldShowIcon()) list.add(inst);
        }
        list.sort(Comparator.comparingInt(StatusEffectInstance::getDuration).reversed()
                .thenComparingInt(StatusEffectInstance::getAmplifier).reversed());
        return list.size() > max ? list.subList(0, max) : list;
    }

    private void renderEffects(HudContext ctx, List<StatusEffectInstance> effects, float alpha) {
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


        // Haetaan nykyiset värit (moduulista tulleet int-arvot) ja sovelletaan alpha-kerroin
        int bgArgb = applyAlpha(backgroundColor, alpha);
        int borderArgb = applyAlpha(borderColor, alpha);
        int txtArgb = applyAlpha(textColor, alpha);

        Color bgCol = new Color(bgArgb);
        Color borderCol = new Color(borderArgb);

        // Tausta pyöristettynä
        Renderer2D.COLOR.drawRoundedRect(x0, y0, boxW, boxH, CORNER_RADIUS, bgCol);

        // Reunus (paksuus skaalautuu, vähintään 1px)
        double thickness = Math.max(1.0, outlineScale);
        Renderer2D.COLOR.drawRoundedRectOutline(x0, y0, boxW, boxH, CORNER_RADIUS, borderCol, thickness);

        int startX = x0 + padding;
        int startY = y0 + padding;

        for (int i = 0; i < count; i++) {
            StatusEffectInstance inst = effects.get(i);
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