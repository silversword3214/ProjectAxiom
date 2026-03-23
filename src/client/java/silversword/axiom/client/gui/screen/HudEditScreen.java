package silversword.axiom.client.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import silversword.axiom.client.config.HudConfigManager;
import silversword.axiom.client.gui.core.Theme;
import silversword.axiom.client.gui.core.ThemeManager;
import silversword.axiom.client.gui.core.UiContext;
import silversword.axiom.client.hud.core.HudContext;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.Renderer2D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.render.RenderUtils;
import silversword.axiom.client.hud.*;

public final class HudEditScreen extends Screen {
    private static final int GRID_SIZE = 10;
    private static final Color BACKGROUND_COLOR = new Color(0x80000000); // 50% musta
    private static final Color GRID_COLOR = new Color(0x40FFFFFF);      // 25% valkoinen
    private static final Color OUTLINE_NORMAL = new Color(0xFFB0BEC5);
    private static final Color OUTLINE_HOVER = new Color(0xFFFFD54F);
    private static final Color OUTLINE_DRAGGING = new Color(0xFFFFA726);

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
        Minecraft mc = Minecraft.getInstance();
        Theme theme = ThemeManager.getCurrentTheme();

        // Aseta 2D-projektio
        RenderUtils.setup2DProjection(this.width, this.height);

        // Aloita taustojen keräys
        Renderer2D.COLOR.begin();

        // Luo UiContext GUI-elementtejä varten
        UiContext uiCtx = new UiContext(mc, vanillaCtx, theme, delta);
        // Luo HudContext HUD-elementtien piirtoa varten
        HudContext hudCtx = new HudContext(mc, vanillaCtx, theme, delta);

        // 1. Piirrä läpikuultava tausta
        uiCtx.fill(0, 0, width, height, BACKGROUND_COLOR.getPacked());

        // 2. Piirrä grid (vain jos raahataan)
        if (gridVisible) {
            drawGrid(uiCtx);
        }

        // 3. Piirrä HUD-elementit (renderEdit) – käytetään HudContextia
        for (HudElement e : HudManager.get().elements()) {
            if (!e.enabled()) continue;
            e.renderEdit(hudCtx);
        }

        // 4. Piirrä outlinet (käytetään suoraan Renderer2D.COLORia, mutta voidaan myös uiCtx:n kautta)
        for (HudElement e : HudManager.get().elements()) {
            if (!e.enabled()) continue;
            drawOutline(uiCtx, e, mouseX, mouseY);
        }

        // 5. Piirrä kaikki COLOR-piirrot
        Renderer2D.COLOR.render();

        // 6. Piirrä tekstit (käyttäen TextRenderer-järjestelmää)
        TextRenderer.get().begin(1.0, false, false);

        // 7. Piirrä elementtien nimet
        for (HudElement e : HudManager.get().elements()) {
            if (!e.enabled()) continue;
            drawElementName(uiCtx, e);
        }

        // 8. Lopeta tekstien piirto
        TextRenderer.get().end();

        super.render(vanillaCtx, mouseX, mouseY, delta);
    }

    private void drawGrid(UiContext ctx) {
        for (int x = 0; x < width; x += GRID_SIZE) {
            Renderer2D.COLOR.line(x, 0, x, height, GRID_COLOR);
        }
        for (int y = 0; y < height; y += GRID_SIZE) {
            Renderer2D.COLOR.line(0, y, width, y, GRID_COLOR);
        }
    }

    private void drawOutline(UiContext ctx, HudElement e, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        int x = e.x();
        int y = e.y();
        int w = Math.max(1, e.width(mc));
        int h = Math.max(1, e.height(mc));

        Color color;
        if (e == dragging) {
            color = OUTLINE_DRAGGING;
        } else if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
            color = OUTLINE_HOVER;
        } else {
            color = OUTLINE_NORMAL;
        }

        Renderer2D.COLOR.boxLines(x, y, w, h, color);
    }

    private void drawElementName(UiContext ctx, HudElement e) {
        Minecraft mc = Minecraft.getInstance();
        int x = e.x();
        int y = e.y();
        int w = Math.max(1, e.width(mc));
        // Piirrä nimi elementin yläpuolelle
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

        // Pysy ruudun sisällä
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