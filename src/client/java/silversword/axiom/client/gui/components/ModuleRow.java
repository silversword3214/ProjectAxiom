package silversword.axiom.client.gui.components;

import net.minecraft.resources.Identifier;
import silversword.axiom.client.config.ClickGuiConfigManager;
import silversword.axiom.client.gui.core.*;
import silversword.axiom.client.hud.core.HudContext;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.misc.DeathLocationModule;
import silversword.axiom.client.modules.render.NoParticleModule;
import silversword.axiom.client.modules.render.WaypointModule;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.rainbow.RainbowPalette;
import silversword.axiom.client.render.rendersystem.utils.texture.Texture;
import silversword.axiom.client.render.rendersystem.utils.texture.TextureManager;

import java.util.function.Consumer;

public final class ModuleRow implements UiComponent {
    private static final Identifier GEAR_TEXTURE = Identifier.fromNamespaceAndPath("projectaxiom", "textures/icons/gear.png");
    private static Texture gearTexture;

    private Rect bounds = new Rect(0, 0, 10, 10);
    private final AxiomMod module;
    private final Consumer<AxiomMod> onOpenSettings;
    private final String ownerWindowId;
    private final int rowIndex;
    private final boolean isLast;
    private Rect gearRect = new Rect(0, 0, 0, 0);
    private boolean leftDown = false;
    private boolean dragStarted = false;
    private double pressX = 0, pressY = 0;
    private static final double DRAG_THRESHOLD = 4.0;
    private float gearRotation = 0f;
    private static final float HOVER_SPIN_SPEED = 10f;
    private static final float RETURN_SPEED = 20f;
    private float fillProgress = 0f;
    private static final float FILL_SPEED = 0.12f;
    private static final int FILL_ALPHA = 64;

    private float enabledProgress = 0f;
    private static final float ANIM_SPEED = 0.15f;

    private boolean highlighted = false;
    private long highlightEndTime = 0;
    private static final int HIGHLIGHT_COLOR = 0x33FFFF00;

    private float hoverTime = 0f;
    private static final float TOOLTIP_DELAY = 20f;

    public ModuleRow(AxiomMod module, Consumer<AxiomMod> onOpenSettings, String ownerWindowId, int rowIndex, boolean isLast) {
        this.module = module;
        this.onOpenSettings = onOpenSettings;
        this.ownerWindowId = ownerWindowId;
        this.rowIndex = rowIndex;
        this.isLast = isLast;
    }

    public int getRowIndex() { return rowIndex; }
    @Override public Rect getBounds() { return bounds; }
    @Override public void setBounds(Rect r) { bounds = r; }
    @Override public int getPreferredHeight() { return 16; }

    public void highlight(long durationMs) {
        highlighted = true;
        highlightEndTime = System.currentTimeMillis() + durationMs;
    }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        if (gearTexture == null) {
            gearTexture = TextureManager.getTexture(GEAR_TEXTURE);
        }

        if (highlighted && System.currentTimeMillis() > highlightEndTime) {
            highlighted = false;
        }
        if (highlighted) {
            ui.fill(bounds, HIGHLIGHT_COLOR);
        }

        boolean hover = bounds.contains(mouseX, mouseY);
        boolean enabled = module.isEnabled();

        if (enabled) {
            enabledProgress = Math.min(1f, enabledProgress + delta * ANIM_SPEED);
        } else {
            enabledProgress = Math.max(0f, enabledProgress - delta * ANIM_SPEED);
        }

        if (enabledProgress > 0) {
            int barWidth = 2;
            int animatedHeight = (int) (bounds.h * enabledProgress);
            int animatedY = bounds.y + (bounds.h - animatedHeight);
            ui.fill(bounds.x - 1, animatedY, barWidth, animatedHeight, ui.theme.accent);
        }

        int rightPad = 2;
        int gearBtnW = 16;
        int btnH = Math.max(14, bounds.h - 2);
        int gearX = bounds.right() - gearBtnW - rightPad;
        int btnY = bounds.y + 1;
        gearRect = new Rect(gearX, btnY, gearBtnW, btnH);

        boolean gearHover = gearRect.contains(mouseX, mouseY);
        if (gearHover) gearRotation += delta * HOVER_SPIN_SPEED;
        else if (gearRotation > 0) gearRotation = Math.max(0, gearRotation - delta * RETURN_SPEED);
        gearRotation %= 360f;

        if (gearTexture != null) {
            ui.addTexture(GEAR_TEXTURE, gearRect.x + 2, gearRect.y + 2, gearRect.w - 4, gearRect.h - 2, gearRotation, new Color(0xFFFFFFFF));
        } else {
            ui.fill(gearRect, gearHover ? ui.theme.buttonHover : ui.theme.panel);
            String dots = "...";
            int dotsW = ui.textWidth(dots);
            int dtx = gearRect.x + (gearRect.w - dotsW) / 2;
            int dty = gearRect.y + (gearRect.h - ui.fontHeight()) / 2;
            ui.text(dots, dtx, dty, ui.theme.textDim);
        }

        if (hover) fillProgress = Math.min(1f, fillProgress + delta * FILL_SPEED);
        else fillProgress = Math.max(0f, fillProgress - delta * FILL_SPEED);

        if (fillProgress > 0) {
            int fillWidth = (int) ((gearRect.x - 2 - bounds.x) * fillProgress);
            if (fillWidth > 0) {
                int accentWithAlpha = (ui.theme.accent & 0x00FFFFFF) | (FILL_ALPHA << 24);
                ui.fill(bounds.x, bounds.y, fillWidth, bounds.h, accentWithAlpha);
            }
        }

        int nameY = bounds.y + (bounds.h - ui.fontHeight()) / 2 + 3;
        int nameX = bounds.x + 2;

        String displayName = module.getName();
        if (displayName == null) displayName = "";

        // TruncateToFit poistettu kokonaan! Piirretään suoraan displayName.
        if (ClickGuiConfigManager.isRainbowWaveEnabled()) {
            // x ja y ovat ne samat nameX ja nameY kuin tavallisella tekstillä
            ui.drawRainbowText(displayName, nameX, nameY, rowIndex);
        } else {
            ui.text(displayName, nameX, nameY, ui.theme.text);
        }

        if (hover && !gearHover) {
            // Lisätään aikaa deltan verran (delta on yleensä sekunnin murto-osa)
            hoverTime += delta;

            if (hoverTime >= TOOLTIP_DELAY) {
                String description = module.getDescription();
                if (description != null && !description.isEmpty()) {
                    TooltipStack.push(description, mouseX, mouseY);
                }
            }
        } else {
            // Nollataan laskuri heti, kun hiiri poistuu tai siirtyy rattaan päälle
            hoverTime = 0f;
        }
    }


    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (!bounds.contains(mouseX, mouseY)) return false;
        if (button == 0) {
            leftDown = true;
            dragStarted = false;
            pressX = mouseX;
            pressY = mouseY;
            if (gearRect.contains(mouseX, mouseY)) {
                leftDown = false;
                if (module instanceof WaypointModule) ((WaypointModule) module).openManager();
                else if (module instanceof NoParticleModule) ((NoParticleModule) module).openManager();
                else if (module instanceof DeathLocationModule) ((DeathLocationModule) module).openListWindow();
                else onOpenSettings.accept(module);
                return true;
            }
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {
        if (button != 0) return;
        boolean wasDown = leftDown;
        leftDown = false;
        if (dragStarted) return;
        if (wasDown && bounds.contains(mouseX, mouseY)) {
            if (gearRect.contains(mouseX, mouseY)) return;
            module.toggle();
        }
    }

    @Override
    public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) {
        if (button != 0) return false;
        if (!leftDown) return false;
        if (dragStarted) return true;
        if (gearRect.contains(pressX, pressY)) return false;
        double dist = Math.hypot(mouseX - pressX, mouseY - pressY);
        if (dist >= DRAG_THRESHOLD) {
            dragStarted = true;
            DragState.start(module, ownerWindowId, rowIndex, pressX, pressY);
            return true;
        }
        return false;
    }

    public AxiomMod getModule() { return module; }

    @Override public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) { return false; }
    @Override public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) { return false; }
    @Override public boolean charTyped(UiContext ui, char chr, int modifiers) { return false; }
}