package silversword.axiom.client.render.rendersystem.axiomrenderer.integration;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;
import silversword.axiom.client.event.render.Render2DEvent;
import silversword.axiom.client.main.AxiomInitialize;
import silversword.axiom.client.render.rendersystem.axiomrenderer.RenderAPI;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer2D;

public class FabricHudHook {
    public static void register() {
        HudRenderCallback.EVENT.register((GuiGraphics graphics, DeltaTracker deltaTracker) -> {
            RenderAPI api = RenderAPI.getInstance();
            float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(true);

            Matrix4f scaledProj = new Matrix4f().setOrtho(0, graphics.guiWidth(), graphics.guiHeight(), 0, -1000, 3000);

            Renderer2D renderer = new Renderer2D(graphics, api.getCore(), scaledProj);

            Render2DEvent event = new Render2DEvent(renderer, tickDelta, graphics, graphics.guiWidth(), graphics.guiHeight());
            AxiomInitialize.EVENT_BUS.post(event);

            api.getCore().flush();
        });
    }
}