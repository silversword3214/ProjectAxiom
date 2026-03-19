package silversword.axiom.mixin.client.gui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.modules.player.ChestStealer;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.Renderer2D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.utils.render.TextUtils;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow protected int backgroundWidth;
    @Shadow public abstract ScreenHandler getScreenHandler();

    // Napin mitat
    private static final int BUTTON_WIDTH = 50;
    private static final int BUTTON_HEIGHT = 15;

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ChestStealer stealer = ChestStealer.INSTANCE;
        if (stealer == null || !stealer.isEnabled()) return;

        ScreenHandler handler = getScreenHandler();
        boolean isValid = handler instanceof GenericContainerScreenHandler ||
                handler instanceof ShulkerBoxScreenHandler;
        if (!isValid) return;

        int buttonX = this.x + this.backgroundWidth - 60;
        int buttonY = this.y - 20;

        boolean hovered = mouseX >= buttonX && mouseX <= buttonX + BUTTON_WIDTH &&
                mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT;

        // Taustaväri: vihreä, hover-tilassa vaaleampi
        Color bgColor = hovered ? new Color(200, 0, 0, 255) : new Color(150, 0, 0, 255);
        Color borderColor = new Color(255, 255, 255, 200);

        // Piirretään pyöristetty tausta Renderer2D.COLOR:lla
        Renderer2D.COLOR.begin();
        Renderer2D.COLOR.drawRoundedRect(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, 3, bgColor);
        Renderer2D.COLOR.drawRoundedRectOutline(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, 3, borderColor, 1);
        Renderer2D.COLOR.end();
        Renderer2D.COLOR.render();

        // Teksti käyttäen TextRenderer-järjestelmää (sama fontti kuin clickguissa)
        TextRenderer textRenderer = TextRenderer.get();
        textRenderer.begin(0.90, false, true);
        String text = "Steal";
        int textWidth = (int) textRenderer.getWidth(text, false);
        int textX = buttonX + (BUTTON_WIDTH - textWidth) / 2 - 4;
        int textY = buttonY + (BUTTON_HEIGHT - TextUtils.FONT_HEIGHT) / 2;
        textRenderer.render(text, textX, textY, new Color(255, 255, 255, 255), false);
        textRenderer.end();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        ChestStealer stealer = ChestStealer.INSTANCE;
        if (stealer == null || !stealer.isEnabled() || click.button() != 0) return;

        ScreenHandler handler = getScreenHandler();
        // --- LISÄYS: Hyväksytään myös shulker-handler ---
        boolean isValid = handler instanceof GenericContainerScreenHandler ||
                handler instanceof ShulkerBoxScreenHandler;
        if (!isValid) return;

        int buttonX = this.x + this.backgroundWidth - 60;
        int buttonY = this.y - 20;

        if (click.x() >= buttonX && click.x() <= buttonX + BUTTON_WIDTH &&
                click.y() >= buttonY && click.y() <= buttonY + BUTTON_HEIGHT) {
            stealer.stealAll(handler);
            cir.setReturnValue(true);
        }
    }
}