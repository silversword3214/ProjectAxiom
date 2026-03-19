package silversword.axiom.mixin.client.gui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        // Etsitään "Quit Game" -nappi - oletetaan, että se on alin nappi
        ButtonWidget quitButton = null;
        int maxY = 0;
        for (var child : this.children()) {
            if (child instanceof ButtonWidget button && button.getY() > maxY) {
                maxY = button.getY();
                quitButton = button;
            }
        }

        if (quitButton != null) {
            // Luo teksti liilalla
            Text text = Text.literal("Obsidian Client")
                    .styled(style -> style.withColor(0xFF55FF));
            int textWidth = this.textRenderer.getWidth(text);
            int x = quitButton.getX() + quitButton.getWidth() / 2 - textWidth / 2;
            int y = quitButton.getY() + quitButton.getHeight() + 5;
            TextWidget textWidget = new TextWidget(x, y, textWidth, 10, text, this.textRenderer);
            this.addDrawableChild(textWidget);
        }
    }
}