package silversword.axiom.mixin.client;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.ShaderESP;
import silversword.axiom.client.mixininterface.IEntityRenderState;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {

    @Unique private ShaderESP shaderESP;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(EntityRendererProvider.Context context, CallbackInfo ci) {
        shaderESP = ModuleManager.getInstance().getModule(ShaderESP.class);
    }

    @Inject(method = "getNameTag", at = @At("HEAD"), cancellable = true)
    private void onRenderLabel(T entity, CallbackInfoReturnable<Component> cir) {
        // Tähän voi lisätä Nametags-moduulin käsittelyn myöhemmin
    }

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void shouldRender(T entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {

    }

    @Inject(method = "affectedByCulling", at = @At("HEAD"), cancellable = true)
    void canBeCulled(T entity, CallbackInfoReturnable<Boolean> cir) {
        if (shaderESP != null && shaderESP.forceRender()) {
            cir.setReturnValue(false);
        }
    }

    // Tallenna entity tilaan (IEntityRenderState)
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onUpdateRenderState(T entity, S state, float tickProgress, CallbackInfo ci) {
        ((IEntityRenderState) state).axiom$setEntity(entity);
    }

    // Glow-tila (jos haluat tukea myös Glow-tilaa)
    @Inject(method = "extractRenderState", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/state/EntityRenderState;outlineColor:I", shift = At.Shift.AFTER, opcode = Opcodes.PUTFIELD))
    private void onGetOutlineColor(T entity, S state, float tickProgress, CallbackInfo ci) {
        if (shaderESP != null && shaderESP.isGlow() && !shaderESP.shouldSkip(entity)) {
            var color = shaderESP.getColor(entity);
            if (color != null) {
                state.outlineColor = color.getPacked();
            }
        }
    }

    @Unique
    private boolean shouldDisableShadow(S renderState) {
        // Get the entity from your IEntityRenderState extension (if you stored it)
        Entity entity = ((IEntityRenderState) renderState).axiom$getEntity();
        return shaderESP != null && shaderESP.shouldSkip(entity);
    }
}