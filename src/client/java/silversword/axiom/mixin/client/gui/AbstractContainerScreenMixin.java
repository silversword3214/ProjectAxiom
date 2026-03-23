package silversword.axiom.mixin.client.gui;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.modules.player.ChestStealer;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.axiomrenderer.RenderAPI;
import silversword.axiom.client.render.rendersystem.axiomrenderer.core.RenderCore;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.utils.render.TextUtils;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;
    @Shadow public abstract AbstractContainerMenu getMenu();

    private static final int BUTTON_WIDTH = 50;
    private static final int BUTTON_HEIGHT = 15;

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ChestStealer stealer = ChestStealer.INSTANCE;
        if (stealer == null || !stealer.isEnabled()) return;

        AbstractContainerMenu handler = getMenu();
        boolean isValid = handler instanceof ChestMenu ||
                handler instanceof ShulkerBoxMenu;
        if (!isValid) return;

        int buttonX = this.leftPos + this.imageWidth - 60;
        int buttonY = this.topPos - 20;

        boolean hovered = mouseX >= buttonX && mouseX <= buttonX + BUTTON_WIDTH &&
                mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT;

        // Taustaväri (ARGB-int)
        int bgColor = hovered ? 0xFFC80000 : 0xFF960000; // punainen
        int borderColor = 0xFFC8C8C8; // vaaleanharmaa

        RenderCore core = RenderAPI.getInstance().getCore();

        // Piirrä pyöristetty tausta ja reunus
        core.addRoundedRect(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, 3, bgColor);
        core.addRoundedRectOutline(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, 3, 1.0f, borderColor);

        // Teksti (käyttää uutta TextRenderer-rajapintaa)
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
    private void onMouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        ChestStealer stealer = ChestStealer.INSTANCE;
        if (stealer == null || !stealer.isEnabled() || click.button() != 0) return;

        AbstractContainerMenu handler = getMenu();
        boolean isValid = handler instanceof ChestMenu ||
                handler instanceof ShulkerBoxMenu;
        if (!isValid) return;

        int buttonX = this.leftPos + this.imageWidth - 60;
        int buttonY = this.topPos - 20;

        if (click.x() >= buttonX && click.x() <= buttonX + BUTTON_WIDTH &&
                click.y() >= buttonY && click.y() <= buttonY + BUTTON_HEIGHT) {
            stealer.stealAll(handler);
            cir.setReturnValue(true);
        }
    }
}