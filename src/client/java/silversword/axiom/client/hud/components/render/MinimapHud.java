package silversword.axiom.client.hud.components.render;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import silversword.axiom.client.hud.BaseHudElement;
import silversword.axiom.client.hud.core.HudContext;
import silversword.axiom.client.render.rendersystem.axiomrenderer.minimap.MinimapRenderer;

public class MinimapHud extends BaseHudElement {

    private static final Identifier NAVIGATOR_TEXTURE =
            Identifier.fromNamespaceAndPath("projectaxiom", "textures/icons/navigation.png");

    private int viewSize = 128;

    public MinimapHud() {
        super("Minimap", 10, 10);
        this.enabled = false;
    }

    @Override public int width(Minecraft mc)  { return viewSize; }
    @Override public int height(Minecraft mc) { return viewSize; }

    public void setViewportSize(int size) {
        this.viewSize = Math.max(64, size);
    }

    @Override
    public void render(HudContext ctx, DeltaTracker tickCounter) {
        if (!enabled) return;
        if (!MinimapRenderer.isEnabled()) return;

        float cx = x + viewSize / 2f;
        float cy = y + viewSize / 2f;
        float radius = viewSize / 2f;

        boolean circular = MinimapRenderer.isCircular();
        boolean rotate   = MinimapRenderer.isRotateWithPlayer();

        float borderThickness = MinimapRenderer.getBorderThickness();
        int   borderColor     = MinimapRenderer.getBorderColor();

        // ─── 1) Minimap-tekstuuri ─────────────────────────────────────────
        ctx.renderer.drawTexture(
                MinimapRenderer.HUD_TEXTURE_ID,
                x, y,
                viewSize, viewSize,
                0xFFFFFFFF
        );

        // ─── 2) Kehys (rinkula) ───────────────────────────────────────────
        if (borderThickness > 0.01f) {
            if (circular) {
                // Pyöreä kehys — piirretään ympyrän ulkoreunalle
                float outlineRadius = radius - borderThickness * 0.5f;
                ctx.renderer.core.addCircleOutline(
                        cx, cy,
                        outlineRadius,
                        borderThickness,
                        borderColor
                );
            } else {
                // Neliönmuotoinen kehys pyöristetyillä kulmilla
                ctx.renderer.core.addRoundedRectOutline(
                        x, y,
                        viewSize, viewSize,
                        4.0f,
                        borderThickness,
                        borderColor
                );
            }
        }

        // ─── 3) Keski-ikoni ───────────────────────────────────────────────
        float iconSize = 12;
        float iconX = cx - iconSize / 2f;
        float iconY = cy - iconSize / 2f;

        if (rotate) {
            ctx.renderer.drawTexture(
                    NAVIGATOR_TEXTURE,
                    iconX, iconY,
                    iconSize, iconSize,
                    0xFFFFFFFF
            );
        } else {
            float yaw = Minecraft.getInstance().player != null
                    ? Minecraft.getInstance().player.getYRot() : 0f;
            ctx.renderer.drawRotatedTexture(
                    NAVIGATOR_TEXTURE,
                    iconX, iconY,
                    iconSize, iconSize,
                    -yaw,
                    0xFFFFFFFF
            );
        }
    }

    @Override
    public void renderEdit(HudContext ctx) {
        float borderThickness = MinimapRenderer.getBorderThickness();
        int   borderColor     = MinimapRenderer.getBorderColor();

        if (borderThickness > 0.01f) {
            if (MinimapRenderer.isCircular()) {
                float cx = x + viewSize / 2f;
                float cy = y + viewSize / 2f;
                float radius = viewSize / 2f - borderThickness * 0.5f;
                ctx.renderer.core.addCircleOutline(cx, cy, radius, borderThickness, borderColor);
            } else {
                ctx.renderer.core.addRoundedRectOutline(
                        x, y, viewSize, viewSize, 4.0f, borderThickness, borderColor);
            }
        }
        ctx.drawScaledText("Minimap", x + 4, y + 4, 0xFFFFFFFF, true, 1.0f);
    }
}