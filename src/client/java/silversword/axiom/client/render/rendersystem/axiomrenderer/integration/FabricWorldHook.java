package silversword.axiom.client.render.rendersystem.axiomrenderer.integration;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.main.AxiomInitialize;
import silversword.axiom.client.render.rendersystem.axiomrenderer.RenderAPI;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer3D;
import silversword.axiom.client.render.rendersystem.utils.render.RenderUtils;

public class FabricWorldHook {
    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            Minecraft mc = Minecraft.getInstance();
            float tickDelta = RenderUtils.getTickDelta();
            Camera camera = mc.gameRenderer.getMainCamera();
            Matrix4f projection = RenderUtils.getProjectionMatrix(tickDelta);
            Matrix4f view = RenderUtils.getViewMatrix(camera);

            RenderAPI api = RenderAPI.getInstance();
            Renderer3D renderer = new Renderer3D(api.getCore(), projection, view, tickDelta);

            Render3DEvent event = new Render3DEvent(
                    renderer,
                    tickDelta,
                    camera.position(),
                    projection,
                    view
            );

            AxiomInitialize.EVENT_BUS.post(event);
            api.end();
        });
    }
}