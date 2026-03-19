package silversword.axiom.client.modules.moduleutils;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.WaterCreatureEntity; // Oikea luokka kaloille
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.SquidEntity; // Mustekalat
import net.minecraft.entity.player.PlayerEntity;

public enum TargetGroup {
    PLAYER,
    HOSTILE,
    PASSIVE,
    NEUTRAL,
    WATER,
    BOSS;

    public static TargetGroup getGroup(Entity entity) {
        if (entity instanceof PlayerEntity) return PLAYER;

        // Bossit ja erikoisvastustajat
        EntityType<?> type = entity.getType();
        if (type == EntityType.ENDER_DRAGON || type == EntityType.WITHER ||
                type == EntityType.WARDEN || entity instanceof IronGolemEntity) {
            return BOSS;
        }

        // Vesieläimet (Kala, mustekalat, delfiinit)
        if (entity instanceof WaterCreatureEntity || entity instanceof SquidEntity) {
            return WATER;
        }

        // Hostile (Zombiet, Creeperit jne.)
        if (entity instanceof HostileEntity) {
            return HOSTILE;
        }

        // Passive (Lehmät, lampaat, lepakot)
        if (entity instanceof AnimalEntity || entity instanceof PassiveEntity || entity instanceof AmbientEntity) {
            // Jos haluat tarkemman jaon, voit tarkistaa onko kyseessä "Angerable" (esim. susi)
            return PASSIVE;
        }

        return NEUTRAL;
    }
}