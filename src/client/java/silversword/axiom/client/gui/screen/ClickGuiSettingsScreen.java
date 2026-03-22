package silversword.axiom.client.gui.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;
import silversword.axiom.client.config.ClickGuiConfigManager;
import silversword.axiom.client.config.ResourcePackBlockerConfig;
import silversword.axiom.client.gui.core.ThemeManager;

public class ClickGuiSettingsScreen extends Screen {
    private final Screen parent;

    public ClickGuiSettingsScreen(Screen parent) {
        super(Component.literal("ClickGui Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 4;

        // Blur checkbox
        Checkbox blurCheckbox = Checkbox.builder(
                        Component.literal("Blur background"),
                        this.font
                ).pos(centerX - 100, y)
                .selected(ClickGuiConfigManager.isBlurEnabled())
                .onValueChange((checkbox, checked) -> {
                    ClickGuiConfigManager.setBlurEnabled(checked);
                })
                .build();
        this.addRenderableWidget(blurCheckbox);

        // Theme settings
        this.addRenderableWidget(Button.builder(
                Component.literal("Theme Settings"),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new ThemeSettingsScreen(this));
                    }
                }
        ).bounds(centerX - 100, y + 30, 200, 20).build());

        // Block server respack
        Checkbox packBlockerCheckbox = Checkbox.builder(
                        Component.literal("Block Server Resource Packs"),
                        this.font
                ).pos(centerX - 100, y + 60)
                .selected(ResourcePackBlockerConfig.isEnabled())
                .onValueChange((checkbox, checked) -> {
                    ResourcePackBlockerConfig.setEnabled(checked);
                })
                .build();
        this.addRenderableWidget(packBlockerCheckbox);



        // Back – käyttää accent-väriä
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