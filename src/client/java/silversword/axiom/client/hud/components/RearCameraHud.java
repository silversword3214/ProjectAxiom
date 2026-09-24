package silversword.axiom.client.hud.components;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import silversword.axiom.client.hud.BaseHudElement;
import silversword.axiom.client.hud.core.HudContext;
import silversword.axiom.client.render.rendersystem.axiomrenderer.rearcamera.RearCameraRenderer;

public class RearCameraHud extends BaseHudElement {

    private static final int PAD = 4;
    private static final int RADIUS = 6;

    // Oletusarvot — moduuli ylikirjoittaa nämä
    private int viewW = 200;
    private int viewH = 120;

    public RearCameraHud() {
        super("RearCamera", 10, 10);
        this.enabled = false;
    }

    @Override public int width(Minecraft mc)  { return viewW + PAD * 2; }
    @Override public int height(Minecraft mc) { return viewH + PAD * 2; }

    /** Asettaa näkymän koon. Kutsutaan moduulista. */
    public void setViewportSize(int w, int h) {
        this.viewW = Math.max(16, w);
        this.viewH = Math.max(9,  h);
    }

    @Override
    public void render(HudContext ctx, DeltaTracker tickCounter) {
        if (!enabled) return;

        var rt = RearCameraRenderer.getRearTarget();
        if (rt == null) return;
        if (rt.getColorTextureView() == null) return;

        int w = width(ctx.mc);
        int h = height(ctx.mc);

        ctx.fillRounded(x, y, w, h, RADIUS, 0xCC000000);
        ctx.drawRoundedOutline(x, y, w, h, RADIUS, 0xFFFFFFFF, 1.0);

        // KÄYTÄ FLIPPED-versiota
        ctx.renderer.drawTextureFlippedY(
                RearCameraRenderer.HUD_TEXTURE_ID,
                x + PAD, y + PAD,
                viewW, viewH,
                0xFFFFFFFF
        );
    }

    @Override
    public void renderEdit(HudContext ctx) {
        int w = width(ctx.mc);
        int h = height(ctx.mc);
        ctx.fillRounded(x, y, w, h, RADIUS, 0x80000000);
        ctx.drawRoundedOutline(x, y, w, h, RADIUS, 0xFFFFFFFF, 1.0);
        ctx.drawScaledText("Rear Camera", x + PAD, y + PAD,
                0xFFFFFFFF, true, 1.0f);
    }
}