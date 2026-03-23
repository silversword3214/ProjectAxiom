package silversword.axiom.mixin.client.gui;

import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.gui.screen.AxiomConfigScreen;
import silversword.axiom.client.gui.core.ThemeManager;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {

    protected PauseScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void axiom$addButton(CallbackInfo ci) {
        // Etsitään alin nappi (oletettavasti Quit Game)
        Button lowestButton = null;
        int maxY = -1;
        for (var child : this.children()) {
            if (child instanceof Button button) {
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
        Component buttonText = Component.literal("Axiom")
                .withStyle(style -> style.withColor(accentColor));

        this.addRenderableWidget(
                Button.builder(buttonText, button -> {
                            Minecraft.getInstance().setScreen(new AxiomConfigScreen(this));
                        })
                        .bounds(x, y, width, height)
                        .build()
        );
    }
}