package silversword.axiom.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.entity.Entity;
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

@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (mc.options.hideGui) return;

        AxiomInitialize.EVENT_BUS.post(new Render2DEvent());

        // 1. Setting up 2D-projection
        RenderUtils.unscaledProjection();

        // 2. Begin meshes
        Renderer2D.COLOR.begin();
        Renderer2D.TEXTURE.begin();

        // 3. Post event
        AxiomInitialize.EVENT_BUS.post(Render2DEvent.get(
                context,
                context.guiWidth(),
                context.guiHeight(),
                tickCounter.getGameTimeDeltaPartialTick(true)
        ));

        // 4. End meshes
        Renderer2D.COLOR.end();
        Renderer2D.TEXTURE.end();

        // 5. Rendering COLOR
        Renderer2D.COLOR.render();

        // 6. Returning 3D-projection for Renderer3D
        RenderUtils.scaledProjection();
    }

    @Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true)
    private void onRenderVignetteOverlay(GuiGraphics context, Entity entity, CallbackInfo ci) {
        NoVignette module = ModuleManager.getInstance().getModule(NoVignette.class);
        if (module != null && module.isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderCameraOverlays", at = @At("HEAD"), cancellable = true)
    private void onRenderMiscOverlays(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof ClickGuiScreen) ci.cancel();
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void onRenderCrosshair(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof ClickGuiScreen) ci.cancel();
    }

    @Inject(method = "renderHotbarAndDecorations", at = @At("HEAD"), cancellable = true)
    private void onRenderMainHud(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof ClickGuiScreen) ci.cancel();

    }

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void onRenderStatusEffectOverlay(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof ClickGuiScreen) ci.cancel();

        PotionEffects potionMod = ModuleManager.getInstance().getModule(PotionEffects.class);
        if (potionMod != null && potionMod.isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderBossOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderBossBarHud(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof ClickGuiScreen) ci.cancel();
    }

    @Inject(method = "renderDemoOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderDemoTimer(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof ClickGuiScreen) ci.cancel();
    }

    // TODO(Ravel): target method renderScoreboardSidebar is ambiguous
    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void onRenderScoreboardSidebar(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof ClickGuiScreen) ci.cancel();
    }

    @Inject(method = "renderOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void onRenderOverlayMessage(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof ClickGuiScreen) ci.cancel();
    }

    @Inject(method = "renderTitle", at = @At("HEAD"), cancellable = true)
    private void onRenderTitleAndSubtitle(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof ClickGuiScreen) ci.cancel();
    }

    @Inject(method = "renderChat", at = @At("HEAD"), cancellable = true)
    private void onRenderChat(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof ClickGuiScreen) ci.cancel();
    }

    @Inject(method = "renderTabList", at = @At("HEAD"), cancellable = true)
    private void onRenderPlayerList(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof ClickGuiScreen) ci.cancel();
    }

    @Inject(method = "renderSubtitleOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderSubtitlesHud(GuiGraphics context, boolean defer, CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof ClickGuiScreen) ci.cancel();
    }

    @Inject(method = "renderSavingIndicator", at = @At("HEAD"), cancellable = true)
    private void onRenderAutosaveIndicator(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof ClickGuiScreen) ci.cancel();
    }

    @Inject(method = "renderSleepOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderSleepOverlay(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof ClickGuiScreen) ci.cancel();
    }

}