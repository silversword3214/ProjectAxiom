package silversword.axiom.client.render.rendersystem.axiomrenderer.integration;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silversword.axiom.client.event.render.Render2DEvent;
import silversword.axiom.client.main.AxiomInitialize;
import silversword.axiom.client.render.rendersystem.axiomrenderer.RenderAPI;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer2D;

public class FabricHudHook {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricHudHook.class);
    private static int frameCounter = 0;

    public static void register() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("projectaxiom", "axiom_hud"),
                (GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) -> {
                    try {

                        RenderAPI api = RenderAPI.getInstance();
                        if (api == null || api.getCore() == null) {
                            if (frameCounter++ % 300 == 0) {
                                LOGGER.warn("RenderAPI or Core is null!");
                            }
                            return;
                        }

                        float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(true);
                        Matrix4f scaledProj = (new Matrix4f()).setOrtho(
                                0.0F,
                                (float) graphics.guiWidth(),
                                (float) graphics.guiHeight(),
                                0.0F,
                                -1000.0F,
                                3000.0F
                        );

                        Renderer2D renderer = new Renderer2D(graphics, api.getCore(), scaledProj);
                        Render2DEvent event = new Render2DEvent(
                                renderer,
                                tickDelta,
                                graphics,
                                graphics.guiWidth(),
                                graphics.guiHeight()
                        );

                        AxiomInitialize.EVENT_BUS.post(event);

                        api.getCore().flush();


                    } catch (Exception e) {
                        if (frameCounter++ % 300 == 0) {
                            LOGGER.error("FabricHudHook render error", e);
                        }
                    }
                });
    }
}