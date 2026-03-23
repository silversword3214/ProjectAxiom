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
import silversword.axiom.client.gui.screen.ClickGuiScreen;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.misc.PotionEffects;
import silversword.axiom.client.modules.render.NoVignette;
import silversword.axiom.client.render.rendersystem.axiomrenderer.RenderAPI;
import silversword.axiom.client.render.rendersystem.axiomrenderer.core.RenderCore;

import static silversword.axiom.client.main.AxiomInitialize.mc;

@Mixin(Gui.class)
public class GuiMixin {

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

    // Note: The chest steal button drawing has been removed from this mixin.
    // It is now handled by the ChestStealer module via RenderHUDEvent.
}