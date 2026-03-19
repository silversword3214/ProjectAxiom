package silversword.axiom.mixin.client.gui;

import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.gui.screen.AxiomConfigScreen;
import silversword.axiom.client.gui.core.ThemeManager;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {

    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void axiom$addButton(CallbackInfo ci) {
        // Etsitään alin nappi (oletettavasti Quit Game)
        ButtonWidget lowestButton = null;
        int maxY = -1;
        for (var child : this.children()) {
            if (child instanceof ButtonWidget button) {
                int buttonY = button.getY();
                if (buttonY > maxY) {
                    maxY = buttonY;
                    lowestButton = button;
                }
            }
        }

        if (lowestButton == null) return;

        int x = lowestButton.getX();
        int y = lowestButton.getY() + lowestButton.getHeight() + 4;
        int width = lowestButton.getWidth();
        int height = lowestButton.getHeight();

        int accentColor = ThemeManager.getCurrentTheme().accent;
        Text buttonText = Text.literal("Axiom")
                .styled(style -> style.withColor(accentColor));

        this.addDrawableChild(
                ButtonWidget.builder(buttonText, button -> {
                            MinecraftClient.getInstance().setScreen(new AxiomConfigScreen(this));
                        })
                        .dimensions(x, y, width, height)
                        .build()
        );
    }
}