package silversword.axiom.client.render.rendersystem.utils;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import net.minecraft.client.renderer.SubmitNodeCollector.CustomGeometryRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector.ParticleGroupRenderer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.mixin.client.SubmitNodeStorageAccessor;

import java.util.List;

public class OutlineRenderCommandQueue extends SubmitNodeStorage {
    private int color;
    private int[] tints;

    public void setColor(Color color) {
        this.color = color.getPacked();
    }

    @Override
    public SubmitNodeCollection order(int i) {
        Int2ObjectAVLTreeMap<SubmitNodeCollection> map = ((SubmitNodeStorageAccessor) this).axiom$getSubmitsPerOrder();
        return map.computeIfAbsent(i, order -> new OutlineBatchingRenderCommandQueue(this));
    }

    private class OutlineBatchingRenderCommandQueue extends SubmitNodeCollection {
        public OutlineBatchingRenderCommandQueue(SubmitNodeStorage orderedQueueImpl) {
            super(orderedQueueImpl);
        }

        @Override
        public void submitShadow(PoseStack matrices, float shadowRadius, List<EntityRenderState.ShadowPiece> shadowPieces) {
        }

        @Override
        public void submitNameTag(PoseStack matrices, @Nullable Vec3 nameLabelPos, int y, Component label, boolean notSneaking, int light, double squaredDistanceToCamera, CameraRenderState cameraState) {
        }

        @Override
        public void submitText(PoseStack matrices, float x, float y, FormattedCharSequence text, boolean dropShadow, Font.DisplayMode layerType, int light, int color, int backgroundColor, int outlineColor) {
        }

        @Override
        public void submitFlame(PoseStack matrices, EntityRenderState renderState, Quaternionf rotation) {
        }

        @Override
        public void submitLeash(PoseStack matrices, EntityRenderState.LeashState leashData) {
        }

        @Override
        public <S> void submitModel(Model<? super S> model, S state, PoseStack matrices, RenderType renderLayer, int light, int overlay, int tintedColor, @Nullable TextureAtlasSprite sprite, int outlineColor, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
            super.submitModel(model, state, matrices, renderLayer, light, overlay, color, sprite, 0, crumblingOverlay);
        }

        @Override
        public void submitModelPart(ModelPart part, PoseStack matrices, RenderType renderLayer, int light, int overlay, @Nullable TextureAtlasSprite sprite, boolean sheeted, boolean hasGlint, int tintedColor, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, int i) {
            super.submitModelPart(part, matrices, renderLayer, light, overlay, sprite, sheeted, hasGlint, color, crumblingOverlay, i);
        }

        @Override
        public void submitBlock(PoseStack matrices, BlockState state, int light, int overlay, int outlineColor) {
        }

        @Override
        public void submitMovingBlock(PoseStack matrices, MovingBlockRenderState state) {
        }

        @Override
        public void submitBlockModel(PoseStack matrices, RenderType renderLayer, BlockStateModel model, float r, float g, float b, int light, int overlay, int outlineColor) {
            r = ((color >> 16) & 0xFF) / 255f;
            g = ((color >> 8) & 0xFF) / 255f;
            b = (color & 0xFF) / 255f;

            super.submitBlockModel(matrices, renderLayer, model, r, g, b, light, overlay, outlineColor);
        }

        @Override
        public void submitItem(PoseStack matrices, ItemDisplayContext displayContext, int light, int overlay, int outlineColors, int[] tintLayers, List<BakedQuad> quads, RenderType renderLayer, ItemStackRenderState.FoilType glintType) {
            if (tints == null || tints[0] != color) {
                tints = new int[] { color, color, color, color };
            }

            super.submitItem(matrices, displayContext, light, overlay, outlineColors, tints, quads, renderLayer, glintType);
        }

        @Override
        public void submitCustomGeometry(PoseStack matrices, RenderType renderLayer, CustomGeometryRenderer customRenderer) {
        }

        @Override
        public void submitParticleGroup(ParticleGroupRenderer customRenderer) {
        }
    }
}