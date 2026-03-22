package silversword.axiom.client.gui.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.hidden.Keybinds;
import silversword.axiom.client.utils.KeyNames;
import silversword.axiom.client.gui.core.ThemeManager;

public class KeybindsScreen extends Screen {
    private final Screen parent;
    private Keybinds keybinds;
    private Button clickGuiKeyButton;
    private boolean waitingForKey = false;

    protected KeybindsScreen(Screen parent) {
        super(Component.literal("Keybinds"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        keybinds = ModuleManager.getInstance().getModule(Keybinds.class);
        if (keybinds == null) return;

        int centerX = this.width / 2;
        int y = this.height / 4;

        // ClickGUI Key -nappi
        clickGuiKeyButton = Button.builder(
                        Component.literal("ClickGUI: " + KeyNames.get(keybinds.clickGuiKey.get())),
                        button -> {
                            waitingForKey = true;
                            button.setMessage(Component.literal("Press any key..."));
                        })
                .bounds(centerX - 100, y, 200, 20)
                .build();
        this.addRenderableWidget(clickGuiKeyButton);

        // Back – käyttää accent-väriä
        int accentColor = ThemeManager.getCurrentTheme().accent;
        this.addRenderableWidget(Button.builder(
                Component.literal("Back").withStyle(style -> style.withColor(accentColor)),
                button -> this.onClose()
        ).bounds(centerX - 50, this.height - 30, 100, 20).build());
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (waitingForKey) {
            System.out.println("Setting key to: " + input.key());
            keybinds.clickGuiKey.set(input.key());
            waitingForKey = false;
            clickGuiKeyButton.setMessage(Component.literal("ClickGUI Key: " + KeyNames.get(input.key())));
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}