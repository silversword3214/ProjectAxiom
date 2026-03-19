package silversword.axiom.mixin.client.block;

import net.minecraft.block.PowderSnowBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.movement.LeatherBoots;

@Mixin(PowderSnowBlock.class)
public class PowderSnowBlockMixin {

    @ModifyReturnValue(method = "canWalkOnPowderSnow", at = @At("RETURN"))
    private static boolean onCanWalkOnPowderSnow(boolean original, Entity entity) {
        // Jos moduuli on päällä ja entity on pelaaja, palautetaan true
        if (entity instanceof PlayerEntity) {
            LeatherBoots mod = ModuleManager.getInstance().getModule(LeatherBoots.class);
            if (mod != null && mod.isEnabled()) {
                return true;
            }
        }
        return original;
    }
}