package silversword.axiom.client.modules.moduleutils;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Predicate;

import static silversword.axiom.client.main.AxiomInitialize.mc;

/**
 * Yleinen targetinhallinta moduuleille.
 * Tukee eri target-tyyppejä (pelaajat, mobit, molemmat) ja etäisyysrajoituksia.
 */
public class TargetManager {

    /**
     * Valitsee lähimmän kelvollisen kohteen.
     *
     * @param world      maailma
     * @param range      maksimietäisyys
     * @param targetMode "Players", "Mobs" tai "Both"
     * @return lähin kelvollinen kohde, tai null jos ei löydy
     */
    public static LivingEntity getClosest(Level world, double range, String targetMode) {
        return getClosest(world, range, targetMode, entity -> true);
    }

    /**
     * Valitsee lähimmän kelvollisen kohteen lisäehdolla.
     *
     * @param world      maailma
     * @param range      maksimietäisyys
     * @param targetMode "Players", "Mobs" tai "Both"
     * @param extraFilter ylimääräinen ehto (esim. ignoreBots)
     * @return lähin kelvollinen kohde, tai null jos ei löydy
     */
    public static LivingEntity getClosest(Level world, double range, String targetMode, Predicate<LivingEntity> extraFilter) {
        if (mc.player == null) return null;

        List<LivingEntity> entities = world.getEntitiesOfClass(
                LivingEntity.class,
                mc.player.getBoundingBox().inflate(range),
                e -> e != mc.player && e.isAlive() && isValidTarget(e, targetMode) && extraFilter.test(e)
        );

        if (entities.isEmpty()) return null;

        // Palautetaan lähin
        return entities.stream()
                .min((e1, e2) -> Double.compare(mc.player.distanceTo(e1), mc.player.distanceTo(e2)))
                .orElse(null);
    }

    /**
     * Tarkistaa, onko entiteetti kelvollinen target-modessa.
     */
    public static boolean isValidTarget(LivingEntity entity, String targetMode) {
        return switch (targetMode) {
            case "Players" -> entity instanceof Player;
            case "Mobs" -> !(entity instanceof Player);
            case "Both" -> true;
            default -> false;
        };
    }

    /**
     * Yksinkertainen bottientarkistus (vertaa tablistiin).
     */
    public static boolean isBot(Player player) {
        return mc.getConnection().getOnlinePlayers().stream()
                .noneMatch(entry -> entry.getProfile().id().equals(player.getUUID()));
    }
}