package silversword.axiom.client.gui.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import silversword.axiom.client.config.ResourcePackBlockerConfig;
import silversword.axiom.client.gui.core.ThemeManager;

public class AxiomConfigScreen extends Screen {
    private final Screen parent;

    public AxiomConfigScreen(Screen parent) {
        super(Component.literal("Axiom Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 4;

        // Keybinds-nappi
        this.addRenderableWidget(Button.builder(
                        Component.literal("Keybinds"),
                        button -> {
                            if (this.minecraft != null) {
                                this.minecraft.setScreen(new KeybindsScreen(this));
                            }
                        })
                .bounds(centerX - 50, y, 100, 20)
                .build());

        // Minecraftin oma Checkbox Resource Pack Blockerille
        // Parametrit: x, y, leveys, korkeus, teksti, valittu-tila
        Checkbox resourcePackCheckbox = Checkbox.builder(
                        Component.literal("Block Resource Packs"),
                        this.font
                )
                .pos(centerX - 100, y + 30)
                .selected(ResourcePackBlockerConfig.isEnabled())
                .onValueChange((checkbox, selected) -> {
                    // Päivitetään config ja tallennetaan
                    ResourcePackBlockerConfig.setEnabled(selected);
                })
                .build();

        this.addRenderableWidget(resourcePackCheckbox);

        // Takaisin-painike
        int accentColor = ThemeManager.getCurrentTheme().accent;
        this.addRenderableWidget(Button.builder(
                Component.literal("Back").withStyle(style -> style.withColor(accentColor)),
                button -> this.onClose()
        ).bounds(centerX - 50, this.height - 30, 100, 20).build());
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}