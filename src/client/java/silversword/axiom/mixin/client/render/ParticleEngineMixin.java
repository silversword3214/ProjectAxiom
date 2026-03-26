package silversword.axiom.mixin.client.render;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.NoParticleModule;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {

    @Inject(method = "makeParticle", at = @At("HEAD"), cancellable = true)
    private void axiom$filterParticle(ParticleOptions type, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, CallbackInfoReturnable<Particle> cir) {
        NoParticleModule mod = ModuleManager.getInstance().getModule(NoParticleModule.class);
        if (mod == null || !mod.isEnabled()) return;

        Identifier id = BuiltInRegistries.PARTICLE_TYPE.getKey(type.getType());
        if (id != null && mod.isParticleDisabled(id)) {
            cir.setReturnValue(null);
        }
    }
}