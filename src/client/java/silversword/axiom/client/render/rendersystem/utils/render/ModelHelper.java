package silversword.axiom.client.render.rendersystem.utils.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.jetbrains.annotations.NotNull;

public class ModelHelper {

    private static AvatarRenderState tempState = null;

    @SuppressWarnings("unchecked")
    public static PlayerModel getUpdatedModel(AbstractClientPlayer player, float partialTick) {
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        var renderer = dispatcher.getRenderer(player);

        if (renderer instanceof LivingEntityRenderer<?, ?, ?> livingRenderer) {

            if (livingRenderer.getModel() instanceof PlayerModel playerModel) {

                if (tempState == null) {
                    tempState = new AvatarRenderState();
                }

                ((LivingEntityRenderer<@NotNull AbstractClientPlayer, @NotNull AvatarRenderState, @NotNull PlayerModel>) livingRenderer)
                        .extractRenderState(player, tempState, partialTick);

                playerModel.setupAnim(tempState);

                return playerModel;
            }
        }
        return null;
    }
}