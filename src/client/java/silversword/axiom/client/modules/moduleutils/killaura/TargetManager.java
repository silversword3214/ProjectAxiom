package silversword.axiom.client.modules.moduleutils.killaura;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import java.util.List;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class TargetManager {

    public LivingEntity selectTarget(Player player, Level world, String priorityMode, boolean ignoreBots, String targetMode) {
        List<LivingEntity> candidates = world.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(6.0),
                e -> e != player && e.isAlive() && isValidTarget(e, ignoreBots, targetMode)
        );

        if (candidates.isEmpty()) return null;

        return candidates.stream()
                .max((e1, e2) -> Double.compare(score(e1, priorityMode), score(e2, priorityMode)))
                .orElse(null);
    }

    private boolean isValidTarget(LivingEntity entity, boolean ignoreBots, String targetMode) {
        boolean isPlayer = entity instanceof Player;

        return switch (targetMode) {
            case "Players" -> isPlayer && (!ignoreBots || !isBot((Player) entity));
            case "Mobs" -> !isPlayer;
            case "Both" -> isPlayer ? (!ignoreBots || !isBot((Player) entity)) : true;
            default -> false;
        };
    }

    private boolean isBot(Player player) {
        return mc.getConnection().getOnlinePlayers().stream()
                .noneMatch(entry -> entry.getProfile().id().equals(player.getUUID()));
    }

    private double score(LivingEntity entity, String mode) {
        double distance = mc.player.distanceTo(entity);
        float health = entity.getHealth();
        float armor = entity.getArmorValue();

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