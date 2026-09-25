package silversword.axiom.client.gui.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import silversword.axiom.client.config.ClickGuiConfigManager;
import silversword.axiom.client.gui.components.*;
import silversword.axiom.client.gui.core.*;
import silversword.axiom.client.render.rendersystem.axiomrenderer.RenderAPI;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer2D;
import silversword.axiom.client.render.rendersystem.utils.color.rainbow.RainbowPalette;
import silversword.axiom.client.render.rendersystem.utils.color.rainbow.RainbowPalettes;

import java.util.ArrayList;
import java.util.List;

public class ThemePickerScreen extends Screen {

    private final Runnable onClose;
    private Theme theme;
    private UiContext lastUi;
    private ScrollContainer scroll;

    private final List<ThemeCard> cards = new ArrayList<>();
    private ActionButton createBtn;

    private String suggestedPaletteName = null;
    private Rect useSuggestedBtnRect = null;

    private Rect bannerCloseRect = null;
    private Rect bannerRect = null;

    // Layout
    private int margin, gap, contentY, contentH;

    public ThemePickerScreen(Runnable onClose) {
        super(Component.literal("Themes"));
        this.onClose = onClose;
    }

    @Override
    protected void init() {
        super.init();
        theme = ThemeManager.getCurrentTheme();

        scroll = new ScrollContainer();
        scroll.setDrawBackground(false);
        scroll.setInnerPadding(0);
        scroll.setGap(0);

        createBtn = new ActionButton("+ Create Custom Theme", this::createCustom);

        rebuildCards();
    }

    private void rebuildCards() {
        cards.clear();
        scroll.clear();

        String current = ClickGuiConfigManager.getThemeName();

        // Built-in ensin
        for (String name : ThemeManager.getThemeNames()) {
            boolean custom = CustomThemeStore.isCustom(name);
            cards.add(new ThemeCard(name, custom, current));
        }

        // Grid-tyylinen wrapper: tehdään yksi "rivi" joka sisältää gridin? Ei — käytetään per-kortti-rivi.
        // ScrollContainer asettaa jokaisen lapsen täysleveyteen, joten teemme oman GridLayout-luokan.
        scroll.add(new GridHost(cards, createBtn));
    }

    private void createCustom() {
        CustomTheme blank = CustomTheme.from("Custom", new Theme());
        // Kopioi nykyinen pohjaksi, jotta käyttäjä näkee heti muutokset
        CustomTheme base = CustomTheme.from("Custom", ThemeManager.getCurrentTheme());
        base.name = "Custom";
        if (minecraft != null) {
            minecraft.setScreenAndShow(new ThemeEditorScreen(base, true, () -> {
                minecraft.setScreenAndShow(new ThemePickerScreen(onClose));
            }));
        }
    }

    @Override public void extractBackground(GuiGraphicsExtractor c, int mx, int my, float d) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);

        theme = ThemeManager.getCurrentTheme();

        Matrix4f proj = new Matrix4f().setOrtho(0, width, height, 0, -1000, 1000);
        Renderer2D renderer = new Renderer2D(ctx, RenderAPI.getInstance().getCore(), proj);
        lastUi = new UiContext(minecraft, ctx, theme, delta, renderer);

        lastUi.fill(0, 0, width, height, 0xE0000000);

        // Otsikko
        String title = "Themes";
        int titleW = lastUi.textWidth(title);
        lastUi.text(title, (width - titleW) / 2, 14, theme.text);

        // Takaisin-nappi
        int btnW = 70, btnH = 22, btnX = width - btnW - 16, btnY = 10;
        Rect backR = new Rect(btnX, btnY, btnW, btnH);
        boolean bHover = backR.contains(mouseX, mouseY);
        lastUi.fillRounded(backR, bHover ? theme.buttonHover : theme.button, theme.radius);
        lastUi.drawRoundedOutline(backR, theme.border, theme.radius, 1.0);
        int closeW = lastUi.textWidth("Back");
        lastUi.text("Back", btnX + (btnW - closeW) / 2, btnY + 6, theme.text);

        // Sisältö
        margin = 20;
        int topY = 48;
        int bottomY = height - 16;
        int cW = Math.min(width - margin * 2, 960);
        int cX = (width - cW) / 2;
        contentY = topY;
        contentH = bottomY - topY;

        scroll.setBounds(new Rect(cX, contentY, cW, contentH));
        scroll.render(lastUi, mouseX, mouseY, delta);

        // ── Ehdotettu paletti -banneri ─────────────────────────────
        // ── Ehdotettu paletti -banneri ─────────────────────────────
        // ── Ehdotettu paletti -banneri ─────────────────────────────
        if (suggestedPaletteName != null) {
            String msg = "Theme applied!  Matching palette: " + suggestedPaletteName;
            int fontH = lastUi.fontHeight();
            int bPad = 10;
            int bUseW = 60;
            int bXW = 18;
            int bGap = 6;

            int textW = lastUi.textWidth(msg);
            int contentW = textW + bPad + bUseW + bGap + bXW + bPad;
            int bw = Math.max(260, contentW);
            int bh = Math.max(28, fontH + 14);
            int bx = (width - bw) / 2;
            int by = 40;

            Rect banner = new Rect(bx, by, bw, bh);
            lastUi.fillRounded(banner, theme.accent, theme.radius);
            lastUi.drawRoundedOutline(banner, theme.text, theme.radius, 1.0);

            // Teksti vasemmalle
            int ty = by + bh / 2 - fontH / 2 + 4;
            lastUi.text(msg, bx + bPad, ty, theme.text);

            // X-sulje oikeaan reunaan
            Rect closeR = new Rect(banner.right() - bPad - bXW, by + (bh - bXW) / 2, bXW, bXW);
            boolean ch = closeR.contains(mouseX, mouseY);
            lastUi.fillRounded(closeR, ch ? 0xFF8B2020 : theme.button, 3);
            lastUi.drawRoundedOutline(closeR, theme.border, 3, 1.0);
            int xw = lastUi.textWidth("x");
            lastUi.text("x", closeR.x + (bXW - xw) / 2, closeR.y + (bXW - fontH) / 2 + 4, theme.text);

            // Use-nappi ennen X:ää
            int bUseH = Math.max(20, fontH + 8);
            int bUseX = closeR.x - bGap - bUseW;
            int bUseY = by + (bh - bUseH) / 2;
            useSuggestedBtnRect = new Rect(bUseX, bUseY, bUseW, bUseH);

            boolean hover = useSuggestedBtnRect.contains(mouseX, mouseY);
            lastUi.fillRounded(useSuggestedBtnRect, hover ? theme.buttonHover : theme.button, 3);
            lastUi.drawRoundedOutline(useSuggestedBtnRect, theme.border, 3, 1.0);
            int tw = lastUi.textWidth("Use");
            lastUi.text("Use", bUseX + (bUseW - tw) / 2, bUseY + (bUseH - fontH) / 2 + 4, theme.text);

            this.bannerCloseRect = closeR;
            this.bannerRect = banner;
        }

        lastUi.renderTexts();

        lastUi.renderTexts();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int mx = (int) click.x(), my = (int) click.y();
        int btnW = 70, btnX = width - btnW - 16;

        // ── Ehdotus-banneri ───────────────────────────────────────
        // ── Ehdotus-banneri ───────────────────────────────────────
        if (suggestedPaletteName != null) {
            // X-nappi
            if (bannerCloseRect != null && bannerCloseRect.contains(mx, my)) {
                suggestedPaletteName = null;
                useSuggestedBtnRect = null;
                bannerCloseRect = null;
                bannerRect = null;
                return true;
            }
            // Use-nappi
            if (useSuggestedBtnRect != null && useSuggestedBtnRect.contains(mx, my)) {
                RainbowPalette suggested = RainbowPalettes.getByName(suggestedPaletteName);
                ClickGuiConfigManager.setRainbowPalette(suggested);
                ClickGuiConfigManager.setRainbowWaveEnabled(true);
                suggestedPaletteName = null;
                useSuggestedBtnRect = null;
                bannerCloseRect = null;
                bannerRect = null;
                return true;
            }
            // Klikkaus bannerin ulkopuolelle → sulje
            if (bannerRect != null && !bannerRect.contains(mx, my)) {
                suggestedPaletteName = null;
                useSuggestedBtnRect = null;
                bannerCloseRect = null;
                bannerRect = null;
                return true;
            }
            // Muut klikkaukset bannerin sisällä → ei päästetä läpi
            return true;
        }

        if (click.button() == 1 && new Rect(btnX, 10, btnW, 22).contains(mx, my)) {
            close();
            return true;
        }
        if (lastUi != null && scroll.mouseClicked(lastUi, mx, my, click.button())) return true;
        return super.mouseClicked(click, doubled);
    }

    @Override public boolean mouseReleased(MouseButtonEvent click) {
        if (lastUi != null) scroll.mouseReleased(lastUi, click.x(), click.y(), click.button());
        return super.mouseReleased(click);
    }

    @Override public boolean mouseDragged(MouseButtonEvent click, double ox, double oy) {
        if (lastUi != null && scroll.mouseDragged(lastUi, click.x(), click.y(), click.button(), ox, oy))
            return true;
        return super.mouseDragged(click, ox, oy);
    }

    @Override public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (lastUi != null && scroll.mouseScrolled(lastUi, mx, my, v)) return true;
        return super.mouseScrolled(mx, my, h, v);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.isEscape()) {
            onClose();
            return true;
        }
        if (lastUi != null && scroll != null) {
            return scroll.keyPressed(lastUi, input.key(), input.keycode(), input.modifiers());
        }
        return super.keyPressed(input);
    }

    @Override public void onClose() { close(); }
    private void close() { if (onClose != null) onClose.run(); }

    @Override public boolean isPauseScreen() { return false; }

    /** Palauttaa mustan tai valkoisen taustavärin luminanssin mukaan. */
    private static int contrastingText(int bgArgb) {
        int r = (bgArgb >> 16) & 0xFF;
        int g = (bgArgb >> 8)  & 0xFF;
        int b =  bgArgb        & 0xFF;
        // Suhteellinen luminanssi (WCAG)
        double lum = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0;
        return lum > 0.55 ? 0xFF000000 : 0xFFFFFFFF;
    }

    // ═══════════════════════════════════════════════════════════════
    //  GRID-HOST
    // ═══════════════════════════════════════════════════════════════

    /** Ottaa listan kortteja ja asettelee ne ruudukkoon. */
    private final class GridHost implements UiComponent {
        private final List<ThemeCard> items;
        private final ActionButton createBtnLocal;
        private Rect bounds;
        private final List<Rect> cardRects = new ArrayList<>();

        GridHost(List<ThemeCard> items, ActionButton createBtn) {
            this.items = items;
            this.createBtnLocal = createBtn;
        }

        @Override public Rect getBounds() { return bounds; }
        @Override public void setBounds(Rect r) { bounds = r; layout(); }

        private int cardW() {
            // Fallback-leveys kun bounds ei ole vielä asetettu (ScrollContainer.add
            // kutsuu getPreferredHeight() ennen setBounds())
            int w = (bounds != null) ? bounds.w : 600;
            int minCardW = 220;
            int cols = Math.max(1, (w + 12) / (minCardW + 12));
            return (w - (cols - 1) * 12) / cols;
        }

        private void layout() {
            cardRects.clear();
            if (bounds == null) return;
            int cw = cardW();
            int ch = 92;
            int gapX = 12, gapY = 12;
            int cols = Math.max(1, (bounds.w + gapX) / (cw + gapX));

            int x = bounds.x;
            int y = bounds.y;
            int col = 0;

            int createH = 34;
            createBtnLocal.setBounds(new Rect(x, y, bounds.w, createH));
            y += createH + gapY;

            for (ThemeCard card : items) {
                if (col == cols) { col = 0; x = bounds.x; y += ch + gapY; }
                cardRects.add(new Rect(x, y, cw, ch));
                card.setBounds(new Rect(x, y, cw, ch));
                x += cw + gapX;
                col++;
            }
        }

        @Override
        public int getPreferredHeight() {
            if (items.isEmpty()) return 200;
            int w = (bounds != null) ? bounds.w : 600;
            int cw = cardW();
            int gapX = 12;
            int cols = Math.max(1, (w + gapX) / (cw + gapX));
            int rows = (items.size() + cols - 1) / cols;
            return 34 + 12 + rows * (92 + 12) + 8;
        }


        @Override
        public void render(UiContext ui, int mouseX, int mouseY, float delta) {
            layout();
            createBtnLocal.render(ui, mouseX, mouseY, delta);
            for (ThemeCard card : items) {
                card.render(ui, mouseX, mouseY, delta);
            }
        }

        @Override
        public boolean mouseClicked(UiContext ui, double mx, double my, int b) {
            if (createBtnLocal.mouseClicked(ui, mx, my, b)) return true;
            for (ThemeCard card : items) {
                if (card.getBounds() != null && card.getBounds().contains(mx, my)) {
                    if (card.mouseClicked(ui, mx, my, b)) return true;
                }
            }
            return false;
        }
        @Override public void mouseReleased(UiContext ui, double mx, double my, int b) {
            for (ThemeCard card : items) card.mouseReleased(ui, mx, my, b);
        }
        @Override public boolean mouseDragged(UiContext ui, double mx, double my, int b, double dx, double dy) { return false; }
        @Override public boolean mouseScrolled(UiContext ui, double mx, double my, double a) { return false; }
        @Override public boolean keyPressed(UiContext ui, int k, int s, int m) { return false; }
        @Override public boolean charTyped(UiContext ui, char c, int m) { return false; }
    }

    // ═══════════════════════════════════════════════════════════════
    //  KORTTI
    // ═══════════════════════════════════════════════════════════════

    private final class ThemeCard implements UiComponent {
        private final String name;
        private final boolean custom;
        private final String currentName;
        private Rect bounds;

        // Alinapit
        private Rect applyR, editR, dupR, delR;

        ThemeCard(String name, boolean custom, String currentName) {
            this.name = name;
            this.custom = custom;
            this.currentName = currentName;
        }

        @Override public Rect getBounds() { return bounds; }
        @Override public void setBounds(Rect r) { bounds = r; }
        @Override public int getPreferredHeight() { return 92; }

        @Override
        public void render(UiContext ui, int mouseX, int mouseY, float delta) {
            if (bounds == null) return;
            Theme preview = ThemeManager.getTheme(name);
            boolean selected = name.equals(ClickGuiConfigManager.getThemeName());

            // Kortin tausta
            ui.fillRounded(bounds, ui.theme.button, ui.theme.radius);
            if (selected) {
                ui.drawRoundedOutline(bounds, ui.theme.accent, ui.theme.radius, 2.0);
            } else {
                ui.drawRoundedOutline(bounds, ui.theme.border, ui.theme.radius, 1.0);
            }

            // Väripalkki vasemmalla
            int stripeW = 6;
            Rect stripe = new Rect(bounds.x, bounds.y, stripeW, bounds.h);
            ui.fillRoundedCustom(stripe, preview.accent, ui.theme.radius,
                    true, false, false, true);

            // Nimi
            int nx = bounds.x + stripeW + 10;
            int ny = bounds.y + 8;
            ui.text(name, nx, ny, ui.theme.text);
            if (custom) {
                String tag = "custom";
                int tagW = ui.textWidth(tag) + 8;
                int tx = bounds.right() - tagW - 8;
                ui.fillRounded(new Rect(tx, ny - 2, tagW, 14), ui.theme.accent, 3);
                ui.text(tag, tx + 4, ny, ui.theme.text);
            }

            // Väripreview rivit
            int[] swatches = {
                    preview.panel, preview.header, preview.border,
                    preview.button, preview.buttonHover, preview.accent,
                    preview.toggleOn, preview.sliderFill, preview.text
            };
            int sw = 12, gap = 3;
            int totalW = swatches.length * sw + (swatches.length - 1) * gap;
            int sx = nx;
            int sy = ny + 18;
            for (int i = 0; i < swatches.length; i++) {
                ui.fillRounded(new Rect(sx + i * (sw + gap), sy, sw, sw), swatches[i], 2);
            }

            // Napit alhaalla
            int btnH = 20;
            int by = bounds.bottom() - btnH - 6;
            int bp = 4;

            // Apply
            int applyW = 60;
            applyR = new Rect(nx, by, applyW, btnH);
            boolean aHover = applyR.contains(mouseX, mouseY);
            ui.fillRounded(applyR, aHover ? ui.theme.buttonHover : ui.theme.button, 3);
            ui.drawRoundedOutline(applyR, selected ? ui.theme.accent : ui.theme.border, 3, 1.0);
            String applyLabel = selected ? "Active" : "Apply";
            int aTw = ui.textWidth(applyLabel);
            int applyBg = aHover ? ui.theme.buttonHover : ui.theme.button;
            int applyTextColor = contrastingText(applyBg);
            ui.text(applyLabel,
                    applyR.x + (applyW - aTw) / 2, applyR.y + 4,
                    applyTextColor);

            // Edit + Delete custom-teemoille
            int rx = applyR.right() + bp;
            if (custom) {
                int smallW = 50;
                editR = new Rect(rx, by, smallW, btnH);
                boolean eH = editR.contains(mouseX, mouseY);
                ui.fillRounded(editR, eH ? ui.theme.buttonHover : ui.theme.button, 3);
                ui.drawRoundedOutline(editR, ui.theme.border, 3, 1.0);
                int eW = ui.textWidth("Edit");
                ui.text("Edit", editR.x + (smallW - eW) / 2, editR.y + 4, ui.theme.text);

                delR = new Rect(rx + smallW + bp, by, smallW, btnH);
                boolean dH = delR.contains(mouseX, mouseY);
                ui.fillRounded(delR, dH ? 0xFF8B2020 : ui.theme.button, 3);
                ui.drawRoundedOutline(delR, ui.theme.border, 3, 1.0);
                int dW = ui.textWidth("Del");
                ui.text("Del", delR.x + (smallW - dW) / 2, delR.y + 4, ui.theme.text);

                dupR = new Rect(rx + (smallW + bp) * 2, by, smallW + 10, btnH);
                boolean dupH = dupR.contains(mouseX, mouseY);
                ui.fillRounded(dupR, dupH ? ui.theme.buttonHover : ui.theme.button, 3);
                ui.drawRoundedOutline(dupR, ui.theme.border, 3, 1.0);
                int dupW = ui.textWidth("Duplicate");
                ui.text("Duplicate", dupR.x + (dupR.w - dupW) / 2, dupR.y + 4, ui.theme.text);
            } else {
                editR = dupR = delR = null;
                int smallW = 80;
                Rect dup = new Rect(rx, by, smallW, btnH);
                boolean dupH = dup.contains(mouseX, mouseY);
                ui.fillRounded(dup, dupH ? ui.theme.buttonHover : ui.theme.button, 3);
                ui.drawRoundedOutline(dup, ui.theme.border, 3, 1.0);
                int dupW = ui.textWidth("Duplicate");
                ui.text("Duplicate", dup.x + (smallW - dupW) / 2, dup.y + 4, ui.theme.text);
                dupR = dup;
            }
        }

        @Override
        public boolean mouseClicked(UiContext ui, double mx, double my, int b) {
            if (b != 1 || bounds == null) return false;

            if (applyR != null && applyR.contains(mx, my)) {
                ClickGuiConfigManager.setThemeName(name);
                // Ehdotetaan palettia
                Theme applied = ThemeManager.getTheme(name);
                suggestedPaletteName = RainbowPalettes.suggestFor(applied.accent).getName();
                return true;
            }
            if (editR != null && editR.contains(mx, my)) {
                CustomTheme custom = CustomThemeStore.get(name);
                if (custom != null && minecraft != null) {
                    CustomTheme copy = CustomTheme.from(name, custom.toTheme());
                    copy.name = name;
                    minecraft.setScreenAndShow(new ThemeEditorScreen(copy, false, () ->
                            minecraft.setScreenAndShow(new ThemePickerScreen(onClose))));
                }
                return true;
            }
            if (dupR != null && dupR.contains(mx, my)) {
                Theme src = ThemeManager.getTheme(name);
                CustomTheme dup = CustomTheme.from(name + " Copy", src);
                // Uniikki nimi
                String base = dup.name;
                int i = 1;
                while (CustomThemeStore.get(dup.name) != null) dup.name = base + " " + (i++);
                CustomThemeStore.put(dup);
                if (minecraft != null) {
                    minecraft.setScreenAndShow(new ThemePickerScreen(onClose));
                }
                return true;
            }
            if (delR != null && delR.contains(mx, my)) {
                if (CustomThemeStore.isCustom(name)) {
                    CustomThemeStore.remove(name);
                    if (name.equals(ClickGuiConfigManager.getThemeName())) {
                        ClickGuiConfigManager.setThemeName("Default");
                    }
                    if (minecraft != null) {
                        minecraft.setScreenAndShow(new ThemePickerScreen(onClose));
                    }
                }
                return true;
            }
            return false;
        }

        @Override public void mouseReleased(UiContext ui, double mx, double my, int b) {}
        @Override public boolean mouseDragged(UiContext ui, double mx, double my, int b, double dx, double dy) { return false; }
        @Override public boolean mouseScrolled(UiContext ui, double mx, double my, double a) { return false; }
        @Override public boolean keyPressed(UiContext ui, int k, int s, int m) { return false; }
        @Override public boolean charTyped(UiContext ui, char c, int m) { return false; }
    }
}