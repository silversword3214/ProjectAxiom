package silversword.axiom.mixin.client.render.xray;

import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.modules.render.XRay;

@Mixin(ModelBlockRenderer.class)
public class XRayModelBlockRendererMixin {

    /**
     * Aseta ThreadLocal jokaisen blockin kohdalla ennen kuin sen quadit emitoidaan.
     */
    @Inject(method = "tesselateBlock", at = @At("HEAD"))
    private void axiom$xray_setContext(
            BlockQuadOutput output, float x, float y, float z,
            BlockAndTintGetter level, BlockPos pos,
            BlockState blockState, BlockStateModel model, long seed,
            CallbackInfo ci) {

        if (!XRay.isXRayEnabled()) {
            XRay.setCurrentBlockHidden(false);
            return;
        }
        XRay.setCurrentBlockHidden(blockState != null && XRay.isXrayHidden(blockState));
    }

    /**
     * Interceptoi output.put(...) -kutsu putQuadWithTint:issä ja muokkaa
     * quad (layer -> TRANSLUCENT) + instance (alpha alas).
     */
    @Redirect(
            method = "putQuadWithTint",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/BlockQuadOutput;put(FFFLnet/minecraft/client/resources/model/geometry/BakedQuad;Lcom/mojang/blaze3d/vertex/QuadInstance;)V"
            )
    )
    private void axiom$xray_redirectPut(
            BlockQuadOutput output, float x, float y, float z,
            BakedQuad quad, QuadInstance instance) {

        if (!XRay.isCurrentBlockHidden()) {
            output.put(x, y, z, quad, instance);
            return;
        }

        float alpha = XRay.getHiddenAlpha();
        if (alpha <= 0.0f || alpha >= 1.0f) {
            output.put(x, y, z, quad, instance);
            return;
        }

        // ── 1) Alpha alas: multiplyColor sekoittaa olemassa olevan kanssa ──
        int alphaByte = Math.max(0, Math.min(255, Math.round(alpha * 255)));
        int colorMask = (alphaByte << 24) | 0x00FFFFFF;
        instance.multiplyColor(colorMask);

        // ── 2) Layer -> TRANSLUCENT: pakottaa blending-pipelineen ──
        BakedQuad.MaterialInfo old = quad.materialInfo();
        BakedQuad.MaterialInfo neu = new BakedQuad.MaterialInfo(
                old.sprite(),
                ChunkSectionLayer.TRANSLUCENT,
                old.itemRenderType(),
                old.itemGlintRenderType(),
                old.itemGlintSpecialRenderType(),
                old.tintIndex(),
                old.shadeDirectionOverride(),
                old.lightEmission()
        );

        BakedQuad modified = new BakedQuad(
                quad.position0(), quad.position1(), quad.position2(), quad.position3(),
                quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(),
                quad.direction(),
                neu
        );

        output.put(x, y, z, modified, instance);
    }
}