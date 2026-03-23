package silversword.axiom.mixin.client.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        // Etsitään "Quit Game" -nappi - oletetaan, että se on alin nappi
        Button quitButton = null;
        int maxY = 0;
        for (var child : this.children()) {
            if (child instanceof Button button && button.getY() > maxY) {
                maxY = button.getY();
                quitButton = button;
            }
        }

        if (quitButton != null) {
            // Luo teksti liilalla
            Component text = Component.literal("Obsidian Client")
                    .withStyle(style -> style.withColor(0xFF55FF));
            int textWidth = this.font.width(text);
            int x = quitButton.getX() + quitButton.getWidth() / 2 - textWidth / 2;
            int y = quitButton.getY() + quitButton.getHeight() + 5;
            StringWidget textWidget = new StringWidget(x, y, textWidth, 10, text, this.font);
            this.addRenderableWidget(textWidget);
        }
    }
}