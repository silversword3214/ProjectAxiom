package silversword.axiom.client.hud.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import silversword.axiom.client.hud.BaseHudElement;
import silversword.axiom.client.hud.core.HudContext;
import silversword.axiom.client.render.rendersystem.Renderer2D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PlayerInfoHudComponent extends BaseHudElement {
    private String mode = "TARGET";
    private long lingerMs = 1500;
    private boolean fadeOut = true;
    private long fadeMs = 250;
    private double maxRange = 64.0;

    // Skaalaukset
    private float textScale = 1.0f;
    private float backgroundScale = 1.0f; // SKAALAA SEKÄ TAUSTAA ETTÄ REUNUSTA

    private boolean showName = true;
    private boolean showHealth = true;
    private boolean showDistance = true;
    private boolean showPing = true;
    private boolean showEffects = true;
    private int maxEffects = 8;
    private boolean showArmor = true;
    private boolean showDurability = true;
    private boolean showHands = true;
    private boolean compact = false;

    private int backgroundColor = 0xAA000000;
    private int borderColor = 0xFF6A00FF;
    private int textColor = 0xFFFFFFFF;

    // Kiinteät perusmitat (ilman skaalausta)
    private static final int BASE_PADDING = 6;
    private static final int BASE_LINE_HEIGHT = 9; // fontHeight + 2
    private static final int SLOT_SIZE = 18;
    private static final int ARMOR_GAP = 4;
    private static final int HANDS_GAP = 2;
    private static final int GROUP_GAP = 8;
    private static final int EFFECTS_GAP = 3;
    private static final int CORNER_RADIUS = 4;

    private int lockedId = -1;
    private long lastSeenAtMs = 0L;
    private int lastW = 200, lastH = 70;

    public PlayerInfoHudComponent() {
        super("PlayerINFO", 10, 160);
        this.enabled = false;
    }

    // Setterit
    public void setMode(String v) { mode = v; }
    public void setLingerMs(long v) { lingerMs = Math.max(0, v); }
    public void setFadeOut(boolean v) { fadeOut = v; }
    public void setFadeMs(long v) { fadeMs = Math.max(0, v); }
    public void setMaxRange(double v) { maxRange = Math.max(0, v); }
    public void setTextScale(float v) { textScale = Math.max(0.5f, Math.min(2.0f, v)); }
    public void setBackgroundScale(float v) { backgroundScale = Math.max(0.5f, Math.min(2.0f, v)); }
    public void setShowName(boolean v) { showName = v; }
    public void setShowHealth(boolean v) { showHealth = v; }
    public void setShowDistance(boolean v) { showDistance = v; }
    public void setShowPing(boolean v) { showPing = v; }
    public void setShowEffects(boolean v) { showEffects = v; }
    public void setMaxEffects(int v) { maxEffects = clamp(v, 0, 32); }
    public void setShowArmor(boolean v) { showArmor = v; }
    public void setShowDurability(boolean v) { showDurability = v; }
    public void setShowHands(boolean v) { showHands = v; }
    public void setCompact(boolean v) { compact = v; }
    public void setBackgroundColor(int c) { backgroundColor = c; }
    public void setBorderColor(int c) { borderColor = c; }
    public void setTextColor(int c) { textColor = c; }

    @Override public boolean isModuleControlled() { return true; }
    @Override public int width(Minecraft mc) { return Math.max(1, lastW); }
    @Override public int height(Minecraft mc) { return Math.max(1, lastH); }

    @Override
    public void render(HudContext ctx, DeltaTracker tickCounter) {
        if (!enabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        Player target = resolveTarget(mc);
        if (target == null) return;
        if (maxRange > 0 && mc.player.distanceTo(target) > maxRange) {
            if (isTargetMode()) lockedId = -1;
            return;
        }
        drawHud(ctx, mc, target, 1.0f);
    }

    @Override
    public void renderEdit(HudContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) drawHud(ctx, mc, mc.player, 1.0f);
    }

    // ---------- piirto ----------
    private void drawHud(HudContext ctx, Minecraft mc, Player p, float alphaMul) {
        if (isTargetMode()) {
            Player aimed = currentAimedPlayer(mc);
            if (aimed == null && fadeOut && fadeMs > 0 && lingerMs > 0) {
                long now = System.currentTimeMillis();
                long elapsed = now - lastSeenAtMs;
                long remaining = lingerMs - elapsed;
                if (remaining < fadeMs) {
                    alphaMul = clamp01((float) remaining / (float) fadeMs);
                }
            }
        }

        // Haetaan nykyiset värit (saatu moduulista)
        Color bgCol = new Color(backgroundColor);
        Color borderCol = new Color(borderColor);
        Color txtCol = new Color(textColor);

        // Skaalatut mitat (backgroundScale vaikuttaa kaikkiin taustamittoihin)
        int padding = (int) (BASE_PADDING * backgroundScale);
        int lineH = (int) (BASE_LINE_HEIGHT * backgroundScale);
        int slotSize = (int) (SLOT_SIZE * backgroundScale);
        int armorGap = (int) (ARMOR_GAP * backgroundScale);
        int handsGap = (int) (HANDS_GAP * backgroundScale);
        int groupGap = (int) (GROUP_GAP * backgroundScale);
        int effectsGap = (int) (EFFECTS_GAP * backgroundScale);

        // Tekstin korkeus skaalautuu textScale:lla
        int underNumberH = (showArmor && showDurability) ? (int) (ctx.fontHeight() * textScale) : 0;
        int effectsUnderH = (int) (ctx.fontHeight() * textScale);

        // Tekstien tiedot
        String name = p.getName().getString();
        float hp = p.getHealth();
        float maxHp = Math.max(1, p.getMaxHealth());
        double dist = mc.player.distanceTo(p);
        String hpText = String.format("HP: %.1f / %.1f", hp, maxHp);
        String distText = String.format("Dist: %.1f m", dist);
        int ping = getPing(mc, p);
        String pingText = (ping >= 0) ? ("Ping: " + ping + " ms") : "Ping: ?";

        // Kerätään efektit
        List<MobEffectInstance> effects = null;
        boolean showEffectsRow = !compact && showEffects;
        if (showEffectsRow) {
            effects = collectEffects(p, maxEffects);
            if (effects.isEmpty()) showEffectsRow = false;
        }

        // Lasketaan tarvittava leveys ja korkeus
        int lines = 0;
        int textW = 0;

        if (showName) { lines++; textW = Math.max(textW, (int) (ctx.textWidth(name) * textScale)); }
        if (showHealth) { lines++; textW = Math.max(textW, (int) (ctx.textWidth(hpText) * textScale)); }
        if (showDistance) { lines++; textW = Math.max(textW, (int) (ctx.textWidth(distText) * textScale)); }
        if (showPing) { lines++; textW = Math.max(textW, (int) (ctx.textWidth(pingText) * textScale)); }

        int effectsRowH = showEffectsRow ? (slotSize + effectsUnderH) : 0;
        int effectsW = 0;
        if (showEffectsRow && effects != null) {
            int count = effects.size();
            effectsW = count * slotSize + (count - 1) * effectsGap;
            textW = Math.max(textW, effectsW);
        }

        boolean showItemsRow = !compact && (showArmor || showHands);
        int itemsRowH = showItemsRow ? (slotSize + underNumberH) : 0;
        int itemsW = 0;
        if (showItemsRow) {
            int armorCount = showArmor ? 4 : 0;
            int handsCount = showHands ? 2 : 0;
            if (armorCount > 0) itemsW += armorCount * slotSize + (armorCount - 1) * armorGap;
            if (handsCount > 0) itemsW += handsCount * slotSize + (handsCount - 1) * handsGap;
            if (armorCount > 0 && handsCount > 0) itemsW += groupGap;
            textW = Math.max(textW, itemsW);
        }

        if (lines == 0 && !showEffectsRow && !showItemsRow) return;

        int minW = (int) (200 * backgroundScale);
        int boxW = Math.max(minW, textW + padding * 2);
        int boxH = padding * 2 + (lines * lineH) + (showEffectsRow ? (effectsRowH + 2) : 0) + (showItemsRow ? (itemsRowH + 2) : 0);
        lastW = boxW;
        lastH = boxH;

        int x0 = x;
        int y0 = y;

        // Alpha-fade (jos käytössä)
        int bg = applyAlpha(backgroundColor, alphaMul);
        int border = applyAlpha(borderColor, alphaMul);
        int txt = applyAlpha(textColor, alphaMul);

        // Tausta (pyöristetty)
        Renderer2D.COLOR.drawRoundedRect(x0, y0, boxW, boxH, CORNER_RADIUS, new Color(bg));

        // Reunus – paksuus skaalautuu backgroundScale:lla, mutta vähintään 1px
        double thickness = Math.max(1.0, backgroundScale);
        Renderer2D.COLOR.drawRoundedRectOutline(x0, y0, boxW, boxH, CORNER_RADIUS, new Color(border), thickness);

        int tx = x0 + padding;
        int ty = y0 + padding;

        // Tekstirivit (käytetään drawScaledText)
        if (showName) {
            ctx.drawScaledText(name, tx, ty, txt, true, textScale);
            ty += lineH;
        }
        if (showHealth) {
            int hpCol = applyAlpha(hpColor(clamp01(hp / maxHp)), alphaMul);
            ctx.drawScaledText(hpText, tx, ty, hpCol, true, textScale);
            ty += lineH;
        }
        if (showDistance) {
            ctx.drawScaledText(distText, tx, ty, txt, true, textScale);
            ty += lineH;
        }
        if (showPing) {
            ctx.drawScaledText(pingText, tx, ty, txt, true, textScale);
            ty += lineH;
        }

        // Efektirivi
        if (showEffectsRow && effects != null) {
            int ix = tx;
            int iy = ty + 2;
            for (MobEffectInstance inst : effects) {
                ctx.drawVanillaEffectIcon(inst, ix, iy, slotSize, alphaMul);
                int seconds = inst.getDuration() / 20;
                String t = Integer.toString(seconds);
                int tw = (int) (ctx.textWidth(t) * textScale);
                int tx2 = ix + (slotSize - tw) / 2;
                int ty2 = iy + slotSize;
                ctx.drawScaledText(t, tx2, ty2, applyAlpha(0xFFFFFFFF, alphaMul), true, textScale);
                ix += slotSize + effectsGap;
            }
            ty = iy + effectsRowH + 2;
        }

        // Item-rivi (armor + kädet)
        if (showItemsRow) {
            int ix = tx;
            int iy = ty + 2;
            if (showArmor) {
                ix = drawArmorSlot(ctx, p.getItemBySlot(EquipmentSlot.HEAD), ix, iy, alphaMul, slotSize);
                ix += armorGap;
                ix = drawArmorSlot(ctx, p.getItemBySlot(EquipmentSlot.CHEST), ix, iy, alphaMul, slotSize);
                ix += armorGap;
                ix = drawArmorSlot(ctx, p.getItemBySlot(EquipmentSlot.LEGS), ix, iy, alphaMul, slotSize);
                ix += armorGap;
                ix = drawArmorSlot(ctx, p.getItemBySlot(EquipmentSlot.FEET), ix, iy, alphaMul, slotSize);
                ix += groupGap;
            }
            if (showHands) {
                drawStack(ctx, p.getMainHandItem(), ix, iy, slotSize);
                ix += slotSize + handsGap;
                drawStack(ctx, p.getOffhandItem(), ix, iy, slotSize);
            }
        }
    }

    private int drawArmorSlot(HudContext ctx, ItemStack stack, int x, int y, float alphaMul, int slotSize) {
        if (stack.isEmpty()) return x + slotSize;
        ctx.drawItem(stack, x, y); // vanilla item
        if (showDurability && stack.isDamageableItem()) {
            int pct = durabilityPct(stack);
            int color = getDurabilityColor(pct);
            color = applyAlpha(color, alphaMul);
            String t = Integer.toString(pct);
            int tw = (int) (ctx.textWidth(t) * textScale);
            int tx = x + (slotSize - tw) / 2;
            int ty = y + slotSize;
            ctx.drawScaledText(t, tx, ty, color, true, textScale);
        }
        return x + slotSize;
    }

    private void drawStack(HudContext ctx, ItemStack stack, int x, int y, int slotSize) {
        if (!stack.isEmpty()) {
            ctx.drawItem(stack, x, y);
        }
    }

    // ---------- apumetodit (ennallaan) ----------
    private static int getPing(Minecraft mc, Player p) {
        if (mc.getConnection() == null) return -1;
        PlayerInfo e = mc.getConnection().getPlayerInfo(p.getUUID());
        return (e != null) ? e.getLatency() : -1;
    }

    private int durabilityPct(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem()) return 0;
        int max = stack.getMaxDamage();
        if (max <= 0) return 0;
        int left = max - stack.getDamageValue();
        return (int) Math.round(left * 100.0 / max);
    }

    private int getDurabilityColor(int pct) {
        if (pct <= 20) return 0xFFFF5555;
        if (pct <= 50) return 0xFFFFFF55;
        return 0xFF55FF55;
    }

    private List<MobEffectInstance> collectEffects(Player p, int max) {
        List<MobEffectInstance> list = new ArrayList<>();
        for (MobEffectInstance inst : p.getActiveEffects()) {
            if (inst != null && inst.getDuration() > 0 && inst.showIcon()) {
                list.add(inst);
            }
        }
        list.sort(Comparator.comparingInt(MobEffectInstance::getDuration).reversed()
                .thenComparingInt(MobEffectInstance::getAmplifier).reversed());
        return list.size() > max ? list.subList(0, max) : list;
    }

    private static Player currentAimedPlayer(Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof Player p) return p;
        return null;
    }

    private Player getLockedPlayer(Minecraft mc) {
        if (lockedId < 0) return null;
        Entity e = mc.level.getEntity(lockedId);
        return (e instanceof Player p) ? p : null;
    }

    private Player resolveTarget(Minecraft mc) {
        if (!isTargetMode()) return mc.player;
        long now = System.currentTimeMillis();
        Player aimed = currentAimedPlayer(mc);
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

    private boolean isTargetMode() { return "TARGET".equalsIgnoreCase(mode); }
    private static int hpColor(float pct) {
        if (pct >= 0.66f) return 0xFF00FF00;
        if (pct >= 0.33f) return 0xFFFFFF00;
        return 0xFFFF0000;
    }
    private static float clamp01(float v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }
    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
    private static int applyAlpha(int argb, float mul) {
        int a = (argb >>> 24) & 0xFF;
        int na = clamp((int) (a * mul), 0, 255);
        return (na << 24) | (argb & 0x00FFFFFF);
    }
}