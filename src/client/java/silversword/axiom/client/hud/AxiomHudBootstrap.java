package silversword.axiom.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import silversword.axiom.client.hud.components.*;
import silversword.axiom.client.hud.components.client.EnabledModulesHud;
import silversword.axiom.client.hud.components.client.WatermarkHud;

import silversword.axiom.client.render.rendersystem.utils.render.RenderUtils;

public final class AxiomHudBootstrap {
    private static boolean initialized = false;
    private static boolean layerRegistered = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        // Rekisteröi elementit HudManageriin
        HudManager.get().register(new WatermarkHud());
        HudManager.get().register(new EnabledModulesHud());

        HudManager.get().register(new CoordinatesHud());
        HudManager.get().register(new HardwareHud());


        // Rekisteröi Fabric HUD -layer VAIN KERRAN
        if (!layerRegistered) {
            HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("projectaxiom", "hud"), (ctx, tickCounter) -> {
                RenderUtils.setup2DProjection(ctx.guiWidth(), ctx.guiHeight());

                HudManager.get().renderAll(ctx, tickCounter);

            });
            layerRegistered = true;
            System.out.println("[Axiom] HUD layer registered");
        }
    }
}