package silversword.axiom.client.modules.moduleutils.SmartKillAura;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import java.util.List;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class TargetManager {

    public LivingEntity selectTarget(PlayerEntity player, World world, String priorityMode, boolean ignoreBots, String targetMode) {
        List<LivingEntity> candidates = world.getEntitiesByClass(
                LivingEntity.class,
                player.getBoundingBox().expand(6.0),
                e -> e != player && e.isAlive() && isValidTarget(e, ignoreBots, targetMode)
        );

        if (candidates.isEmpty()) return null;

        return candidates.stream()
                .max((e1, e2) -> Double.compare(score(e1, priorityMode), score(e2, priorityMode)))
                .orElse(null);
    }

    private boolean isValidTarget(LivingEntity entity, boolean ignoreBots, String targetMode) {
        boolean isPlayer = entity instanceof PlayerEntity;

        return switch (targetMode) {
            case "Players" -> isPlayer && (!ignoreBots || !isBot((PlayerEntity) entity));
            case "Mobs" -> !isPlayer;
            case "Both" -> isPlayer ? (!ignoreBots || !isBot((PlayerEntity) entity)) : true;
            default -> false;
        };
    }

    private boolean isBot(PlayerEntity player) {
        return mc.getNetworkHandler().getPlayerList().stream()
                .noneMatch(entry -> entry.getProfile().id().equals(player.getUuid()));
    }

    private double score(LivingEntity entity, String mode) {
        double distance = mc.player.distanceTo(entity);
        float health = entity.getHealth();
        float armor = entity.getArmor();

        return switch (mode) {
            case "Health" -> 100.0 - health;
            case "Distance" -> 100.0 - distance;
            case "Armor" -> 100.0 - armor;
            case "Hybrid" -> (100.0 - health) * 2 + (100.0 - distance) + (100.0 - armor);
            default -> 100.0 - distance;
        };
    }

    public void reset() {}
}