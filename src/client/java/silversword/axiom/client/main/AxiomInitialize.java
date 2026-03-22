package silversword.axiom.client.main;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.PackType;
import net.minecraft.resources.Identifier;
import silversword.axiom.client.config.HudConfigManager;
import silversword.axiom.client.config.PauseUiConfigManager;
import silversword.axiom.client.config.ResourcePackBlockerConfig;
import silversword.axiom.client.config.SettingsConfigManager;
import silversword.axiom.client.event.InputListener;
import silversword.axiom.client.gui.window.WindowManager;
import silversword.axiom.client.hud.HudManager;
import silversword.axiom.client.hud.AxiomHudBootstrap;
import silversword.axiom.client.managers.ModuleKeybindManager;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.eventbus.EventBus;
import silversword.axiom.client.eventbus.IEventBus;
import silversword.axiom.client.modules.waypoints.WaypointCommands;
import silversword.axiom.client.render.rendersystem.CustomRenderingPipelineProvider;
import silversword.axiom.client.render.rendersystem.Renderer2D;
import silversword.axiom.client.render.rendersystem.utils.postprocess.PostProcessShaders;
import silversword.axiom.client.sound.CustomSounds;


import java.lang.invoke.MethodHandles;

public final class AxiomInitialize implements ClientModInitializer {
    public static final IEventBus EVENT_BUS = new EventBus();
    public static final Minecraft mc = Minecraft.getInstance();
    public static final WindowManager pauseWindowManager = new WindowManager();



    @Override
    public void onInitializeClient() {
        // Reload listener
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return Identifier.fromNamespaceAndPath("projectaxiom", "shader_loader");
            }

            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                CustomRenderingPipelineProvider.precompile();
                CustomRenderingPipelineProvider.rebuildAll();
                silversword.axiom.client.render.font.Fonts.refresh();
            }
        });

        // Graphics
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {

            Renderer2D.init();
            PostProcessShaders.init();
            // custom font renderer must be created after the device is available
            silversword.axiom.client.render.font.Fonts.refresh();
        });

        EVENT_BUS.registerLambdaFactory("silversword.axiom", (lookupInMethod, axiomClass) ->
                (MethodHandles.Lookup) lookupInMethod.invoke(null, axiomClass, MethodHandles.lookup()));

        EVENT_BUS.subscribe(this);
        ModuleManager.getInstance().init();

        for (var module : ModuleManager.getInstance().getModules()) {
            EVENT_BUS.subscribe(module);
        }
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