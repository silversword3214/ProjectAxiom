package silversword.axiom.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import silversword.axiom.client.hud.components.*;
import silversword.axiom.client.hud.components.client.EnabledModulesHud;

public final class AxiomHudBootstrap {
    private static boolean initialized = false;
    private static boolean layerRegistered = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        HudManager.get().register(new EnabledModulesHud());

        HudManager.get().register(new CoordinatesHud());
        HudManager.get().register(new HardwareHud());
        HudManager.get().register(new FpsHud());
        HudManager.get().register(new InventoryViewer());

        if (!layerRegistered) {
            HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("projectaxiom", "hud"), (ctx, tickCounter) -> {
                HudManager.get().renderAll(ctx, tickCounter);
            });
            layerRegistered = true;
            System.out.println("[Axiom] HUD layer registered");
        }
    }
}