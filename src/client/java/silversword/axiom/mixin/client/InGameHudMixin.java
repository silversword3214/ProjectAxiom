package silversword.axiom.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.event.render.Render2DEvent;
import silversword.axiom.client.gui.screen.ClickGuiScreen;
import silversword.axiom.client.main.AxiomInitialize;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.misc.PotionEffects;
import silversword.axiom.client.modules.render.NoVignette;
import silversword.axiom.client.render.rendersystem.Renderer2D;
import silversword.axiom.client.render.rendersystem.utils.render.RenderUtils;

import static silversword.axiom.client.main.AxiomInitialize.mc;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (mc.options.hudHidden) return;

        AxiomInitialize.EVENT_BUS.post(new Render2DEvent());

        // 1. Setting up 2D-projection
        RenderUtils.unscaledProjection();

        // 2. Begin meshes
        Renderer2D.COLOR.begin();
        Renderer2D.TEXTURE.begin();

        // 3. Post event
        AxiomInitialize.EVENT_BUS.post(Render2DEvent.get(
                context,
                context.getScaledWindowWidth(),
                context.getScaledWindowHeight(),
                tickCounter.getTickProgress(true)
        ));

        // 4. End meshes
        Renderer2D.COLOR.end();
        Renderer2D.TEXTURE.end();

        // 5. Rendering COLOR
        Renderer2D.COLOR.render();

        // 6. Returning 3D-projection for Renderer3D
        RenderUtils.scaledProjection();
    }

    @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderVignetteOverlay(DrawContext context, Entity entity, CallbackInfo ci) {
        NoVignette module = ModuleManager.getInstance().getModule(NoVignette.class);
        if (module != null && module.isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderMiscOverlays", at = @At("HEAD"), cancellable = true)
    private void onRenderMiscOverlays(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen instanceof ClickGuiScreen) ci.cancel();
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void onRenderCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen instanceof ClickGuiScreen) ci.cancel();
    }

    @Inject(method = "renderMainHud", at = @At("HEAD"), cancellable = true)
    private void onRenderMainHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen instanceof ClickGuiScreen) ci.cancel();

    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderStatusEffectOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen instanceof ClickGuiScreen) ci.cancel();

        PotionEffects potionMod = ModuleManager.getInstance().getModule(PotionEffects.class);
        if (potionMod != null && potionMod.isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderBossBarHud", at = @At("HEAD"), cancellable = true)
    private void onRenderBossBarHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen instanceof ClickGuiScreen) ci.cancel();
    }

    @Inject(method = "renderDemoTimer", at = @At("HEAD"), cancellable = true)
    private void onRenderDemoTimer(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen instanceof ClickGuiScreen) ci.cancel();
    }

    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void onRenderScoreboardSidebar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen instanceof ClickGuiScreen) ci.cancel();
    }

    @Inject(method = "renderOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void onRenderOverlayMessage(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen instanceof ClickGuiScreen) ci.cancel();
    }

    @Inject(method = "renderTitleAndSubtitle", at = @At("HEAD"), cancellable = true)
    private void onRenderTitleAndSubtitle(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen instanceof ClickGuiScreen) ci.cancel();
    }

    @Inject(method = "renderChat", at = @At("HEAD"), cancellable = true)
    private void onRenderChat(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen instanceof ClickGuiScreen) ci.cancel();
    }

    @Inject(method = "renderPlayerList", at = @At("HEAD"), cancellable = true)
    private void onRenderPlayerList(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen instanceof ClickGuiScreen) ci.cancel();
    }

    @Inject(method = "renderSubtitlesHud", at = @At("HEAD"), cancellable = true)
    private void onRenderSubtitlesHud(DrawContext context, boolean defer, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen instanceof ClickGuiScreen) ci.cancel();
    }

    @Inject(method = "renderAutosaveIndicator", at = @At("HEAD"), cancellable = true)
    private void onRenderAutosaveIndicator(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen instanceof ClickGuiScreen) ci.cancel();
    }

    @Inject(method = "renderSleepOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderSleepOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (MinecraftClient.getInstance().currentScreen instanceof ClickGuiScreen) ci.cancel();
    }

}