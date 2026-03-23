package silversword.axiom.client.modules.moduleutils;

import net.minecraft.world.level.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import silversword.axiom.client.gui.components.HsvColorPicker;
import silversword.axiom.client.gui.components.UiComponent;
import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;

public class BlockEntry implements UiComponent {
    private final Block block;
    private final BlockSelectable module;
    private Rect bounds;
    private Rect colorSwatchRect;
    private boolean hovered = false;
    private boolean colorHovered = false;

    public BlockEntry(Block block, BlockSelectable module) {
        this.block = block;
        this.module = module;
    }

    @Override
    public Rect getBounds() { return bounds; }

    @Override
    public void setBounds(Rect bounds) {
        this.bounds = bounds;
        int swatchSize = 16;
        colorSwatchRect = new Rect(
                bounds.right() - swatchSize - 4,
                bounds.y + (bounds.h - swatchSize) / 2,
                swatchSize,
                swatchSize
        );
    }

    @Override
    public int getPreferredHeight() { return 20; }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        hovered = bounds.contains(mouseX, mouseY) && !colorSwatchRect.contains(mouseX, mouseY);
        colorHovered = colorSwatchRect.contains(mouseX, mouseY);

        boolean selected = module.isBlockSelected(block);

        int bg;
        if (selected) {
            bg = ui.theme.accentColor();
        } else {
            bg = hovered ? ui.theme.buttonHover : ui.theme.button;
        }
        ui.fill(bounds, bg);

        ItemStack stack = block.asItem().getDefaultInstance();
        if (!stack.isEmpty()) {
            ui.draw.renderItem(stack, bounds.x + 2, bounds.y + 2);
        }

        String name = block.getName().getString();
        ui.text(name, bounds.x + 22, bounds.y + 6, ui.theme.text);

        // Väriläikkä (jos moduuli tukee värejä)
        if (module instanceof BlockColorSelectable) {
            BlockColorSelectable colorModule = (BlockColorSelectable) module;
            SettingColor sc = colorModule.getBlockColor(block);
            int blockColor = sc != null ? sc.getCurrentColor().getARGB() : 0xFFFFFFFF;

            ui.fill(colorSwatchRect.x - 1, colorSwatchRect.y - 1,
                    colorSwatchRect.w + 2, colorSwatchRect.h + 2,
                    colorHovered ? 0xFFFFFFFF : 0xFF888888);
            ui.fill(colorSwatchRect, blockColor);
        }
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        if (module instanceof BlockColorSelectable && colorSwatchRect.contains(mouseX, mouseY)) {
            openColorPicker((BlockColorSelectable) module);
            return true;
        }

        if (bounds.contains(mouseX, mouseY)) {
            module.toggleBlock(block);
            return true;
        }
        return false;
    }

    private void openColorPicker(BlockColorSelectable colorModule) {
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;

        final SettingColor current;
        SettingColor existing = colorModule.getBlockColor(block);
        if (existing != null) {
            current = existing;
        } else {
            current = new SettingColor("temp", new Color(0xFFFFFFFF));
            colorModule.setBlockColor(block, current);
        }

        SettingColor temp = new SettingColor("temp", current.copy());
        temp.rainbow = current.rainbow;
        temp.speed = current.speed;

        HsvColorPicker picker = new HsvColorPicker(temp, () -> {
            current.set(temp.r, temp.g, temp.b, temp.a);
            current.rainbow = temp.rainbow;
            current.speed = temp.speed;
            colorModule.setBlockColor(block, current);
        });

        Minecraft mc = Minecraft.getInstance();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        factory.openCustomWindow("block_color_picker", "Choose Color for " + block.getName().getString(), sw, sh, picker);
    }

    @Override public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) { return false; }
    @Override public boolean charTyped(UiContext ui, char chr, int modifiers) { return false; }
    @Override public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) { return false; }
    @Override public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {}
    @Override public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) { return false; }
}