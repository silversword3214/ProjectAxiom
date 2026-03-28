package silversword.axiom.client.hud.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.item.ItemStack;
import silversword.axiom.client.hud.BaseHudElement;
import silversword.axiom.client.hud.core.HudContext;
import silversword.axiom.client.modules.NamedColor;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingNumber;

public final class InventoryViewer extends BaseHudElement {
    private final SettingNumber scale;
    private final SettingNumber backgroundPadding;
    private final SettingNumber backgroundRadius;
    private final SettingNumber outlineThickness;
    private final SettingColor backgroundColor;
    private final SettingColor borderColor;


    private static final int SLOT_SIZE = 18;
    private static final int COLUMNS = 9;
    private static final int INVENTORY_ROWS = 3;
    private static final int TOTAL_ROWS = INVENTORY_ROWS;

    public InventoryViewer() {
        super("Inventory Viewer", 10, 10);

        scale = new SettingNumber("Scale", 0.5, 2.0, 0.1, 1.0);
        backgroundPadding = new SettingNumber("Background Padding", 0, 20, 1, 4);
        backgroundRadius = new SettingNumber("Background Radius", 0, 10, 1, 4);
        outlineThickness = new SettingNumber("Outline Thickness", 0.5, 5.0, 0.5, 1.0);
        backgroundColor = new SettingColor("Background Color", new Color(0x80000000));
        borderColor = new SettingColor("Border Color", new Color(0xFFAAAAAA));


        settings.addSetting(scale);
        settings.addSetting(backgroundPadding);
        settings.addSetting(backgroundRadius);
        settings.addSetting(outlineThickness);
        settings.addNamedColor(new NamedColor("Background", backgroundColor));
        settings.addNamedColor(new NamedColor("Border", borderColor));
    }

    @Override
    public int width(Minecraft mc) {
        double s = scale.getValue();
        int slotSize = (int) (SLOT_SIZE * s);
        return COLUMNS * slotSize + (int) (backgroundPadding.getValue() * 2);
    }

    @Override
    public int height(Minecraft mc) {
        double s = scale.getValue();
        int slotSize = (int) (SLOT_SIZE * s);
        return TOTAL_ROWS * slotSize + (int) (backgroundPadding.getValue() * 2);
    }

    @Override
    public void render(HudContext ctx, DeltaTracker tickCounter) {
        if (ctx.mc.player == null) return;

        double s = scale.getValue();
        int slotSize = (int) (SLOT_SIZE * s);
        int padding = (int) backgroundPadding.getValue();
        int radius = (int) backgroundRadius.getValue();
        float thickness = (float) outlineThickness.getValue();

        int bgW = width(ctx.mc);
        int bgH = height(ctx.mc);
        int bgX = x;
        int bgY = y;

        // 1. Tausta
        if (radius > 0) {
            ctx.fillRounded(bgX, bgY, bgW, bgH, radius, backgroundColor.getCurrentColor().getARGB());
        } else {
            ctx.fill(bgX, bgY, bgW, bgH, backgroundColor.getCurrentColor().getARGB());
        }

        // 2. Reunus (piirtyy esineiden päälle, koska esineet piirretään seuraavaksi)
        if (borderColor.getCurrentColor().getAlpha() != 0 && thickness > 0) {
            if (radius > 0) {
                ctx.drawRoundedOutline(bgX, bgY, bgW, bgH, radius, borderColor.getCurrentColor().getARGB(), thickness);
            } else {
                ctx.drawOutline(bgX, bgY, bgW, bgH, borderColor.getCurrentColor().getARGB(), thickness);
            }
        }

        int startX = bgX + padding;
        int startY = bgY + padding;

        // 3. Esineet (vanilla-piirto)
        // Inventaario (rivit 0–2, slotit 9–35)
        for (int row = 0; row < INVENTORY_ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int slotIndex = 9 + row * 9 + col;
                ItemStack stack = ctx.mc.player.getInventory().items.get(slotIndex);
                if (!stack.isEmpty()) {
                    drawItem(ctx, stack, startX + col * slotSize, startY + row * slotSize, slotSize);
                }
            }
        }
    }
    /**
     * Piirtää esineen kuvakkeen vanillan ItemRendererillä.
     * @param ctx  HudContext, joka tarjoaa drawItem-metodin
     * @param stack  esine
     * @param x  vasen yläkulma (sijainti taustan sisällä)
     * @param y  vasen yläkulma
     * @param size  slottikoko (ei vaikuta itse kuvakkeeseen, koska vanilla piirtää 16×16)
     */
    private void drawItem(HudContext ctx, ItemStack stack, int x, int y, int size) {
        if (stack.isEmpty()) return;
        ctx.item(stack, x, y, size);
    }

    /**
     * Piirtää lukumäärätekstin (esim. "64") skaalattuna.
     * Teksti lisätään textEntries-listaan ja piirretään myöhemmin.
     */


    @Override
    public void renderEdit(HudContext ctx) {
        render(ctx, null);
    }
}