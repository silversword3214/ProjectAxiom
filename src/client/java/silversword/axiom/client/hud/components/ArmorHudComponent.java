package silversword.axiom.client.hud.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import silversword.axiom.client.hud.BaseHudElement;
import silversword.axiom.client.hud.core.HudContext;
import silversword.axiom.client.render.rendersystem.Renderer2D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;

import java.util.ArrayList;
import java.util.List;

public final class ArmorHudComponent extends BaseHudElement {
    private String mode = "SELF";
    private long lingerMs = 1500;
    private boolean fadeOut = true;
    private long fadeMs = 250;
    private double maxRange = 64.0;
    private float textScale = 1.0f;
    private float backgroundScale = 1.0f; // UUSI

    private boolean showHelmet = true;
    private boolean showChestplate = true;
    private boolean showLeggings = true;
    private boolean showBoots = true;
    private boolean showDurability = true;
    private boolean showDurabilityNumbers = true;
    private boolean compact = false;

    private SettingColor backgroundColor;
    private SettingColor borderColor;
    private SettingColor textColor;

    private int lockedId = -1;
    private long lastSeenAtMs = 0L;
    private int lastW = 100, lastH = 30;

    // Perusmitat (kun scale = 1.0)
    private static final int BASE_SLOT_SIZE = 18;
    private static final int BASE_GAP = 2;
    private static final int BASE_PADDING = 4;
    private static final int CORNER_RADIUS = 4;

    public ArmorHudComponent() {
        super("ArmorHUD", 10, 200);
        this.enabled = false;
    }

    // Setterit (värit annetaan SettingColor-olioina)
    public void setMode(String mode) { this.mode = mode; }
    public void setLingerMs(long lingerMs) { this.lingerMs = Math.max(0, lingerMs); }
    public void setFadeOut(boolean fadeOut) { this.fadeOut = fadeOut; }
    public void setFadeMs(long fadeMs) { this.fadeMs = Math.max(0, fadeMs); }
    public void setMaxRange(double maxRange) { this.maxRange = Math.max(0, maxRange); }
    public void setTextScale(float scale) { this.textScale = Math.max(0.5f, Math.min(2.0f, scale)); }
    public void setBackgroundScale(float scale) { this.backgroundScale = Math.max(0.5f, Math.min(2.0f, scale)); }
    public void setShowHelmet(boolean b) { showHelmet = b; }
    public void setShowChestplate(boolean b) { showChestplate = b; }
    public void setShowLeggings(boolean b) { showLeggings = b; }
    public void setShowBoots(boolean b) { showBoots = b; }
    public void setShowDurability(boolean b) { showDurability = b; }
    public void setShowDurabilityNumbers(boolean b) { showDurabilityNumbers = b; }
    public void setCompact(boolean b) { compact = b; }
    public void setBackgroundColor(SettingColor c) { backgroundColor = c; }
    public void setBorderColor(SettingColor c) { borderColor = c; }
    public void setTextColor(SettingColor c) { textColor = c; }

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

        List<ItemStack> armor = new ArrayList<>();
        if (showHelmet) armor.add(target.getItemBySlot(EquipmentSlot.HEAD));
        if (showChestplate) armor.add(target.getItemBySlot(EquipmentSlot.CHEST));
        if (showLeggings) armor.add(target.getItemBySlot(EquipmentSlot.LEGS));
        if (showBoots) armor.add(target.getItemBySlot(EquipmentSlot.FEET));
        if (compact) armor.removeIf(ItemStack::isEmpty);

        int count = armor.size();
        if (count == 0) return;

        // Skaalatut mitat taustalle
        int slotSize = (int) (BASE_SLOT_SIZE * backgroundScale);
        int gap = (int) (BASE_GAP * backgroundScale);
        int padding = (int) (BASE_PADDING * backgroundScale);

        int contentW = count * slotSize + (count - 1) * gap;
        int contentH = slotSize;

        int numberH = (showDurability && showDurabilityNumbers) ? (int) (ctx.fontHeight() * textScale) : 0;
        if (numberH > 0) contentH += numberH + (int) (2 * backgroundScale); // väli skaalautuu taustan mukaan

        int boxW = contentW + padding * 2;
        int boxH = contentH + padding * 2;
        lastW = boxW;
        lastH = boxH;

        int x0 = x;
        int y0 = y;

        // Haetaan nykyiset värit (rainbow-tuki)
        Color bgCol = backgroundColor.getCurrentColor();
        Color borderCol = borderColor.getCurrentColor();
        Color txtCol = textColor.getCurrentColor();

        float alpha = 1.0f; // fade-out voidaan lisätä myöhemmin

        // Tausta pyöristettynä
        Renderer2D.COLOR.drawRoundedRect(x0, y0, boxW, boxH, CORNER_RADIUS, bgCol);
        // Reunus
        Renderer2D.COLOR.drawRoundedRectOutline(x0, y0, boxW, boxH, CORNER_RADIUS, borderCol, 1.0);

        int ix = x0 + padding;
        int iy = y0 + padding;

        for (ItemStack stack : armor) {
            ctx.drawItem(stack, ix, iy); // vanilla item – ei skaalaudu

            if (showDurability && showDurabilityNumbers && !stack.isEmpty() && stack.isDamageableItem()) {
                int pct = durabilityPct(stack);
                String text = pct + "";
                int tw = (int) (ctx.textWidth(text) * textScale);
                int tx = ix + (slotSize - tw) / 2;
                int ty = iy + slotSize + (int) (2 * backgroundScale); // väli skaalautuu taustan mukaan
                int color = getDurabilityColor(pct);
                // Käytetään textCol-väriä, jos halutaan sama kuin tekstiväri, muuten pidetään durability-väri
                ctx.drawScaledText(text, tx, ty, color, true, textScale);
            }
            ix += slotSize + gap;
        }
    }

    @Override
    public void renderEdit(HudContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            int x0 = x, y0 = y;
            int w = (int) (100 * backgroundScale);
            int h = (int) (30 * backgroundScale);
            Color bgCol = backgroundColor.getCurrentColor();
            Color borderCol = borderColor.getCurrentColor();
            ctx.drawScaledText("Armor", x0 + 4, y0 + 4, textColor.getCurrentColor().getPacked(), true, textScale);
        }
    }

    // ---------- apumetodit (ennallaan) ----------
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

    private boolean isTargetMode() { return "TARGET".equalsIgnoreCase(mode); }

    private Player currentAimedPlayer(Minecraft mc) {
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
        if (aimed == null && now - lastSeenAtMs > lingerMs) {
            lockedId = -1;
            return null;
        }
        return locked;
    }
}