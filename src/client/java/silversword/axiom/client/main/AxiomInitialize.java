package silversword.axiom.client.main;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import silversword.axiom.client.config.*;
import silversword.axiom.client.event.InputListener;
import silversword.axiom.client.eventbus.EventBus;
import silversword.axiom.client.gui.window.WindowManager;
import silversword.axiom.client.hud.AxiomHudBootstrap;
import silversword.axiom.client.hud.HudManager;
import silversword.axiom.client.managers.ModuleKeybindManager;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.waypoints.WaypointCommands;
import silversword.axiom.client.render.font.Fonts;
import silversword.axiom.client.render.rendersystem.axiomrenderer.core.RenderPipelines;
import silversword.axiom.client.render.rendersystem.axiomrenderer.integration.FabricHudHook;
import silversword.axiom.client.sound.CustomSounds;

public final class AxiomInitialize implements ClientModInitializer {
    public static final EventBus EVENT_BUS = new EventBus();
    public static final WindowManager pauseWindowManager = new WindowManager();

    // Julkinen kenttä olemassa olevaa koodia varten, alustetaan kun peli käynnistyy
    public static Minecraft mc;

    // Metodi, joka hakee tuoreen instanssin aina kun sitä kutsutaan
    public static Minecraft mc() {
        return Minecraft.getInstance();
    }

    @Override
    public void onInitializeClient() {
        // Päivitetään mc-kenttä heti kun pelin instanssi on valmis
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            mc = client;

            Fonts.refresh();
            String savedFont = FontConfigManager.loadFont();
            if (savedFont != null) {
                silversword.axiom.client.render.font.Fonts.setFont(savedFont);
            }
            RenderPipelines.rebuildAll();
        });

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return Identifier.fromNamespaceAndPath("projectaxiom", "pipeline_rebuilder");
            }

            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                RenderPipelines.rebuildAll();
            }
        });

        // Rekisteröidään hookit
        FabricHudHook.register();

        // Rekisteröidään eventbus ja moduulit
        EVENT_BUS.register(this);
        ModuleManager.getInstance().init();

        ResourcePackBlockerConfig.load();
        SettingsConfigManager.loadAll();
        ModuleKeybindManager.register();
        InputListener.register();
        AxiomHudBootstrap.init();
        HudConfigManager.load(HudManager.get());
        CustomSounds.initialize();
        WaypointCommands.register();
        PauseUiConfigManager.load(pauseWindowManager);

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            HudConfigManager.save(HudManager.get());
            SettingsConfigManager.saveAll();
            PauseUiConfigManager.save(pauseWindowManager);
        });

        System.out.println("[Axiom] Entrypoint loaded.");
    }
}