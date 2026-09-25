package silversword.axiom.client.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import silversword.axiom.client.config.ClickGuiConfigManager;
import silversword.axiom.client.gui.components.*;
import silversword.axiom.client.gui.core.*;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.render.rendersystem.axiomrenderer.RenderAPI;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer2D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Täysi teemaeditori. Kaikki Theme-kentät muokattavissa.
 *  Väri-picker aukeaa editorin VIEREEN, ei päälle. */
public final class ThemeEditorScreen extends Screen {

    private final Runnable onClose;
    private final CustomTheme editing;
    private final boolean isNew;

    private Theme theme;
    private ScrollContainer scroll;
    private ScrollContainer previewScroll;
    private UiContext lastUi;

    private final List<ColorRow> colorRows = new ArrayList<>();
    private final List<LayoutRow> layoutRows = new ArrayList<>();

    private ActionButton saveBtn;
    private ActionButton cancelBtn;
    private ActionButton resetBtn;

    // Inline-väri-picker
    private HsvColorPicker activePicker = null;
    private Rect activePickerRect = null;
    private String activePickerLabel = "";

    // Layout
    private int leftColX, leftColW, rightColX, rightColW, contentY, contentH;

    public ThemeEditorScreen(CustomTheme editing, boolean isNew, Runnable onClose) {
        super(Component.literal("Theme Editor"));
        this.editing = editing;
        this.isNew = isNew;
        this.onClose = onClose;
    }

    @Override
    protected void init() {
        super.init();
        theme = editing.toTheme();

        // ── Väri-rivit ────────────────────────────────────────────
        colorRows.clear();
        colorRows.add(new ColorRow("Panel",          () -> editing.panel,          v -> editing.panel = v));
        colorRows.add(new ColorRow("Header",         () -> editing.header,         v -> editing.header = v));
        colorRows.add(new ColorRow("Border",         () -> editing.border,         v -> editing.border = v));
        colorRows.add(new ColorRow("Knob",           () -> editing.knob,           v -> editing.knob = v));
        colorRows.add(new ColorRow("Text",           () -> editing.text,           v -> editing.text = v));
        colorRows.add(new ColorRow("Text Dim",       () -> editing.textDim,        v -> editing.textDim = v));
        colorRows.add(new ColorRow("Accent",         () -> editing.accent,         v -> editing.accent = v));
        colorRows.add(new ColorRow("Button",         () -> editing.button,         v -> editing.button = v));
        colorRows.add(new ColorRow("Button Hover",   () -> editing.buttonHover,    v -> editing.buttonHover = v));
        colorRows.add(new ColorRow("Toggle Off",     () -> editing.toggleOff,      v -> editing.toggleOff = v));
        colorRows.add(new ColorRow("Toggle On",      () -> editing.toggleOn,       v -> editing.toggleOn = v));
        colorRows.add(new ColorRow("Slider Track",   () -> editing.sliderTrack,    v -> editing.sliderTrack = v));
        colorRows.add(new ColorRow("Slider Fill",    () -> editing.sliderFill,     v -> editing.sliderFill = v));
        colorRows.add(new ColorRow("Scrollbar",      () -> editing.scrollbar,      v -> editing.scrollbar = v));
        colorRows.add(new ColorRow("Scrollbar Hover",() -> editing.scrollbarHover, v -> editing.scrollbarHover = v));

        // ── Layout-rivit ──────────────────────────────────────────
        layoutRows.clear();
        layoutRows.add(new LayoutRow("Radius",         0, 24,
                () -> editing.radius,       v -> editing.radius = v));
        layoutRows.add(new LayoutRow("Padding",        0, 20,
                () -> editing.padding,      v -> editing.padding = v));
        layoutRows.add(new LayoutRow("Inner Padding",  0, 12,
                () -> editing.innerPadding, v -> editing.innerPadding = v));
        layoutRows.add(new LayoutRow("Header Height", 14, 40,
                () -> editing.headerHeight, v -> editing.headerHeight = v));
        layoutRows.add(new LayoutRow("Row Height",    12, 40,
                () -> editing.rowHeight,    v -> editing.rowHeight = v));

        // ── Scroll container ──────────────────────────────────────
        scroll = new ScrollContainer();
        scroll.setDrawBackground(true);
        scroll.setInnerPadding(8);
        scroll.setGap(4);
        for (ColorRow r : colorRows) scroll.add(r);
        for (LayoutRow r : layoutRows) scroll.add(r);

        previewScroll = new ScrollContainer();
        previewScroll.setDrawBackground(true);
        previewScroll.setInnerPadding(8);
        previewScroll.setGap(6);
        previewScroll.add(new PreviewBlock());

        // ── Bottom-napit ──────────────────────────────────────────
        saveBtn   = new ActionButton(isNew ? "Create Theme" : "Save Changes", this::saveAndClose);
        cancelBtn = new ActionButton("Cancel", this::close);
        resetBtn  = new ActionButton("Reset to Default", this::resetToDefault);
    }

    // ═══════════════════════════════════════════════════════════════
    //  TALLENNUS / RESET
    // ═══════════════════════════════════════════════════════════════

    private void saveAndClose() {
        String desired = editing.name == null || editing.name.trim().isEmpty()
                ? "Custom " + System.currentTimeMillis()
                : editing.name.trim();

        if (ThemeManager.isBuiltIn(desired)) {
            int i = 1;
            while (ThemeManager.isBuiltIn(desired + " " + i) ||
                    CustomThemeStore.get(desired + " " + i) != null) i++;
            desired = desired + " " + i;
        } else if (isNew && CustomThemeStore.get(desired) != null) {
            int i = 1;
            while (CustomThemeStore.get(desired + " " + i) != null) i++;
            desired = desired + " " + i;
        }

        editing.name = desired;
        CustomThemeStore.put(editing);
        ClickGuiConfigManager.setThemeName(desired);
        close();
    }

    private void resetToDefault() {
        CustomTheme def = CustomTheme.from(editing.name, new Theme());
        editing.panel          = def.panel;
        editing.header         = def.header;
        editing.border         = def.border;
        editing.knob           = def.knob;
        editing.text           = def.text;
        editing.textDim        = def.textDim;
        editing.accent         = def.accent;
        editing.button         = def.button;
        editing.buttonHover    = def.buttonHover;
        editing.toggleOff      = def.toggleOff;
        editing.toggleOn       = def.toggleOn;
        editing.sliderTrack    = def.sliderTrack;
        editing.sliderFill     = def.sliderFill;
        editing.scrollbar      = def.scrollbar;
        editing.scrollbarHover = def.scrollbarHover;
        editing.radius         = def.radius;
        editing.padding        = def.padding;
        editing.innerPadding   = def.innerPadding;
        editing.headerHeight   = def.headerHeight;
        editing.rowHeight      = def.rowHeight;
    }

    // ═══════════════════════════════════════════════════════════════
    //  RENDER
    // ═══════════════════════════════════════════════════════════════

    @Override
    public void extractBackground(GuiGraphicsExtractor ctx, int mx, int my, float d) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);

        Matrix4f proj = new Matrix4f().setOrtho(0, width, height, 0, -1000, 1000);
        Renderer2D renderer = new Renderer2D(ctx, RenderAPI.getInstance().getCore(), proj);
        Theme liveTheme = editing.toTheme();
        lastUi = new UiContext(minecraft, ctx, liveTheme, delta, renderer);

        // Tausta
        lastUi.fill(0, 0, width, height, 0xE0000000);

        // Otsikko
        String title = isNew ? "Create Custom Theme" : "Edit Theme: " + editing.name;
        lastUi.text(title, 16, 12, liveTheme.text);

        // Close-nappi
        int btnW = 70, btnH = 22, btnX = width - btnW - 16, btnY = 12;
        Rect btnRect = new Rect(btnX, btnY, btnW, btnH);
        boolean backHover = btnRect.contains(mouseX, mouseY);
        lastUi.fillRounded(btnRect, backHover ? liveTheme.buttonHover : liveTheme.button, liveTheme.radius);
        lastUi.drawRoundedOutline(btnRect, liveTheme.border, liveTheme.radius, 1.0);
        int closeW = lastUi.textWidth("Close");
        lastUi.text("Close", btnX + (btnW - closeW) / 2, btnY + 6, liveTheme.text);

        // ── Layout: editori vasemmalle, picker oikealle ─────────────
        boolean pickerOpen = activePicker != null;
        int margin = 16;
        int topY = 48;
        int bottomY = height - 56;
        int gap = 12;
        contentY = topY;
        contentH = bottomY - topY;

        if (pickerOpen) {
            int totalW = width - margin * 2;
            int pickerW = Math.min(620, Math.max(480, (int)(totalW * 0.55f)));
            // Varmista että editorille jää vähintään 320px
            if (pickerW > totalW - 320) pickerW = Math.max(360, totalW - 320);
            int editorW = totalW - pickerW - gap;

            leftColX = margin;
            leftColW = editorW;
            rightColX = leftColX + leftColW + gap;
            rightColW = pickerW;

            // Aseta pickerin bounds — viereen, ei päälle
            activePickerRect = new Rect(rightColX, topY, pickerW, contentH);
            activePicker.setBounds(new Rect(
                    rightColX + 6,
                    topY + 32,
                    pickerW - 12,
                    contentH - 38
            ));
        } else {
            boolean wide = width >= 720;
            if (wide) {
                int available = width - margin * 2 - gap;
                leftColX = margin;
                leftColW = Math.max(320, (int)(available * 0.55f));
                rightColX = leftColX + leftColW + gap;
                rightColW = width - margin - rightColX;
            } else {
                leftColX = margin;
                leftColW = width - margin * 2;
                rightColX = leftColX;
                rightColW = 0;
            }
        }

        // Editori
        scroll.setBounds(new Rect(leftColX, contentY, leftColW, contentH));
        scroll.render(lastUi, mouseX, mouseY, delta);

        // Preview (vain kun picker ei ole auki)
        if (!pickerOpen && rightColW > 0) {
            previewScroll.setBounds(new Rect(rightColX, contentY, rightColW, contentH));
            previewScroll.render(lastUi, mouseX, mouseY, delta);
        }

        // Bottom-bar
        int barY = bottomY + 8;
        int bw = 140, bh = 28, bgap = 8;
        int totalBtnW = bw * 3 + bgap * 2;
        int startX = (width - totalBtnW) / 2;
        resetBtn.setBounds(new Rect(startX, barY, bw, bh));
        cancelBtn.setBounds(new Rect(startX + bw + bgap, barY, bw, bh));
        saveBtn.setBounds(new Rect(startX + (bw + bgap) * 2, barY, bw, bh));
        resetBtn.render(lastUi, mouseX, mouseY, delta);
        cancelBtn.render(lastUi, mouseX, mouseY, delta);
        saveBtn.render(lastUi, mouseX, mouseY, delta);

        // ── INLINE VÄRI-PICKER (oikealla puolella) ─────────────────
        if (activePicker != null && activePickerRect != null) {
            lastUi.fillRounded(activePickerRect, liveTheme.panel, liveTheme.radius + 2);
            lastUi.drawRoundedOutline(activePickerRect, liveTheme.accent, liveTheme.radius + 2, 2.0);

            // Otsikkorivi
            String t = "Edit: " + activePickerLabel;
            int tY = activePickerRect.y + 10;
            lastUi.text(t, activePickerRect.x + 10, tY, liveTheme.text);

            // X-sulje-nappi
            int cb = 18;
            Rect closeR = new Rect(activePickerRect.right() - cb - 6, activePickerRect.y + 6, cb, cb);
            boolean closeHover = closeR.contains(mouseX, mouseY);
            lastUi.fillRounded(closeR, closeHover ? 0xFF8B2020 : liveTheme.button, 3);
            int xw = lastUi.textWidth("x");
            lastUi.text("x", closeR.x + (cb - xw) / 2,
                    closeR.y + (cb - lastUi.fontHeight()) / 2 + 4, liveTheme.text);

            // Itse picker
            activePicker.render(lastUi, mouseX, mouseY, delta);
        }

        lastUi.renderTexts();
    }

    // ═══════════════════════════════════════════════════════════════
    //  INPUT
    // ═══════════════════════════════════════════════════════════════

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int mx = (int) click.x(), my = (int) click.y();
        if (lastUi == null) return super.mouseClicked(click, doubled);

        // 1) Picker nappaa klikit VAIN omalta alueeltaan
        if (activePicker != null && activePickerRect != null) {
            int cb = 18;
            Rect closeR = new Rect(activePickerRect.right() - cb - 6, activePickerRect.y + 6, cb, cb);
            if (closeR.contains(mx, my)) { activePicker = null; return true; }

            if (activePickerRect.contains(mx, my)) {
                if (activePicker.mouseClicked(lastUi, mx, my, click.button())) return true;
                return true;
            }
            // Klikkaus pickerin ulkopuolella → päästetään editorille läpi
        }

        // 2) Close-nappi oikealla ylhäällä
        int btnW = 70, btnX = width - btnW - 16;
        if (click.button() == 1 && new Rect(btnX, 12, btnW, 22).contains(mx, my)) {
            close();
            return true;
        }

        // 3) Bottom-napit
        if (saveBtn.mouseClicked(lastUi, mx, my, click.button())) return true;
        if (cancelBtn.mouseClicked(lastUi, mx, my, click.button())) return true;
        if (resetBtn.mouseClicked(lastUi, mx, my, click.button())) return true;

        // 4) Editorin scroll
        if (scroll.mouseClicked(lastUi, mx, my, click.button())) return true;

        // 5) Preview (vain kun picker ei ole auki)
        if (activePicker == null && previewScroll != null && previewScroll.getBounds() != null
                && previewScroll.getBounds().contains(mx, my)) {
            if (previewScroll.mouseClicked(lastUi, mx, my, click.button())) return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (lastUi == null) return super.mouseReleased(click);
        if (activePicker != null && activePickerRect != null
                && activePickerRect.contains(click.x(), click.y())) {
            activePicker.mouseReleased(lastUi, click.x(), click.y(), click.button());
            return true;
        }
        scroll.mouseReleased(lastUi, click.x(), click.y(), click.button());
        if (previewScroll != null) previewScroll.mouseReleased(lastUi, click.x(), click.y(), click.button());
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double ox, double oy) {
        if (lastUi == null) return super.mouseDragged(click, ox, oy);
        if (activePicker != null && activePickerRect != null
                && activePickerRect.contains(click.x(), click.y())) {
            if (activePicker.mouseDragged(lastUi, click.x(), click.y(), click.button(), ox, oy)) return true;
            return true;
        }
        if (scroll.mouseDragged(lastUi, click.x(), click.y(), click.button(), ox, oy)) return true;
        if (previewScroll != null
                && previewScroll.mouseDragged(lastUi, click.x(), click.y(), click.button(), ox, oy)) return true;
        return super.mouseDragged(click, ox, oy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (lastUi == null) return super.mouseScrolled(mx, my, h, v);
        if (activePicker != null && activePickerRect != null && activePickerRect.contains(mx, my)) {
            return true; // estä scrollin läpimeno pickerin alta
        }
        if (scroll.mouseScrolled(lastUi, mx, my, v)) return true;
        if (previewScroll != null && previewScroll.mouseScrolled(lastUi, mx, my, v)) return true;
        return super.mouseScrolled(mx, my, h, v);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        // ESC: picker ensin, sitten editori
        if (input.isEscape()) {
            if (activePicker != null) { activePicker = null; return true; }
            close();
            return true;
        }
        // Välitä näppäimet pickerille jos se on auki
        if (activePicker != null && lastUi != null) {
            if (activePicker.keyPressed(lastUi, input.key(), input.keycode(), input.modifiers()))
                return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (activePicker != null && lastUi != null && input.isAllowedChatCharacter()) {
            String s = input.codepointAsString();
            for (char c : s.toCharArray()) {
                if (activePicker.charTyped(lastUi, c, 0)) return true;
            }
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public void onClose() { close(); }

    private void close() { if (onClose != null) onClose.run(); }

    @Override public boolean isPauseScreen() { return false; }

    // ═══════════════════════════════════════════════════════════════
    //  APUMETODIT
    // ═══════════════════════════════════════════════════════════════

    /** Luo SettingColor suoraan oikealla konstruktorilla (String, Color). */
    private static SettingColor makeSettingColor(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8)  & 0xFF;
        int b =  argb         & 0xFF;
        Color color = new Color(r, g, b, a);
        return new SettingColor("theme_edit", color);
    }

    // ═══════════════════════════════════════════════════════════════
    //  COLOR ROW
    // ═══════════════════════════════════════════════════════════════

    private final class ColorRow implements UiComponent {
        private final String label;
        private final IntSupplier getter;
        private final IntConsumer setter;
        private Rect bounds;
        private Rect swatchRect;

        ColorRow(String label, IntSupplier getter, IntConsumer setter) {
            this.label = label;
            this.getter = getter;
            this.setter = setter;
        }

        @Override public Rect getBounds() { return bounds; }
        @Override public void setBounds(Rect r) { bounds = r; }
        @Override public int getPreferredHeight() { return 30; }

        @Override
        public void render(UiContext ui, int mouseX, int mouseY, float delta) {
            if (bounds == null) return;
            boolean hover = bounds.contains(mouseX, mouseY);
            int bg = hover ? ui.theme.buttonHover : ui.theme.button;
            ui.fillRounded(bounds, bg, ui.theme.radius);

            int textY = bounds.y + bounds.h / 2 - ui.fontHeight() / 2 + 4;
            ui.text(label, bounds.x + 10, textY, ui.theme.text);

            int swatchSize = Math.max(18, bounds.h - 8);
            int swatchX = bounds.right() - swatchSize - 8;
            int swatchY = bounds.y + (bounds.h - swatchSize) / 2;
            swatchRect = new Rect(swatchX, swatchY, swatchSize, swatchSize);

            ui.fillRounded(swatchRect, getter.getAsInt(), 4);
            ui.drawRoundedOutline(swatchRect, ui.theme.border, 4, 1.0);

            String hex = String.format("#%06X", getter.getAsInt() & 0xFFFFFF);
            int hexW = ui.textWidth(hex);
            ui.text(hex, swatchRect.x - hexW - 8, textY, ui.theme.textDim);
        }

        @Override
        public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
            if (button != 1 || bounds == null || !bounds.contains(mouseX, mouseY)) return false;

            int argb = getter.getAsInt();
            SettingColor sc = makeSettingColor(argb);
            if (sc == null) return false;

            activePicker = new HsvColorPicker(sc, () ->
                    setter.accept(sc.getCurrentColor().getARGB()));
            activePickerLabel = label;
            // activePickerRect ja setBounds tehdään renderissä
            return true;
        }

        @Override public void mouseReleased(UiContext ui, double mx, double my, int b) {}
        @Override public boolean mouseDragged(UiContext ui, double mx, double my, int b, double dx, double dy) { return false; }
        @Override public boolean mouseScrolled(UiContext ui, double mx, double my, double a) { return false; }
        @Override public boolean keyPressed(UiContext ui, int k, int s, int m) { return false; }
        @Override public boolean charTyped(UiContext ui, char c, int m) { return false; }
    }

    // ═══════════════════════════════════════════════════════════════
    //  LAYOUT ROW
    // ═══════════════════════════════════════════════════════════════

    private final class LayoutRow implements UiComponent {
        private final String label;
        private final int min, max;
        private final IntSupplier getter;
        private final IntConsumer setter;
        private Rect bounds;

        LayoutRow(String label, int min, int max, IntSupplier getter, IntConsumer setter) {
            this.label = label;
            this.min = min;
            this.max = max;
            this.getter = getter;
            this.setter = setter;
        }

        @Override public Rect getBounds() { return bounds; }
        @Override public void setBounds(Rect r) { bounds = r; }
        @Override public int getPreferredHeight() { return 26; }

        @Override
        public void render(UiContext ui, int mouseX, int mouseY, float delta) {
            if (bounds == null) return;
            boolean hover = bounds.contains(mouseX, mouseY);
            int bg = hover ? ui.theme.buttonHover : ui.theme.button;
            ui.fillRounded(bounds, bg, ui.theme.radius);

            int textY = bounds.y + bounds.h / 2 - ui.fontHeight() / 2 + 4;
            ui.text(label, bounds.x + 10, textY, ui.theme.text);

            String valStr = String.valueOf(getter.getAsInt());
            int valW = ui.textWidth(valStr);
            ui.text(valStr, bounds.right() - valW - 10, textY, ui.theme.textDim);

            int trackX = bounds.x + 110;
            int trackW = bounds.w - 110 - valW - 30;
            if (trackW > 20) {
                int trackY = bounds.y + bounds.h / 2 - 3;
                int trackH = 6;
                ui.fillRounded(trackX, trackY, trackW, trackH, ui.theme.sliderTrack, trackH / 2.0);
                double pct = (getter.getAsInt() - min) / (double)(max - min);
                int fillW = (int)(trackW * pct);
                if (fillW > 0) ui.fillRounded(trackX, trackY, fillW, trackH, ui.theme.sliderFill, trackH / 2.0);
                int knobX = trackX + fillW;
                ui.fillCircle(knobX, trackY + trackH / 2, 6, ui.theme.accent);
            }
        }

        @Override
        public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
            if (button != 1 || bounds == null || !bounds.contains(mouseX, mouseY)) return false;
            updateValue(mouseX);
            return true;
        }

        @Override
        public boolean mouseDragged(UiContext ui, double mx, double my, int b, double dx, double dy) {
            if (b != 1) return false;
            updateValue(mx);
            return true;
        }

        private void updateValue(double mx) {
            int trackX = bounds.x + 110;
            int valW = 4 * 8;
            int trackW = bounds.w - 110 - valW - 30;
            if (trackW <= 10) return;
            double t = (mx - trackX) / trackW;
            t = Math.max(0, Math.min(1, t));
            int val = (int) Math.round(min + t * (max - min));
            setter.accept(val);
        }

        @Override public void mouseReleased(UiContext ui, double mx, double my, int b) {}
        @Override public boolean mouseScrolled(UiContext ui, double mx, double my, double a) { return false; }
        @Override public boolean keyPressed(UiContext ui, int k, int s, int m) { return false; }
        @Override public boolean charTyped(UiContext ui, char c, int m) { return false; }
    }

    // ═══════════════════════════════════════════════════════════════
    //  PREVIEW BLOCK
    // ═══════════════════════════════════════════════════════════════

    private final class PreviewBlock implements UiComponent {
        private Rect bounds;

        @Override public Rect getBounds() { return bounds; }
        @Override public void setBounds(Rect r) { bounds = r; }
        @Override public int getPreferredHeight() { return 420; }

        @Override
        public void render(UiContext ui, int mouseX, int mouseY, float delta) {
            if (bounds == null) return;

            Theme t = editing.toTheme();
            ui.fillRounded(bounds, t.panel, t.radius);
            ui.drawRoundedOutline(bounds, t.border, t.radius, 1.5);

            Rect header = new Rect(bounds.x + 2, bounds.y + 2, bounds.w - 4, Math.max(20, t.headerHeight));
            ui.fillRoundedCustom(header, t.header, t.radius, true, true, false, false);
            ui.text("Preview", header.x + 8, header.y + header.h / 2 - ui.fontHeight() / 2 + 4, t.text);

            int y = header.bottom() + 10;
            int x = bounds.x + 10;
            int w = bounds.w - 20;

            ui.fill(x, y, w, 2, t.accent);
            y += 10;

            Rect btnR = new Rect(x, y, w, 24);
            ui.fillRounded(btnR, t.button, 4);
            ui.text("Sample Button", btnR.x + 8, btnR.y + 6, t.text);
            y += btnR.h + 8;

            int toggleW = 36, toggleH = 16;
            Rect toggleR = new Rect(x, y, toggleW, toggleH);
            ui.fillRounded(toggleR, t.toggleOn, toggleH / 2.0);
            ui.fillCircle(toggleR.x + toggleW - 9, toggleR.y + toggleH / 2, 6, t.text);
            ui.text("Toggle", x + toggleW + 10, y + 2, t.text);
            y += toggleH + 8;

            Rect trackR = new Rect(x, y + 4, w, 6);
            ui.fillRounded(trackR, t.sliderTrack, 3);
            ui.fillRounded(trackR.x, trackR.y, (int)(w * 0.65), 6, t.sliderFill, 3);
            ui.fillCircle(trackR.x + (int)(w * 0.65), trackR.y + 3, 6, t.accent);
            y += 18;

            ui.text("Primary text", x, y, t.text); y += 14;
            ui.text("Dimmed text", x, y, t.textDim); y += 18;

            int sw = 22, gap = 4;
            int cols = Math.max(1, w / (sw + gap));
            int[] cols2 = { t.accent, t.button, t.buttonHover, t.sliderFill, t.toggleOn, t.scrollbar };
            for (int i = 0; i < cols2.length; i++) {
                int cx = x + (i % cols) * (sw + gap);
                int cy = y + (i / cols) * (sw + gap);
                ui.fillRounded(new Rect(cx, cy, sw, sw), cols2[i], 4);
            }
        }

        @Override public boolean mouseClicked(UiContext ui, double mx, double my, int b) { return false; }
        @Override public void mouseReleased(UiContext ui, double mx, double my, int b) {}
        @Override public boolean mouseDragged(UiContext ui, double mx, double my, int b, double dx, double dy) { return false; }
        @Override public boolean mouseScrolled(UiContext ui, double mx, double my, double a) { return false; }
        @Override public boolean keyPressed(UiContext ui, int k, int s, int m) { return false; }
        @Override public boolean charTyped(UiContext ui, char c, int m) { return false; }
    }
}