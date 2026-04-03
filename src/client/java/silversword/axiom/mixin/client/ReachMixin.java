package silversword.axiom.mixin.client;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silversword.axiom.client.modules.combat.Reach;
import silversword.axiom.client.managers.ModuleManager;
import net.minecraft.client.Minecraft;

@Mixin(MultiPlayerGameMode.class)
public class ReachMixin {

    private double lastReachValue = -1;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Reach reach = ModuleManager.getInstance().getModule(Reach.class);

        var blockAttr = mc.player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        var entityAttr = mc.player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);

        if (blockAttr == null || entityAttr == null) return;

        if (reach != null && reach.isEnabled()) {
            double targetValue = reach.getReachDistance();

            // Jos päällä, asetetaan modin arvo molempiin
            if (lastReachValue != targetValue) {
                blockAttr.setBaseValue(targetValue);
                entityAttr.setBaseValue(targetValue);
                lastReachValue = targetValue;
            }
        } else {
            // JOS POIS PÄÄLTÄ: Palautetaan kumpikin omaan alkuperäiseen oletukseensa
            // Survival: Block = 4.5, Entity = 3.0
            // Creative: Block = 5.0, Entity = 5.0
            double defaultBlock = blockAttr.getAttribute().value().getDefaultValue();
            double defaultEntity = entityAttr.getAttribute().value().getDefaultValue();

            if (blockAttr.getBaseValue() != defaultBlock || entityAttr.getBaseValue() != defaultEntity) {
                blockAttr.setBaseValue(defaultBlock);
                entityAttr.setBaseValue(defaultEntity);
                lastReachValue = -1; // Resetoidaan muisti
            }
        }
    }
}
