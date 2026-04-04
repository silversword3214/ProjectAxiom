package silversword.axiom.client.gui.components;

import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.render.rendersystem.utils.color.RainbowColor;

public class HsvColorPicker implements UiComponent {

    private final SettingColor setting;
    private final Runnable onColorChanged;

    private Rect bounds;
    private Rect svRect;
    private Rect hueRect;
    private Rect currentColorRect;
    private Rect alphaSliderRect;
    private Rect speedSliderRect;
    private Rect rainbowToggleRect;

    private float hue;
    private float saturation;
    private float value;
    private float alpha; // 0.0 - 1.0
    private float speed; // 0.001 - 5.0
    private boolean rainbow;

    private boolean draggingSV;
    private boolean draggingHue;
    private boolean draggingAlpha;
    private boolean draggingSpeed;

    // Gradient cache
    private int[][] gradientCache;
    private boolean gradientDirty = true;

    public HsvColorPicker(SettingColor setting, Runnable onColorChanged) {
        this.setting = setting;
        this.onColorChanged = onColorChanged;

        float[] hsv = setting.toHsv();
        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.value = hsv[2];
        this.alpha = setting.getCurrentColor().a / 255f;
        this.speed = setting.speed;
        this.rainbow = setting.rainbow;
    }

    @Override
    public Rect getBounds() {
        return bounds;
    }

    @Override
    public void setBounds(Rect bounds) {
        this.bounds = bounds;

        int padding = 10;
        int hueWidth = 12;
        int bottomPanelHeight = 70; // korkeampi, jotta mahtuu kaksi riviä

        svRect = new Rect(
                bounds.x + padding,
                bounds.y + padding,
                bounds.w - padding * 2 - hueWidth - 6,
                bounds.h - padding * 2 - bottomPanelHeight - padding
        );

        hueRect = new Rect(
                svRect.right() + 6,
                svRect.y,
                hueWidth,
                svRect.h
        );

        int bottomY = bounds.y + bounds.h - bottomPanelHeight - padding;
        int row1Y = bottomY;
        int row2Y = bottomY + 35;

        // Nykyisen värin näyttö (vasen ylä)
        currentColorRect = new Rect(
                bounds.x + padding,
                row1Y,
                60,
                30
        );

        // Rainbow-toggle (oikea ylä)
        rainbowToggleRect = new Rect(
                bounds.x + bounds.w - padding - 80,
                row1Y,
                80,
                30
        );

        // Alpha-liukusäädin (vasen ala)
        int sliderWidth = (bounds.w - 2 * padding - 10) / 2;
        alphaSliderRect = new Rect(
                bounds.x + padding,
                row2Y,
                sliderWidth,
                30
        );

        // Speed-liukusäädin (oikea ala)
        speedSliderRect = new Rect(
                alphaSliderRect.right() + 10,
                row2Y,
                sliderWidth,
                30
        );

        gradientDirty = true;
    }

    @Override
    public int getPreferredHeight() {
        return 300;
    }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        renderSVGradientOptimized(ui);
        renderHueSliderOptimized(ui);
        renderCursors(ui);
        renderBottomPanel(ui, mouseX, mouseY);
    }

    private void ensureGradient() {
        if (gradientDirty || gradientCache == null ||
                gradientCache.length != svRect.w || gradientCache[0].length != svRect.h) {

            gradientCache = new int[svRect.w][svRect.h];
            for (int y = 0; y < svRect.h; y++) {
                float v = 1f - (float) y / svRect.h;
                for (int x = 0; x < svRect.w; x++) {
                    float s = (float) x / svRect.w;
                    gradientCache[x][y] = Color.fromHsv(hue, s, v).getARGB();
                }
            }
            gradientDirty = false;
        }
    }

    private void renderSVGradientOptimized(UiContext ui) {
        if (svRect.w <= 0 || svRect.h <= 0) return;
        ensureGradient();
        int step = Math.max(1, Math.min(svRect.w, svRect.h) / 40);
        for (int y = 0; y < svRect.h; y += step) {
            for (int x = 0; x < svRect.w; x += step) {
                int color = gradientCache[x][y];
                int w = Math.min(step, svRect.w - x);
                int h = Math.min(step, svRect.h - y);
                ui.fill(svRect.x + x, svRect.y + y, w, h, color);
            }
        }
    }

    private void renderHueSliderOptimized(UiContext ui) {
        if (hueRect.h <= 0) return;
        int step = Math.max(1, hueRect.h / 30);
        for (int y = 0; y < hueRect.h; y += step) {
            float h = 360f * y / hueRect.h;
            Color c = Color.fromHsv(h, 1f, 1f);
            int height = Math.min(step, hueRect.h - y);
            ui.fill(hueRect.x, hueRect.y + y, hueRect.w, height, c.getARGB());
        }
    }

    private void renderCursors(UiContext ui) {
        // SV cursor
        int cx = (int) (svRect.x + saturation * svRect.w);
        int cy = (int) (svRect.y + (1 - value) * svRect.h);
        ui.fill(cx - 2, cy - 2, 4, 4, 0xFFFFFFFF);
        ui.fill(cx - 1, cy - 1, 2, 2, 0xFF000000);

        // Hue cursor
        int hy = (int) (hueRect.y + (hue / 360f) * hueRect.h);
        ui.fill(hueRect.x - 2, hy - 2, hueRect.w + 4, 4, 0xFFFFFFFF);
        ui.fill(hueRect.x - 1, hy - 1, hueRect.w + 2, 2, 0xFF000000);
    }

    private void renderBottomPanel(UiContext ui, int mouseX, int mouseY) {
        // Nykyinen väri (vasen ylä)
        Color currentColor;
        if (rainbow) {
            currentColor = new RainbowColor().set(setting).setSpeed(speed);
        } else {
            currentColor = Color.fromHsv(hue, saturation, value);
            currentColor.a = (int)(alpha * 255);
        }
        ui.fill(currentColorRect, currentColor.getARGB());
        ui.text("Current", currentColorRect.x, currentColorRect.y - 10, ui.theme.text);

        // Rainbow-toggle (oikea ylä)
        boolean rainbowHover = rainbowToggleRect.contains(mouseX, mouseY);
        int buttonColor = rainbowHover ? ui.theme.buttonHover : ui.theme.button;
        // Outline
        ui.fill(rainbowToggleRect.x - 1, rainbowToggleRect.y - 1,
                rainbowToggleRect.w + 2, rainbowToggleRect.h + 2, 0xFF888888);
        ui.fill(rainbowToggleRect, buttonColor);

        if (rainbow) {
            for (int i = 0; i < rainbowToggleRect.w; i += 4) {
                float h = (i / (float) rainbowToggleRect.w) * 360f;
                Color c = Color.fromHsv(h, 1f, 1f);
                ui.fill(rainbowToggleRect.x + i, rainbowToggleRect.y,
                        Math.min(4, rainbowToggleRect.w - i), 3, c.getARGB());
            }
        }
        String toggleText = rainbow ? "Rainbow: ON" : "Rainbow: OFF";
        int toggleX = rainbowToggleRect.x + (rainbowToggleRect.w - ui.textWidth(toggleText)) / 2;
        int toggleY = rainbowToggleRect.y + (rainbowToggleRect.h - ui.fontHeight()) / 2;
        ui.text(toggleText, toggleX, toggleY, ui.theme.text);

        // Alpha-slider (vasen ala)
        boolean alphaHover = alphaSliderRect.contains(mouseX, mouseY);
        int alphaBg = alphaHover ? ui.theme.buttonHover : ui.theme.button;
        // Outline
        ui.fill(alphaSliderRect.x - 1, alphaSliderRect.y - 1,
                alphaSliderRect.w + 2, alphaSliderRect.h + 2, 0xFF888888);
        ui.fill(alphaSliderRect, alphaBg);

        int sliderX = (int) (alphaSliderRect.x + 2 + (alphaSliderRect.w - 4) * alpha);
        ui.fill(sliderX - 2, alphaSliderRect.y + 2, 4, alphaSliderRect.h - 4, 0xFFFFFFFF);

        String alphaText = String.format("Alpha: %d%%", (int)(alpha * 100));
        int alphaTextX = alphaSliderRect.x + (alphaSliderRect.w - ui.textWidth(alphaText)) / 2;
        int alphaTextY = alphaSliderRect.y + 2;
        ui.text(alphaText, alphaTextX, alphaTextY, ui.theme.text);

        // Speed-slider (oikea ala) – näytetään vain jos rainbow päällä
        if (rainbow) {
            boolean speedHover = speedSliderRect.contains(mouseX, mouseY);
            int speedBg = speedHover ? ui.theme.buttonHover : ui.theme.button;
            // Outline
            ui.fill(speedSliderRect.x - 1, speedSliderRect.y - 1,
                    speedSliderRect.w + 2, speedSliderRect.h + 2, 0xFF888888);
            ui.fill(speedSliderRect, speedBg);

            int speedX = (int) (speedSliderRect.x + 2 + (speedSliderRect.w - 4) * (speed / 5.0f));
            ui.fill(speedX - 2, speedSliderRect.y + 2, 4, speedSliderRect.h - 4, 0xFFFFFFFF);

            String speedText = String.format("Speed: %.2f", speed);
            int speedTextX = speedSliderRect.x + (speedSliderRect.w - ui.textWidth(speedText)) / 2;
            int speedTextY = speedSliderRect.y + 2;
            ui.text(speedText, speedTextX, speedTextY, ui.theme.text);
        } else {
            // Jos rainbow ei päällä, piirretään speed-slider harmaana (disabled)
            ui.fill(speedSliderRect.x - 1, speedSliderRect.y - 1,
                    speedSliderRect.w + 2, speedSliderRect.h + 2, 0xFF888888);
            ui.fill(speedSliderRect, 0x44000000);
            String speedText = "Speed: N/A";
            int speedTextX = speedSliderRect.x + (speedSliderRect.w - ui.textWidth(speedText)) / 2;
            int speedTextY = speedSliderRect.y + 2;
            ui.text(speedText, speedTextX, speedTextY, ui.theme.textDim);
        }
    }


    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        if (rainbowToggleRect.contains(mouseX, mouseY)) {
            rainbow = !rainbow;
            updateColor();
            return true;
        }

        if (rainbow) {
            if (speedSliderRect.contains(mouseX, mouseY)) {
                draggingSpeed = true;
                updateSpeed(mouseX);
                return true;
            }
        }

        if (svRect.contains(mouseX, mouseY)) {
            draggingSV = true;
            updateSV(mouseX, mouseY);
            return true;
        }

        if (hueRect.contains(mouseX, mouseY)) {
            draggingHue = true;
            updateHue(mouseY);
            return true;
        }

        if (alphaSliderRect.contains(mouseX, mouseY)) {
            draggingAlpha = true;
            updateAlpha(mouseX);
            return true;
        }

        return false;
    }

    @Override
    public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {
        draggingSV = false;
        draggingHue = false;
        draggingAlpha = false;
        draggingSpeed = false;
    }

    @Override
    public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (rainbow && draggingSpeed && speedSliderRect.contains(mouseX, mouseY)) {
            updateSpeed(mouseX);
            return true;
        }

        if (draggingSV && svRect.contains(mouseX, mouseY)) {
            updateSV(mouseX, mouseY);
            return true;
        }

        if (draggingHue && hueRect.contains(mouseX, mouseY)) {
            updateHue(mouseY);
            return true;
        }

        if (draggingAlpha && alphaSliderRect.contains(mouseX, mouseY)) {
            updateAlpha(mouseX);
            return true;
        }

        return false;
    }

    private void updateSpeed(double mouseX) {
        float newSpeed = (float) ((mouseX - speedSliderRect.x) / speedSliderRect.w) * 5.0f;
        newSpeed = clamp(newSpeed, 0.001f, 5.0f);
        if (Math.abs(speed - newSpeed) > 0.001f) {
            speed = newSpeed;
            updateColor();
        }
    }

    private void updateAlpha(double mouseX) {
        float newAlpha = (float) ((mouseX - alphaSliderRect.x) / alphaSliderRect.w);
        newAlpha = clamp(newAlpha, 0f, 1f);
        if (Math.abs(alpha - newAlpha) > 0.001f) {
            alpha = newAlpha;
            updateColor();
        }
    }

    private void updateSV(double mouseX, double mouseY) {
        float newSaturation = clamp((float) ((mouseX - svRect.x) / svRect.w), 0f, 1f);
        float newValue = 1f - clamp((float) ((mouseY - svRect.y) / svRect.h), 0f, 1f);
        if (Math.abs(saturation - newSaturation) > 0.001f || Math.abs(value - newValue) > 0.001f) {
            saturation = newSaturation;
            value = newValue;
            updateColor();
        }
    }

    private void updateHue(double mouseY) {
        if (rainbow) return;
        float newHue = clamp((float) ((mouseY - hueRect.y) / hueRect.h), 0f, 1f) * 360f;
        if (Math.abs(hue - newHue) > 0.001f) {
            hue = newHue;
            gradientDirty = true;
            updateColor();
        }
    }

    private void updateColor() {
        if (rainbow) {
            setting.rainbow = true;
            setting.speed = speed;
        } else {
            Color rgb = Color.fromHsv(hue, saturation, value);
            rgb.a = (int)(alpha * 255);
            setting.set(rgb.r, rgb.g, rgb.b, rgb.a);
            setting.rainbow = false;
            setting.speed = 1.0f;
        }
        if (onColorChanged != null) {
            onColorChanged.run();
        }
    }

    private float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    @Override public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) { return false; }
    @Override public boolean charTyped(UiContext ui, char chr, int modifiers) { return false; }
    @Override public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) { return false; }
}