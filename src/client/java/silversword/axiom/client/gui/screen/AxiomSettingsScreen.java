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

public class AxiomSettingsScreen extends Screen {

    private enum Tab { GENERAL, APPEARANCE, THEME, FONT, HUD }

    private final Runnable onClose;
    private Theme theme;
    private UiContext lastUi;
    private Tab activeTab = Tab.GENERAL;

    private ScrollContainer scroll;

    public AxiomSettingsScreen(Runnable onClose) {
        super(Component.literal("Axiom Settings"));
        this.onClose = onClose;
    }

    @Override
    protected void init() {
        super.init();
        theme = ThemeManager.getCurrentTheme();
        scroll = new ScrollContainer();
        scroll.setDrawBackground(true);
        scroll.setInnerPadding(12);
        scroll.setGap(10);
        rebuild();
    }

    private void rebuild() {
        scroll.clear();
        switch (activeTab) {
            case GENERAL   -> buildGeneral();
            case APPEARANCE-> buildAppearance();
            case THEME     -> buildTheme();
            case FONT      -> buildFont();
            case HUD       -> buildHud();
        }
    }

    // ── Tabit ──────────────────────────────────────────────────────
    private void buildGeneral() {
        scroll.add(new SectionLabel("Interface"));

        scroll.add(new Button("Reset Windows", () -> {
            ClickGuiScreen gui = new ClickGuiScreen();
            gui.init(this.width, this.height);
            gui.resetWindows();
            if (minecraft != null) minecraft.setScreenAndShow(new ClickGuiScreen());
        }));

        scroll.add(new Toggle("Rainbow wave for modules",
                ClickGuiConfigManager::isRainbowWaveEnabled,
                val -> {
                    ClickGuiConfigManager.setRainbowWaveEnabled(val);
                    rebuild();
                }));

        if (ClickGuiConfigManager.isRainbowWaveEnabled()) {
            scroll.add(new Slider("Wave speed", 0.1, 5.0, 0.05,
                    () -> (double) ClickGuiConfigManager.getRainbowWaveSpeed(),
                    val -> ClickGuiConfigManager.setRainbowWaveSpeed((float) val),
                    val -> String.format("%.2f", val)));

            scroll.add(new Button("Choose Rainbow Palette", () -> {
                if (minecraft != null) {
                    minecraft.setScreenAndShow(new PaletteSelectorScreen(() ->
                            minecraft.setScreenAndShow(new AxiomSettingsScreen(onClose))));
                }
            }));
        }
    }

    private void buildAppearance() {
        scroll.add(new SectionLabel("Global Opacity"));

        scroll.add(new Slider("Opacity", 0, 100, 1,
                () -> (double) ClickGuiConfigManager.getGlobalAlpha(),
                val -> ClickGuiConfigManager.setGlobalAlpha((int) Math.round(val)),
                val -> (int) Math.round(val) + "%"));

        scroll.add(new SectionLabel("Info"));
        scroll.add(new DummyLabel("Opacity affects every theme uniformly."));
        scroll.add(new DummyLabel("For fine-grained colors, use Theme Editor."));
    }

    private void buildTheme() {
        scroll.add(new SectionLabel("Active Theme"));
        scroll.add(new DummyLabel("Current: " + ClickGuiConfigManager.getThemeName()));

        scroll.add(new Button("Open Theme Picker", () -> {
            if (minecraft != null) {
                minecraft.setScreenAndShow(new ThemePickerScreen(() ->
                        minecraft.setScreenAndShow(new AxiomSettingsScreen(onClose))));
            }
        }));

        scroll.add(new SectionLabel("Custom Themes"));

        scroll.add(new Button("+ Create Custom Theme", () -> {
            CustomTheme base = CustomTheme.from("Custom", ThemeManager.getCurrentTheme());
            if (minecraft != null) {
                minecraft.setScreenAndShow(new ThemeEditorScreen(base, true, () ->
                        minecraft.setScreenAndShow(new AxiomSettingsScreen(onClose))));
            }
        }));

        boolean any = false;
        for (CustomTheme c : CustomThemeStore.all()) {
            any = true;
            scroll.add(new Button("Edit: " + c.name, () -> {
                CustomTheme copy = CustomTheme.from(c.name, c.toTheme());
                if (minecraft != null) {
                    minecraft.setScreenAndShow(new ThemeEditorScreen(copy, false, () ->
                            minecraft.setScreenAndShow(new AxiomSettingsScreen(onClose))));
                }
            }));
        }
        if (!any) scroll.add(new DummyLabel("No custom themes yet."));

        scroll.add(new SectionLabel("Rainbow Palettes"));
        scroll.add(new Button("Choose Rainbow Palette", () -> {
            if (minecraft != null) {
                minecraft.setScreenAndShow(new PaletteSelectorScreen(() ->
                        minecraft.setScreenAndShow(new AxiomSettingsScreen(onClose))));
            }
        }));
    }

    private void buildFont() {
        scroll.add(new SectionLabel("Font"));
        scroll.add(new Button("Open Font Settings", () -> {
            if (minecraft != null) {
                minecraft.setScreenAndShow(new FontSettingsScreen(() ->
                        minecraft.setScreenAndShow(new AxiomSettingsScreen(onClose))));
            }
        }));
    }

    private void buildHud() {
        scroll.add(new SectionLabel("HUD"));
        scroll.add(new Button("HUD Components", () -> {
            scroll.clear();
            scroll.add(new SectionLabel("HUD Components"));
            scroll.add(new HudComponentsList());
            scroll.add(new Button("← Back", this::rebuild));
        }));
        scroll.add(new Button("Edit HUD Positions", () -> {
            if (minecraft != null) minecraft.setScreenAndShow(new HudEditScreen());
        }));
    }

    // ── Layout ─────────────────────────────────────────────────────
    @Override public void extractBackground(GuiGraphicsExtractor c, int mx, int my, float d) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);

        theme = ThemeManager.getCurrentTheme();

        Matrix4f proj = new Matrix4f().setOrtho(0, width, height, 0, -1000, 1000);
        Renderer2D renderer = new Renderer2D(ctx, RenderAPI.getInstance().getCore(), proj);
        lastUi = new UiContext(minecraft, ctx, theme, delta, renderer);

        lastUi.fill(0, 0, width, height, 0xE0000000);

        // Ylätabit
        drawTabs(lastUi, mouseX, mouseY);

        // Sisältö
        int margin = 20;
        int topY = 68;
        int bottomY = height - 16;
        int cW = Math.min(width - margin * 2, 560);
        int cX = (width - cW) / 2;
        scroll.setBounds(new Rect(cX, topY, cW, bottomY - topY));
        scroll.render(lastUi, mouseX, mouseY, delta);

        lastUi.renderTexts();
    }

    private void drawTabs(UiContext ui, int mouseX, int mouseY) {
        Tab[] tabs = Tab.values();
        String[] labels = { "General", "Appearance", "Theme", "Font", "HUD" };

        int fontH = ui.fontHeight();
        int padding = Math.max(8, fontH / 2 + 4);
        int gap = 4;
        int btnH = Math.max(22, fontH + 10);
        int y = 16;

        // Mittaa kokonaisleveys
        int totalW = 0;
        int[] widths = new int[tabs.length];
        for (int i = 0; i < tabs.length; i++) {
            widths[i] = ui.textWidth(labels[i]) + padding * 2;
            totalW += widths[i];
        }
        totalW += gap * (tabs.length - 1);

        int x = (width - totalW) / 2;
        int textY = y + btnH / 2 - fontH / 2 + 4;

        // Piirrä taustapalkki
        int barPad = 6;
        Rect barR = new Rect(x - barPad, y - barPad, totalW + barPad * 2, btnH + barPad * 2);
        ui.fillRounded(barR, ui.theme.panel, ui.theme.radius + 2);
        ui.drawRoundedOutline(barR, ui.theme.border, ui.theme.radius + 2, 1.0);

        for (int i = 0; i < tabs.length; i++) {
            Rect r = new Rect(x, y, widths[i], btnH);
            boolean active = activeTab == tabs[i];
            boolean hover = r.contains(mouseX, mouseY);
            int bg = active ? ui.theme.accent : (hover ? ui.theme.buttonHover : ui.theme.button);
            ui.fillRounded(r, bg, ui.theme.radius);
            if (!active && hover) {
                ui.drawRoundedOutline(r, ui.theme.accent, ui.theme.radius, 1.0);
            }
            int tw = ui.textWidth(labels[i]);
            ui.text(labels[i], r.x + (widths[i] - tw) / 2, textY, ui.theme.text);
            x += widths[i] + gap;
        }

        // Takaisin-nappi oikealle
        int backW = 70, backH = btnH;
        int backX = width - backW - 16;
        Rect backR = new Rect(backX, y, backW, backH);
        boolean bHover = backR.contains(mouseX, mouseY);
        ui.fillRounded(backR, bHover ? ui.theme.buttonHover : ui.theme.button, ui.theme.radius);
        ui.drawRoundedOutline(backR, ui.theme.border, ui.theme.radius, 1.0);
        int bW = ui.textWidth("Back");
        ui.text("Back", backX + (backW - bW) / 2, textY, ui.theme.text);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int mx = (int) click.x(), my = (int) click.y();
        if (click.button() != 1) return super.mouseClicked(click, doubled);

        if (lastUi == null) return super.mouseClicked(click, doubled);

        // Takaisin
        int backW = 70, backH = Math.max(22, lastUi.fontHeight() + 10);
        int backX = width - backW - 16;
        if (new Rect(backX, 16, backW, backH).contains(mx, my)) {
            close();
            return true;
        }

        // Tabit
        Tab[] tabs = Tab.values();
        String[] labels = { "General", "Appearance", "Theme", "Font", "HUD" };
        int fontH = lastUi.fontHeight();
        int padding = Math.max(8, fontH / 2 + 4);
        int gap = 4;
        int btnH = Math.max(22, fontH + 10);
        int y = 16;

        int totalW = 0;
        int[] widths = new int[tabs.length];
        for (int i = 0; i < tabs.length; i++) {
            widths[i] = lastUi.textWidth(labels[i]) + padding * 2;
            totalW += widths[i];
        }
        totalW += gap * (tabs.length - 1);

        int x = (width - totalW) / 2;
        for (int i = 0; i < tabs.length; i++) {
            Rect r = new Rect(x, y, widths[i], btnH);
            if (r.contains(mx, my)) {
                if (activeTab != tabs[i]) {
                    activeTab = tabs[i];
                    rebuild();
                }
                return true;
            }
            x += widths[i] + gap;
        }

        if (scroll.mouseClicked(lastUi, mx, my, click.button())) return true;
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

    @Override public boolean keyPressed(KeyEvent input) {
        if (input.isEscape()) { close(); return true; }
        if (lastUi != null && scroll.keyPressed(lastUi, input.key(), input.keycode(), input.modifiers()))
            return true;
        return super.keyPressed(input);
    }

    @Override public boolean charTyped(CharacterEvent input) {
        if (lastUi != null && input.isAllowedChatCharacter()) {
            String s = input.codepointAsString();
            for (char c : s.toCharArray()) {
                if (scroll.charTyped(lastUi, c, 0)) return true;
            }
        }
        return super.charTyped(input);
    }

    @Override public void onClose() { close(); }
    private void close() { if (onClose != null) onClose.run(); else super.onClose(); }

    @Override public boolean isPauseScreen() { return false; }

    // ── Apuluokka: osion otsikko ───────────────────────────────────
    private final class SectionLabel implements UiComponent {
        private final String text;
        private Rect bounds;
        SectionLabel(String text) { this.text = text; }
        @Override public Rect getBounds() { return bounds; }
        @Override public void setBounds(Rect r) { bounds = r; }
        @Override public int getPreferredHeight() { return 22; }

        @Override
        public void render(UiContext ui, int mouseX, int mouseY, float delta) {
            if (bounds == null) return;
            int textY = bounds.y + bounds.h / 2 - ui.fontHeight() / 2 + 4;
            ui.text(text, bounds.x + 2, textY, ui.theme.accent);
            int lineY = bounds.y + bounds.h - 4;
            ui.fill(bounds.x, lineY, bounds.w, 1, ui.theme.border);
        }
        @Override public boolean mouseClicked(UiContext ui, double mx, double my, int b) { return false; }
        @Override public void mouseReleased(UiContext ui, double mx, double my, int b) {}
        @Override public boolean mouseDragged(UiContext ui, double mx, double my, int b, double dx, double dy) { return false; }
        @Override public boolean mouseScrolled(UiContext ui, double mx, double my, double a) { return false; }
        @Override public boolean keyPressed(UiContext ui, int k, int s, int m) { return false; }
        @Override public boolean charTyped(UiContext ui, char c, int m) { return false; }
    }
}