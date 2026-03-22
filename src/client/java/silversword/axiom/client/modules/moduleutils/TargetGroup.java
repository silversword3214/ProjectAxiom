package silversword.axiom.client.modules.moduleutils;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.animal.fish.WaterAnimal; // Oikea luokka kaloille
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.squid.Squid; // Mustekalat
import net.minecraft.world.entity.player.Player;

public enum TargetGroup {
    PLAYER,
    HOSTILE,
    PASSIVE,
    NEUTRAL,
    WATER,
    BOSS;

    public static TargetGroup getGroup(Entity entity) {
        if (entity instanceof Player) return PLAYER;

        // Bossit ja erikoisvastustajat
        EntityType<?> type = entity.getType();
        if (type == EntityType.ENDER_DRAGON || type == EntityType.WITHER ||
                type == EntityType.WARDEN || entity instanceof IronGolem) {
            return BOSS;
        }

        // Vesieläimet (Kala, mustekalat, delfiinit)
        if (entity instanceof WaterAnimal || entity instanceof Squid) {
            return WATER;
        }

        // Hostile (Zombiet, Creeperit jne.)
        if (entity instanceof Monster) {
            return HOSTILE;
        }

        // Passive (Lehmät, lampaat, lepakot)
        if (entity instanceof Animal || entity instanceof AgeableMob || entity instanceof AmbientCreature) {
            // Jos haluat tarkemman jaon, voit tarkistaa onko kyseessä "Angerable" (esim. susi)
            return PASSIVE;
        }

        return NEUTRAL;
    }
}