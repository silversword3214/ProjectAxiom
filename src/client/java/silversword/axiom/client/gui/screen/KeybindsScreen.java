package silversword.axiom.client.gui.screen;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.hidden.Keybinds;
import silversword.axiom.client.utils.KeyNames;
import silversword.axiom.client.gui.core.ThemeManager;

public class KeybindsScreen extends Screen {
    private final Screen parent;
    private Keybinds keybinds;
    private ButtonWidget clickGuiKeyButton;
    private boolean waitingForKey = false;

    protected KeybindsScreen(Screen parent) {
        super(Text.literal("Keybinds"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        keybinds = ModuleManager.getInstance().getModule(Keybinds.class);
        if (keybinds == null) return;

        int centerX = this.width / 2;
        int y = this.height / 4;

        // ClickGUI Key -nappi
        clickGuiKeyButton = ButtonWidget.builder(
                        Text.literal("ClickGUI: " + KeyNames.get(keybinds.clickGuiKey.get())),
                        button -> {
                            waitingForKey = true;
                            button.setMessage(Text.literal("Press any key..."));
                        })
                .dimensions(centerX - 100, y, 200, 20)
                .build();
        this.addDrawableChild(clickGuiKeyButton);

        // Back – käyttää accent-väriä
        int accentColor = ThemeManager.getCurrentTheme().accent;
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Back").styled(style -> style.withColor(accentColor)),
                button -> this.close()
        ).dimensions(centerX - 50, this.height - 30, 100, 20).build());
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (waitingForKey) {
            System.out.println("Setting key to: " + input.key());
            keybinds.clickGuiKey.set(input.key());
            waitingForKey = false;
            clickGuiKeyButton.setMessage(Text.literal("ClickGUI Key: " + KeyNames.get(input.key())));
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}