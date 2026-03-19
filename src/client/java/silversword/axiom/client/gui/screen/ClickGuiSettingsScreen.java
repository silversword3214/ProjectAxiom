package silversword.axiom.client.gui.screen;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.text.Text;
import silversword.axiom.client.config.ClickGuiConfigManager;
import silversword.axiom.client.config.ResourcePackBlockerConfig;
import silversword.axiom.client.gui.core.ThemeManager;

public class ClickGuiSettingsScreen extends Screen {
    private final Screen parent;

    public ClickGuiSettingsScreen(Screen parent) {
        super(Text.literal("ClickGui Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 4;

        // Blur checkbox
        CheckboxWidget blurCheckbox = CheckboxWidget.builder(
                        Text.literal("Blur background"),
                        this.textRenderer
                ).pos(centerX - 100, y)
                .checked(ClickGuiConfigManager.isBlurEnabled())
                .callback((checkbox, checked) -> {
                    ClickGuiConfigManager.setBlurEnabled(checked);
                })
                .build();
        this.addDrawableChild(blurCheckbox);

        // Theme settings
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Theme Settings"),
                button -> {
                    if (this.client != null) {
                        this.client.setScreen(new ThemeSettingsScreen(this));
                    }
                }
        ).dimensions(centerX - 100, y + 30, 200, 20).build());

        // Block server respack
        CheckboxWidget packBlockerCheckbox = CheckboxWidget.builder(
                        Text.literal("Block Server Resource Packs"),
                        this.textRenderer
                ).pos(centerX - 100, y + 60)
                .checked(ResourcePackBlockerConfig.isEnabled())
                .callback((checkbox, checked) -> {
                    ResourcePackBlockerConfig.setEnabled(checked);
                })
                .build();
        this.addDrawableChild(packBlockerCheckbox);



        // Back – käyttää accent-väriä
        int accentColor = ThemeManager.getCurrentTheme().accent;
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Back").styled(style -> style.withColor(accentColor)),
                button -> this.close()
        ).dimensions(centerX - 50, this.height - 30, 100, 20).build());
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}