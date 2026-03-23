package silversword.axiom.client.render.rendersystem.axiomrenderer.integration;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import silversword.axiom.client.event.render.Render2DEvent;
import silversword.axiom.client.main.AxiomInitialize;
import silversword.axiom.client.render.rendersystem.axiomrenderer.RenderAPI;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer2D;

public class FabricHudHook {
    public static void register() {
        HudRenderCallback.EVENT.register((GuiGraphics graphics, DeltaTracker deltaTracker) -> {
            RenderAPI api = RenderAPI.getInstance();
            float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(true);
            api.beginHUDUnscaled(graphics, tickDelta);
            Renderer2D renderer = api.hud();

            Render2DEvent event = new Render2DEvent(renderer, tickDelta, graphics, graphics.guiWidth(), graphics.guiHeight());
            AxiomInitialize.EVENT_BUS.post(event);

            api.end();
        });
    }
}