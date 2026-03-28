package silversword.axiom.client.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import silversword.axiom.client.config.HudConfigManager;
import silversword.axiom.client.gui.core.Theme;
import silversword.axiom.client.gui.core.ThemeManager;
import silversword.axiom.client.gui.core.UiContext;
import silversword.axiom.client.hud.core.HudContext;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.axiomrenderer.RenderAPI;
import silversword.axiom.client.render.rendersystem.axiomrenderer.core.RenderCore;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer2D;
import silversword.axiom.client.hud.*;
import silversword.axiom.client.render.rendersystem.utils.render.RenderUtils;

public final class HudEditScreen extends Screen {
    private static final int GRID_SIZE = 10;
    private static final int BACKGROUND_COLOR = 0x80000000; // 50% musta
    private static final int GRID_COLOR = 0x40FFFFFF;      // 25% valkoinen
    private static final int OUTLINE_NORMAL = 0xFFB0BEC5;
    private static final int OUTLINE_HOVER = 0xFFFFD54F;
    private static final int OUTLINE_DRAGGING = 0xFFFFA726;

    private HudElement dragging = null;
    private int dragOffX, dragOffY;
    private int lastMouseX, lastMouseY;
    private boolean gridVisible = false;

    public HudEditScreen() {
        super(Component.literal("HUD Edit"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // Estetään vaniljan taustan piirto
    }

    @Override
    public void render(GuiGraphics vanillaCtx, int mouseX, int mouseY, float delta) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        Matrix4f proj = RenderUtils.getScaledProjection(vanillaCtx);
        Minecraft mc = Minecraft.getInstance();
        Theme theme = ThemeManager.getCurrentTheme();

        // Käytetään Screen-luokan valmiita pikselimittoja (this.width, this.height)

        Renderer2D renderer = new Renderer2D(vanillaCtx, RenderAPI.getInstance().getCore(), proj);

        // Luo UiContext ja HudContext
        UiContext uiCtx = new UiContext(mc, vanillaCtx, theme, delta, renderer);
        HudContext hudCtx = new HudContext(mc, vanillaCtx, theme, delta, renderer);

        // 1. Piirrä läpikuultava tausta (koko ruudun kokoinen)
        uiCtx.fill(0, 0, width, height, BACKGROUND_COLOR);

        // 2. Piirrä grid (vain jos raahataan)
        if (gridVisible) {
            drawGrid(uiCtx);
        }

        // 3. Piirrä HUD-elementit (renderEdit)
        for (HudElement e : HudManager.get().elements()) {
            if (!e.enabled()) continue;
            e.renderEdit(hudCtx);
        }

        // 4. Piirrä outlinet
        for (HudElement e : HudManager.get().elements()) {
            if (!e.enabled()) continue;
            drawOutline(uiCtx, e, mouseX, mouseY);
        }

        // 5. Piirrä tekstit
        TextRenderer.get().begin(1.0, false, false);

        // 6. Piirrä elementtien nimet
        for (HudElement e : HudManager.get().elements()) {
            if (!e.enabled()) continue;
            drawElementName(uiCtx, e);
        }

        TextRenderer.get().end();

        RenderAPI.getInstance().getCore().flush();

        super.render(vanillaCtx, mouseX, mouseY, delta);
    }

    private void drawGrid(UiContext ctx) {
        RenderCore core = RenderAPI.getInstance().getCore();
        // Pystysuorat viivat
        for (int x = 0; x < width; x += GRID_SIZE) {
            core.addLine2D(x, 0, x, height, 1.0f, GRID_COLOR);
        }
        // Vaakasuorat viivat
        for (int y = 0; y < height; y += GRID_SIZE) {
            core.addLine2D(0, y, width, y, 1.0f, GRID_COLOR);
        }
    }

    private void drawOutline(UiContext ctx, HudElement e, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        int x = e.x();
        int y = e.y();
        int w = Math.max(1, e.width(mc));
        int h = Math.max(1, e.height(mc));

        int color;
        if (e == dragging) {
            color = OUTLINE_DRAGGING;
        } else if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
            color = OUTLINE_HOVER;
        } else {
            color = OUTLINE_NORMAL;
        }

        ctx.drawOutline(x, y, w, h, color);
    }

    private void drawElementName(UiContext ctx, HudElement e) {
        Minecraft mc = Minecraft.getInstance();
        int x = e.x();
        int y = e.y();
        ctx.text(e.id(), x, y - 10, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);

        int mx = (int) click.x();
        int my = (int) click.y();

        HudElement hit = HudManager.get().hitTest(mx, my);
        if (hit != null) {
            dragging = hit;
            dragOffX = mx - hit.x();
            dragOffY = my - hit.y();
            gridVisible = true;
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
        if (dragging == null) return super.mouseDragged(click, offsetX, offsetY);
        if (click.button() != 0) return super.mouseDragged(click, offsetX, offsetY);

        Minecraft mc = Minecraft.getInstance();
        int newX = (int) click.x() - dragOffX;
        int newY = (int) click.y() - dragOffY;

        int w = Math.max(1, dragging.width(mc));
        int h = Math.max(1, dragging.height(mc));

        // Snap gridiin
        newX = snapToGrid(newX);
        newY = snapToGrid(newY);

        // Pysy ruudun sisällä (käytetään this.width/this.height)
        newX = clamp(newX, 0, this.width - w);
        newY = clamp(newY, 0, this.height - h);

        dragging.setPos(newX, newY);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (click.button() == 0) {
            dragging = null;
            gridVisible = false;
        }
        return super.mouseReleased(click);
    }

    private int snapToGrid(int value) {
        return Math.round(value / (float) GRID_SIZE) * GRID_SIZE;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.isEscape()) {
            HudConfigManager.save(HudManager.get());
            onClose();
            return true;
        }
        if (input.key() == 261) { // DELETE
            HudElement target = dragging;
            if (target == null) {
                target = HudManager.get().hitTest(lastMouseX, lastMouseY);
            }
            if (target != null) {
                target.setEnabled(!target.enabled());
                HudConfigManager.save(HudManager.get());
                return true;
            }
        }
        return super.keyPressed(input);
    }
}