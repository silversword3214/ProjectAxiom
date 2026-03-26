package silversword.axiom.client.gui.components;

import net.minecraft.resources.Identifier;
import silversword.axiom.client.gui.core.*;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.misc.DeathLocationModule;
import silversword.axiom.client.modules.render.NoParticleModule;
import silversword.axiom.client.modules.render.WaypointModule;
import silversword.axiom.client.render.rendersystem.axiomrenderer.RenderAPI;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.texture.Texture;
import silversword.axiom.client.render.rendersystem.utils.texture.TextureManager;
import silversword.axiom.client.utils.render.DrawTexture;

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
    private double pressX = 0;
    private double pressY = 0;

    private static final double DRAG_THRESHOLD = 4.0;
    private static final boolean DEBUG_SHOW_TEXT_BOX = false;

    // Gear rotation animation
    private float gearRotation = 0f;
    private static final float HOVER_SPIN_SPEED = 10f;
    private static final float RETURN_SPEED = 20f;

    // Background fill animation
    private float fillProgress = 0f;
    private static final float FILL_SPEED = 0.12f;
    private static final int FILL_ALPHA = 64;

    public ModuleRow(
            AxiomMod module,
            Consumer<AxiomMod> onOpenSettings,
            String ownerWindowId,
            int rowIndex,
            boolean isLast
    ) {
        this.module = module;
        this.onOpenSettings = onOpenSettings;
        this.ownerWindowId = ownerWindowId;
        this.rowIndex = rowIndex;
        this.isLast = isLast;

        if (gearTexture == null) {
            gearTexture = TextureManager.getTexture(GEAR_TEXTURE);
        }
    }

    public int getRowIndex() {
        return rowIndex;
    }

    @Override
    public Rect getBounds() {
        return bounds;
    }

    @Override
    public void setBounds(Rect r) {
        bounds = r;
    }

    @Override
    public int getPreferredHeight() {
        return 16;
    }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        boolean hover = bounds.contains(mouseX, mouseY);
        boolean enabled = module.isEnabled();

        // Small vertical bar if module is enabled
        if (enabled) {
            ui.fillRounded(bounds.x, bounds.y, 3, bounds.h, ui.theme.accent, 1.5);
        }

        // Right side buttons
        int rightPad = 2;
        int gearBtnW = 16;
        int btnH = Math.max(14, bounds.h - 2);
        int gearX = bounds.right() - gearBtnW - rightPad;
        int btnY = bounds.y + 1;
        gearRect = new Rect(gearX, btnY, gearBtnW, btnH);

        // Gear button
        boolean gearHover = gearRect.contains(mouseX, mouseY);

        // Update gear rotation
        if (gearHover) {
            gearRotation += delta * HOVER_SPIN_SPEED;
        } else if (gearRotation > 0) {
            gearRotation = Math.max(0, gearRotation - delta * RETURN_SPEED);
        }
        gearRotation %= 360f;



        if (gearTexture != null) {
            RenderAPI.getInstance().getCore().addRotatedTexture(
                    GEAR_TEXTURE,
                    gearRect.x + 2,
                    gearRect.y + 2,
                    gearRect.w - 4,
                    gearRect.h - 2,
                    gearRotation,
                    0xFFFFFFFF
            );
        } else {
            // Fallback to dots
            ui.fill(gearRect, gearHover ? ui.theme.buttonHover : ui.theme.panel);
            String dots = "...";
            int dotsW = ui.textWidth(dots);
            int dtx = gearRect.x + (gearRect.w - dotsW) / 2;
            int dty = gearRect.y + (gearRect.h - ui.fontHeight()) / 2;
            ui.text(dots, dtx, dty, ui.theme.textDim);
        }

        // Update fill animation based on hover state
        if (hover) {
            fillProgress = Math.min(1f, fillProgress + delta * FILL_SPEED);
        } else {
            fillProgress = Math.max(0f, fillProgress - delta * FILL_SPEED);
        }

        // Draw animated background fill (from left to right) – after gearRect is set
        if (fillProgress > 0) {
            int fillWidth = (int) ((gearRect.x - 2 - bounds.x) * fillProgress);
            if (fillWidth > 0) {
                int accentWithAlpha = (ui.theme.accent & 0x00FFFFFF) | (FILL_ALPHA << 24);
                ui.fill(bounds.x, bounds.y, fillWidth, bounds.h, accentWithAlpha);
            }
        }

        // Module name – no background, only text with hover color
        int nameX = bounds.x + ui.theme.innerPadding + 4;
        int rightLimit = gearRect.x;
        if (nameX > rightLimit) nameX = rightLimit;

        String name = module.getName();
        if (name == null) name = "";
        int nameY = bounds.y + bounds.h / 2 - ui.fontHeight() / 2 + 4;

        // Text color: normal text (could be changed later if needed)
        int textColor = ui.theme.text;
        ui.text(name, nameX, nameY, textColor);

        // Tooltip: show module description on hover (but not over gear button)
        if (hover && !gearHover) {
            String description = module.getDescription();
            if (description != null && !description.isEmpty()) {
                TooltipStack.push(description, mouseX, mouseY);
            }
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
                // Handle special modules
                if (module instanceof WaypointModule) {
                    ((WaypointModule) module).openManager();
                } else if (module instanceof NoParticleModule) {
                    ((NoParticleModule) module).openManager();
                } else if (module instanceof DeathLocationModule) {
                    ((DeathLocationModule) module).openListWindow();
                } else {
                    // Normal setting
                    onOpenSettings.accept(module);
                }
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
}