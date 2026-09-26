package silversword.axiom.mixin.client.render;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.NoFog;

@Mixin(FogRenderer.class)
public class FogRendererMixin {

    @ModifyReturnValue(
            method = "setupFog",
            at = @At("RETURN")
    )
    private FogData axiom$noFog(FogData original,
                                Camera camera,
                                int renderDistanceInChunks,
                                DeltaTracker deltaTracker,
                                float darkenWorldAmount,
                                ClientLevel level) {

        NoFog noFog = ModuleManager.getInstance().getModule(NoFog.class);
        if (noFog == null || !noFog.isEnabled()) return original;

        FogType type = camera.getFluidInCamera();
        boolean wipeEnvironmental = false;
        boolean wipeRenderDistance = false;

        // --- Neste- ja jauhosumu: poistetaan environmental ---
        if (noFog.disableWaterFog.get() && type == FogType.WATER) {
            wipeEnvironmental = true;
        }
        if (noFog.disableLavaFog.get() && type == FogType.LAVA) {
            wipeEnvironmental = true;
        }
        if (noFog.disablePowderSnowFog.get() && type == FogType.POWDER_SNOW) {
            wipeEnvironmental = true;
        }

        // --- Atmospheric: poistetaan sekä environmental (tiheä haze)
        //     että render-distance-fade PALKIKOILLE (mutta EI taivaalle).
        if (noFog.disableAtmosphericFog.get() && type == FogType.NONE) {
            wipeEnvironmental = true;
            wipeRenderDistance = true;
        }

        // Nesteissä: poistetaan vain environmental, jätetään
        // render-distance-fade palikoille, jotta vedenalainen
        // horisontti ei muutu teräväksi.
        if (type == FogType.WATER || type == FogType.LAVA) {
            wipeRenderDistance = false;
        }

        if (wipeEnvironmental) {
            original.environmentalStart = Float.MAX_VALUE;
            original.environmentalEnd   = Float.MAX_VALUE;
        }

        if (wipeRenderDistance) {
            // Poistaa chunkkien reunan faden PALKIKOILLE.
            // HUOM: emme koske skyEnd / cloudEnd / color -kenttiin,
            // joten taivas fadeaa edelleen horisontissa → ei mustaa aukkoa.
            original.renderDistanceStart = Float.MAX_VALUE;
            original.renderDistanceEnd   = Float.MAX_VALUE;
        }

        return original;
    }
}