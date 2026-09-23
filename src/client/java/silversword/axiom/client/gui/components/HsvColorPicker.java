package silversword.axiom.client.gui.components;

import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.render.rendersystem.utils.color.rainbow.RainbowColor;

import java.util.ArrayList;
import java.util.List;

public class HsvColorPicker implements UiComponent {

    public enum Mode { SQUARE, WHEEL }

    private final SettingColor setting;
    private final Runnable onColorChanged;

    private Rect bounds;
    private Rect pickerRect;
    private Rect sideSliderRect;
    private Rect currentColorRect;

    private float hue;
    private float saturation;
    private float value;

    private int r, g, b;
    private float alpha;
    private float speed;
    private boolean rainbow;

    private Mode mode = Mode.SQUARE;

    private boolean draggingPicker;
    private boolean draggingSide;

    private int[][] gradientCache;
    private boolean gradientDirty = true;
    private float cachedWheelValue = -1f;

    // Alikomponentit
    private final List<UiComponent> children = new ArrayList<>();
    private ActionButton rainbowToggle;
    private ActionButton modeToggle;
    private Slider rSlider, gSlider, bSlider, aSlider, speedSlider;

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

        syncHsvToRgb();

        this.rainbowToggle = new ActionButton(getRainbowText(), () -> {
            this.rainbow = !this.rainbow;
            rainbowToggle.setLabel(getRainbowText());
            updateColor();
        });

        this.modeToggle = new ActionButton("Style: SQUARE", () -> {
            this.mode = (this.mode == Mode.SQUARE) ? Mode.WHEEL : Mode.SQUARE;
            this.modeToggle.setLabel("Style: " + this.mode.name());
            this.gradientDirty = true;
        });

        this.rSlider = new Slider("Red", 0, 255, 1, () -> this.r, val -> { this.r = (int)val; syncRgbToHsv(); });
        this.gSlider = new Slider("Green", 0, 255, 1, () -> this.g, val -> { this.g = (int)val; syncRgbToHsv(); });
        this.bSlider = new Slider("Blue", 0, 255, 1, () -> this.b, val -> { this.b = (int)val; syncRgbToHsv(); });
        this.aSlider = new Slider("Alpha", 0, 100, 1, () -> this.alpha * 100, val -> { this.alpha = (float)(val / 100f); updateColor(); });
        this.speedSlider = new Slider("Speed", 0.1, 5.0, 0.1, () -> this.speed, val -> { this.speed = (float)val; updateColor(); });

        children.add(rainbowToggle);
        children.add(modeToggle);
        children.add(rSlider);
        children.add(gSlider);
        children.add(bSlider);
        children.add(aSlider);
        children.add(speedSlider);
    }

    private String getRainbowText() {
        return rainbow ? "Rainbow: ON" : "Rainbow: OFF";
    }

    @Override
    public Rect getBounds() {
        return bounds;
    }

    @Override
    public void setBounds(Rect bounds) {
        this.bounds = bounds;

        int padding = 8;
        int hueWidth = 16;
        int topAreaHeight = 180; // Kasvatettu korkeus ympyrÃ¤lle
        int rightColumnWidth = 80;

        pickerRect = new Rect(
                bounds.x + padding,
                bounds.y + padding,
                bounds.w - padding * 4 - hueWidth - rightColumnWidth,
                topAreaHeight
        );

        sideSliderRect = new Rect(
                pickerRect.right() + padding,
                bounds.y + padding,
                hueWidth,
                topAreaHeight
        );

        int rightX = sideSliderRect.right() + padding;
        currentColorRect = new Rect(rightX, bounds.y + padding, rightColumnWidth, 40);

        modeToggle.setBounds(new Rect(rightX, currentColorRect.bottom() + padding, rightColumnWidth, 24));
        rainbowToggle.setBounds(new Rect(rightX, modeToggle.getBounds().bottom() + 4, rightColumnWidth, 24));

        int currentY = pickerRect.bottom() + padding + 15;
        int rowHeight = 24;
        int rowPadding = 6;
        int compWidth = bounds.w - padding * 2;

        rSlider.setBounds(new Rect(bounds.x + padding, currentY, compWidth, rowHeight)); currentY += rowHeight + rowPadding;
        gSlider.setBounds(new Rect(bounds.x + padding, currentY, compWidth, rowHeight)); currentY += rowHeight + rowPadding;
        bSlider.setBounds(new Rect(bounds.x + padding, currentY, compWidth, rowHeight)); currentY += rowHeight + rowPadding;
        aSlider.setBounds(new Rect(bounds.x + padding, currentY, compWidth, rowHeight)); currentY += rowHeight + rowPadding;
        speedSlider.setBounds(new Rect(bounds.x + padding, currentY, compWidth, rowHeight));

        gradientDirty = true;
    }

    @Override
    public int getPreferredHeight() {
        return 1000;
    }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        if (mode == Mode.SQUARE) {
            renderSquareGradientOptimized(ui);
            renderHueSliderOptimized(ui);
        } else {
            renderWheelGradientOptimized(ui);
            renderValueSliderOptimized(ui);
        }

        renderCursors(ui);

        Color preview = rainbow ? new RainbowColor().set(setting).setSpeed(speed) : Color.fromHsv(hue, saturation, value);
        preview.a = (int)(alpha * 255);

        ui.fillRounded(currentColorRect, preview.getARGB(), ui.theme.radius);
        ui.drawRoundedOutline(currentColorRect, ui.theme.border, ui.theme.radius, 1.0);

        for (UiComponent comp : children) {
            if (comp == speedSlider && !rainbow) continue;
            comp.render(ui, mouseX, mouseY, delta);
        }
    }

    private void syncHsvToRgb() {
        Color c = Color.fromHsv(hue, saturation, value);
        this.r = c.r;
        this.g = c.g;
        this.b = c.b;
        updateColor();
    }

    private void syncRgbToHsv() {
        float rF = r / 255f, gF = g / 255f, bF = b / 255f;
        float max = Math.max(rF, Math.max(gF, bF));
        float min = Math.min(rF, Math.min(gF, bF));
        float d = max - min;

        this.value = max;
        this.saturation = max == 0 ? 0 : d / max;

        if (max == min) {
            this.hue = 0;
        } else {
            if (max == rF) this.hue = (gF - bF) / d + (gF < bF ? 6 : 0);
            else if (max == gF) this.hue = (bF - rF) / d + 2;
            else if (max == bF) this.hue = (rF - gF) / d + 4;
            this.hue /= 6f;
            this.hue *= 360f;
        }

        gradientDirty = true;
        updateColor();
    }

    private void updateColor() {
        if (rainbow) {
            setting.rainbow = true;
            setting.speed = speed;
        } else {
            setting.set(r, g, b, (int)(alpha * 255));
            setting.rainbow = false;
        }
        if (onColorChanged != null) onColorChanged.run();
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (button != 1) return false;

        for (UiComponent comp : children) {
            if (comp == speedSlider && !rainbow) continue;
            if (comp.getBounds().contains(mouseX, mouseY) && comp.mouseClicked(ui, mouseX, mouseY, button)) {
                return true;
            }
        }

        if (pickerRect.contains(mouseX, mouseY)) {
            draggingPicker = true;
            updatePickerTarget(mouseX, mouseY);
            return true;
        }

        if (sideSliderRect.contains(mouseX, mouseY)) {
            draggingSide = true;
            updateSideTarget(mouseY);
            return true;
        }

        return false;
    }

    @Override
    public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {
        draggingPicker = false;
        draggingSide = false;
        for (UiComponent comp : children) comp.mouseReleased(ui, mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        for (UiComponent comp : children) {
            if (comp == speedSlider && !rainbow) continue;
            if (comp.mouseDragged(ui, mouseX, mouseY, button, deltaX, deltaY)) return true;
        }

        if (draggingPicker) {
            updatePickerTarget(mouseX, mouseY);
            return true;
        }
        if (draggingSide) {
            updateSideTarget(mouseY);
            return true;
        }
        return false;
    }

    private void updatePickerTarget(double mouseX, double mouseY) {
        if (mode == Mode.SQUARE) {
            this.saturation = clamp((float) ((mouseX - pickerRect.x) / pickerRect.w), 0f, 1f);
            this.value = 1f - clamp((float) ((mouseY - pickerRect.y) / pickerRect.h), 0f, 1f);
        } else {
            int size = Math.min(pickerRect.w, pickerRect.h);
            float cx = pickerRect.x + (pickerRect.w / 2f);
            float cy = pickerRect.y + (pickerRect.h / 2f);
            float radius = size / 2f;

            float dx = (float) (mouseX - cx);
            float dy = (float) (mouseY - cy);
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            this.saturation = clamp(dist / radius, 0f, 1f);

            float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
            if (angle < 0) angle += 360f;
            this.hue = angle;
        }
        syncHsvToRgb();
    }

    private void updateSideTarget(double mouseY) {
        float val = clamp((float) ((mouseY - sideSliderRect.y) / sideSliderRect.h), 0f, 1f);
        if (mode == Mode.SQUARE) {
            this.hue = val * 360f;
            gradientDirty = true;
        } else {
            this.value = 1f - val;
            gradientDirty = true;
        }
        syncHsvToRgb();
    }

    private float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private void renderSquareGradientOptimized(UiContext ui) {
        if (pickerRect.w <= 0 || pickerRect.h <= 0) return;

        if (gradientDirty || gradientCache == null || gradientCache.length != pickerRect.w || gradientCache[0].length != pickerRect.h) {
            gradientCache = new int[pickerRect.w][pickerRect.h];
            for (int y = 0; y < pickerRect.h; y++) {
                float v = 1f - (float) y / pickerRect.h;
                for (int x = 0; x < pickerRect.w; x++) {
                    float s = (float) x / pickerRect.w;
                    gradientCache[x][y] = Color.fromHsv(hue, s, v).getARGB();
                }
            }
            gradientDirty = false;
        }

        int step = Math.max(1, Math.min(pickerRect.w, pickerRect.h) / 80);
        for (int y = 0; y < pickerRect.h; y += step) {
            for (int x = 0; x < pickerRect.w; x += step) {
                int w = Math.min(step, pickerRect.w - x);
                int h = Math.min(step, pickerRect.h - y);
                ui.fill(pickerRect.x + x, pickerRect.y + y, w, h, gradientCache[x][y]);
            }
        }
        ui.drawOutline(pickerRect, ui.theme.border);
    }

    private void renderHueSliderOptimized(UiContext ui) {
        if (sideSliderRect.h <= 0) return;
        int step = Math.max(1, sideSliderRect.h / 60);
        for (int y = 0; y < sideSliderRect.h; y += step) {
            float h = 360f * y / sideSliderRect.h;
            int height = Math.min(step, sideSliderRect.h - y);
            ui.fill(sideSliderRect.x, sideSliderRect.y + y, sideSliderRect.w, height, Color.fromHsv(h, 1f, 1f).getARGB());
        }
        ui.drawOutline(sideSliderRect, ui.theme.border);
    }

    private void renderWheelGradientOptimized(UiContext ui) {
        if (pickerRect.w <= 0 || pickerRect.h <= 0) return;

        int size = Math.min(pickerRect.w, pickerRect.h);
        int cx = size / 2;
        int cy = size / 2;
        float radius = size / 2f;

        if (gradientDirty || gradientCache == null || gradientCache.length != size || gradientCache[0].length != size || Math.abs(cachedWheelValue - value) > 0.01f) {
            gradientCache = new int[size][size];
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    float dx = x - cx;
                    float dy = y - cy;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);

                    if (dist > radius) {
                        gradientCache[x][y] = 0;
                    } else {
                        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
                        if (angle < 0) angle += 360f;
                        float s = dist / radius;
                        gradientCache[x][y] = Color.fromHsv(angle, s, value).getARGB();
                    }
                }
            }
            cachedWheelValue = value;
            gradientDirty = false;
        }

        // TÃ¤ysi tarkkuus ympyrÃ¤lle, step on aina 1
        int step = 1;
        int offsetX = pickerRect.x + (pickerRect.w - size) / 2;
        int offsetY = pickerRect.y + (pickerRect.h - size) / 2;

        for (int y = 0; y < size; y += step) {
            for (int x = 0; x < size; x += step) {
                int color = gradientCache[x][y];
                if (color != 0) {
                    ui.fill(offsetX + x, offsetY + y, 1, 1, color);
                }
            }
        }
    }

    private void renderValueSliderOptimized(UiContext ui) {
        if (sideSliderRect.h <= 0) return;
        int step = Math.max(1, sideSliderRect.h / 60);
        for (int y = 0; y < sideSliderRect.h; y += step) {
            float v = 1f - (float) y / sideSliderRect.h;
            int height = Math.min(step, sideSliderRect.h - y);
            ui.fill(sideSliderRect.x, sideSliderRect.y + y, sideSliderRect.w, height, Color.fromHsv(hue, saturation, v).getARGB());
        }
        ui.drawOutline(sideSliderRect, ui.theme.border);
    }

    private void renderCursors(UiContext ui) {
        if (mode == Mode.SQUARE) {
            int cx = (int) (pickerRect.x + saturation * pickerRect.w);
            int cy = (int) (pickerRect.y + (1 - value) * pickerRect.h);
            ui.fillCircle(cx, cy, 3, 0xFFFFFFFF);
            ui.fillCircle(cx, cy, 2, 0xFF000000);

            int hy = (int) (sideSliderRect.y + (hue / 360f) * sideSliderRect.h);
            ui.fill(sideSliderRect.x - 2, hy - 2, sideSliderRect.w + 4, 4, 0xFFFFFFFF);
            ui.fill(sideSliderRect.x - 1, hy - 1, sideSliderRect.w + 2, 2, 0xFF000000);

        } else {
            int size = Math.min(pickerRect.w, pickerRect.h);
            float cx = pickerRect.x + (pickerRect.w / 2f);
            float cy = pickerRect.y + (pickerRect.h / 2f);
            float radius = size / 2f;

            float radAngle = (float) Math.toRadians(hue);
            int px = (int) (cx + Math.cos(radAngle) * (saturation * radius));
            int py = (int) (cy + Math.sin(radAngle) * (saturation * radius));
            ui.fillCircle(px, py, 3, 0xFFFFFFFF);
            ui.fillCircle(px, py, 2, 0xFF000000);

            int vy = (int) (sideSliderRect.y + (1f - value) * sideSliderRect.h);
            ui.fill(sideSliderRect.x - 2, vy - 2, sideSliderRect.w + 4, 4, 0xFFFFFFFF);
            ui.fill(sideSliderRect.x - 1, vy - 1, sideSliderRect.w + 2, 2, 0xFF000000);
        }
    }
}