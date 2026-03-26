package silversword.axiom.client.modules.moduleutils.noparticle;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import silversword.axiom.client.gui.components.Toggle;
import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;
import silversword.axiom.client.modules.render.NoParticleModule;

public class ParticleEntry implements silversword.axiom.client.gui.components.UiComponent {

    private Rect bounds;
    private final Identifier id;
    private final NoParticleModule module;
    private final Toggle toggle;
    private final String displayName;

    public ParticleEntry(Identifier id, NoParticleModule module) {
        this.id = id;
        this.module = module;
        this.displayName = getReadableName(id);
        this.toggle = new Toggle(displayName,
                () -> !module.isParticleDisabled(id),
                enabled -> module.setParticleDisabled(id, !enabled));
    }

    private String getReadableName(Identifier id) {
        String path = id.getPath();
        // Replace underscores with spaces
        String name = path.replace('_', ' ');
        // Capitalize first letter
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
        toggle.setBounds(bounds);
    }

    @Override
    public int getPreferredHeight() { return 18; }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        toggle.render(ui, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        return toggle.mouseClicked(ui, mouseX, mouseY, button);
    }

    @Override
    public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {
        toggle.mouseReleased(ui, mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) {
        return false;
    }

    @Override
    public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) {
        return false;
    }

    @Override
    public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public boolean charTyped(UiContext ui, char chr, int modifiers) {
        return false;
    }
}