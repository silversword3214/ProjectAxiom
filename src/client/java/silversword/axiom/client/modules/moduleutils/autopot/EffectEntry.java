package silversword.axiom.client.modules.moduleutils.autopot;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import silversword.axiom.client.gui.components.ActionButton;
import silversword.axiom.client.gui.components.Toggle;
import silversword.axiom.client.gui.components.UiComponent;
import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;
import silversword.axiom.client.modules.combat.PotionRefill;

import java.util.Optional;

public final class EffectEntry implements UiComponent {

    private Rect bounds;
    private final Identifier effectId;
    private final PotionRefill module;
    private final Toggle toggle;

    private final ActionButton minusBtn;
    private final ActionButton plusBtn;

    public EffectEntry(Identifier effectId, PotionRefill module) {
        this.effectId = effectId;
        this.module = module;

        String displayName = getReadableName(effectId);

        this.toggle = new Toggle(displayName,
                () -> module.isEffectSelected(effectId),
                enabled -> module.setEffectSelected(effectId, enabled));

        this.minusBtn = new ActionButton("", () -> {
            int current = module.getTargetAmount(effectId);
            if (current > 1) module.setTargetAmount(effectId, current - 1);
        });

        this.plusBtn = new ActionButton("", () -> {
            int current = module.getTargetAmount(effectId);
            if (current < 64) module.setTargetAmount(effectId, current + 1);
        });
    }

    private String getReadableName(Identifier id) {
        Optional<Holder.Reference<MobEffect>> optional = BuiltInRegistries.MOB_EFFECT.get(id);
        if (optional.isPresent()) {
            return optional.get().value().getDisplayName().getString();
        }
        String name = id.getPath().replace('_', ' ');
        if (!name.isEmpty()) {
            name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        return name;
    }

    @Override
    public Rect getBounds() { return bounds; }

    @Override
    public void setBounds(Rect bounds) {
        this.bounds = bounds;
        int btnSize = 14;
        int centerY = bounds.y + (bounds.h - btnSize) / 2;

        toggle.setBounds(new Rect(bounds.x, bounds.y, bounds.w - 65, bounds.h));

        minusBtn.setBounds(new Rect(bounds.x + bounds.w - 55, centerY, btnSize, btnSize));
        plusBtn.setBounds(new Rect(bounds.x + bounds.w - 18, centerY, btnSize, btnSize));
    }

    @Override
    public int getPreferredHeight() { return 18; }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        toggle.render(ui, mouseX, mouseY, delta);

        if (module.isEffectSelected(effectId)) {
            int normalColor = 0x40000000;
            int hoverColor = 0x80000000;
            double radius = 3.0;

            ui.fillRounded(minusBtn.getBounds(), minusBtn.getBounds().contains(mouseX, mouseY) ? hoverColor : normalColor, radius);
            ui.fillRounded(plusBtn.getBounds(), plusBtn.getBounds().contains(mouseX, mouseY) ? hoverColor : normalColor, radius);

            int textARGB = 0xFFFFFFFF;
            int fontH = ui.fontHeight();

            Rect m = minusBtn.getBounds();
            ui.centeredText("-", m.x + m.w / 2, m.y + m.h / 2 - fontH / 2 + 4, textARGB);

            Rect p = plusBtn.getBounds();
            ui.centeredText("+", p.x + p.w / 2, p.y + p.h / 2 - fontH / 2 + 4, textARGB);

            String amount = String.valueOf(module.getTargetAmount(effectId));
            int textCenterX = (m.right() + p.x) / 2;
            ui.centeredText(amount, textCenterX, bounds.y + bounds.h / 2 - fontH / 2 + 4, textARGB);

            minusBtn.render(ui, mouseX, mouseY, delta);
            plusBtn.render(ui, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (toggle.mouseClicked(ui, mouseX, mouseY, button)) return true;
        if (module.isEffectSelected(effectId)) {
            if (minusBtn.mouseClicked(ui, mouseX, mouseY, button)) return true;
            if (plusBtn.mouseClicked(ui, mouseX, mouseY, button)) return true;
        }
        return false;
    }

    @Override public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {}
    @Override public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) { return false; }
    @Override public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) { return false; }
    @Override public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) { return false; }
    @Override public boolean charTyped(UiContext ui, char chr, int modifiers) { return false; }
}