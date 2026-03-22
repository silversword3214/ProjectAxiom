package silversword.axiom.client.modules.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingNumber;
import silversword.axiom.client.sound.CustomSounds;

import java.util.Random;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class HitEffect extends AxiomMod {

    // Partikkelivaihtoehdot
    private static final String[] PARTICLE_OPTIONS = {
            "CRIT", "ENCHANTED_HIT", "DAMAGE_INDICATOR", "SWEEP_ATTACK",
            "HEART", "ANGRY_VILLAGER", "HAPPY_VILLAGER", "FIREWORK",
            "FLAME", "LAVA", "SMOKE", "LARGE_SMOKE", "CLOUD", "POOF",
            "SPIT", "SQUID_INK", "GLOW", "WITCH", "TOTEM_OF_UNDYING"
    };

    // Äänivaihtoehdot
    private static final String[] SOUND_OPTIONS = {
            "ENTITY_PLAYER_ATTACK_CRIT", "ENTITY_PLAYER_ATTACK_STRONG",
            "ENTITY_PLAYER_ATTACK_SWEEP", "ENTITY_PLAYER_ATTACK_WEAK",
            "ENTITY_PLAYER_ATTACK_KNOCKBACK", "ENTITY_EXPERIENCE_ORB_PICKUP",
            "ENTITY_ARROW_HIT_PLAYER", "ENTITY_PLAYER_HURT",
            "ENTITY_GENERIC_EXPLODE", "BLOCK_ANVIL_LAND",
            "ENTITY_ENDER_DRAGON_HURT", "ENTITY_WITHER_HURT",
            "ENTITY_ZOMBIE_HURT", "ENTITY_PLAYER_LEVELUP", "OOF"
    };

    // --- Asetukset ---
    private final SettingMode particleType = new SettingMode("Particle Type", PARTICLE_OPTIONS, "CRIT");
    private final SettingNumber particleCount = new SettingNumber("Particle Count", 1, 20, 1, 5);
    private final SettingNumber particleSpeed = new SettingNumber("Particle Speed", 0.0, 2.0, 0.1, 0.2);
    private final SettingNumber particleSpread = new SettingNumber("Particle Spread", 0.0, 1.0, 0.1, 0.3);
    private final SettingBoolean particleEnabled = new SettingBoolean("Enable Particles", true);

    private final SettingMode soundType = new SettingMode("Sound Type", SOUND_OPTIONS, "ENTITY_PLAYER_ATTACK_CRIT");
    private final SettingNumber soundVolume = new SettingNumber("Sound Volume", 0.1, 2.0, 0.1, 0.5);
    private final SettingNumber soundPitch = new SettingNumber("Sound Pitch", 0.5, 2.0, 0.1, 1.0);
    private final SettingBoolean soundEnabled = new SettingBoolean("Enable Sound", true);

    private final SettingBoolean onlyPlayers = new SettingBoolean("Only Players", true);
    private final SettingBoolean onlyOwnHits = new SettingBoolean("Only Own Hits", true); // jos false, näyttää kaikkien pelaajien hitit (vaatii verkkopakettien kuuntelua, mutta yksinkertaisuuden vuoksi jätetään pois)

    // Tila
    private boolean wasSwinging = false;
    private final Random random = new Random();

    public HitEffect() {
        super("Hit Effect", "Visual and audio feedback when you hit an entity", ModuleCategory.COMBAT);
        addSetting(particleEnabled);
        addSetting(particleType);
        addSetting(particleCount);
        addSetting(particleSpeed);
        addSetting(particleSpread);
        addSetting(soundEnabled);
        addSetting(soundType);
        addSetting(soundVolume);
        addSetting(soundPitch);
        addSetting(onlyPlayers);
        // addSetting(onlyOwnHits); // jätetään pois yksinkertaisuuden vuoksi
    }

    @Override
    protected void onTick() {
        if (mc.player == null || mc.level == null) return;

        boolean isSwinging = mc.player.swinging;

        // Jos käsi heilahti juuri tällä tickillä
        if (isSwinging && !wasSwinging) {
            // Tarkistetaan, osuiko pelaaja johonkin
            HitResult hit = mc.hitResult;
            if (hit instanceof EntityHitResult entityHit) {
                Entity target = entityHit.getEntity();

                // Jos vain pelaajat ja target ei ole pelaaja, skip
                if (onlyPlayers.get() && !(target instanceof Player)) return;

                // Suoritetaan efekti
                triggerEffect(target);
            }
        }

        wasSwinging = isSwinging;
    }

    private void triggerEffect(Entity target) {
        if (particleEnabled.get()) {
            spawnParticles(target);
        }
        if (soundEnabled.get()) {
            playSound(target);
        }
    }

    private void spawnParticles(Entity target) {
        ParticleOptions particle = getParticleEffect(particleType.getMode());
        if (particle == null) return;

        int count = (int) particleCount.getValue();
        double speed = particleSpeed.getValue();
        double spread = particleSpread.getValue();

        Vec3 pos = target.position().add(0, target.getBbHeight() / 2, 0); // keskikohta

        for (int i = 0; i < count; i++) {
            double offsetX = (random.nextDouble() - 0.5) * spread;
            double offsetY = (random.nextDouble() - 0.5) * spread;
            double offsetZ = (random.nextDouble() - 0.5) * spread;
            double vx = (random.nextDouble() - 0.5) * speed;
            double vy = (random.nextDouble() - 0.5) * speed;
            double vz = (random.nextDouble() - 0.5) * speed;

            mc.level.addParticle(particle, pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ, vx, vy, vz);
        }
    }

    private void playSound(Entity target) {
        SoundEvent sound = getSoundEvent(soundType.getMode());
        if (sound == null) return;

        float volume = (float) soundVolume.getValue();
        float pitch = (float) soundPitch.getValue();

        mc.level.playSound(
                mc.player,
                target.getX(), target.getY(), target.getZ(),
                sound,
                SoundSource.PLAYERS,
                volume,
                pitch
        );
    }

    @Nullable
    private ParticleOptions getParticleEffect(String name) {
        // Yksinkertainen switch-case yleisimmille partikkeleille
        switch (name) {
            case "CRIT": return ParticleTypes.CRIT;
            case "ENCHANTED_HIT": return ParticleTypes.ENCHANTED_HIT;
            case "DAMAGE_INDICATOR": return ParticleTypes.DAMAGE_INDICATOR;
            case "SWEEP_ATTACK": return ParticleTypes.SWEEP_ATTACK;
            case "HEART": return ParticleTypes.HEART;
            case "ANGRY_VILLAGER": return ParticleTypes.ANGRY_VILLAGER;
            case "HAPPY_VILLAGER": return ParticleTypes.HAPPY_VILLAGER;
            case "FIREWORK": return ParticleTypes.FIREWORK;
            case "FLAME": return ParticleTypes.FLAME;
            case "LAVA": return ParticleTypes.LAVA;
            case "SMOKE": return ParticleTypes.SMOKE;
            case "LARGE_SMOKE": return ParticleTypes.LARGE_SMOKE;
            case "CLOUD": return ParticleTypes.CLOUD;
            case "POOF": return ParticleTypes.POOF;
            case "SPIT": return ParticleTypes.SPIT;
            case "SQUID_INK": return ParticleTypes.SQUID_INK;
            case "GLOW": return ParticleTypes.GLOW;
            case "WITCH": return ParticleTypes.WITCH;
            case "TOTEM_OF_UNDYING": return ParticleTypes.TOTEM_OF_UNDYING;
            default: return ParticleTypes.CRIT;
        }
    }

    @Nullable
    private SoundEvent getSoundEvent(String name) {
        switch (name) {
            case "ENTITY_PLAYER_ATTACK_CRIT": return SoundEvents.PLAYER_ATTACK_CRIT;
            case "ENTITY_PLAYER_ATTACK_STRONG": return SoundEvents.PLAYER_ATTACK_STRONG;
            case "ENTITY_PLAYER_ATTACK_SWEEP": return SoundEvents.PLAYER_ATTACK_SWEEP;
            case "ENTITY_PLAYER_ATTACK_WEAK": return SoundEvents.PLAYER_ATTACK_WEAK;
            case "ENTITY_PLAYER_ATTACK_KNOCKBACK": return SoundEvents.PLAYER_ATTACK_KNOCKBACK;
            case "ENTITY_EXPERIENCE_ORB_PICKUP": return SoundEvents.EXPERIENCE_ORB_PICKUP;
            case "ENTITY_ARROW_HIT_PLAYER": return SoundEvents.ARROW_HIT_PLAYER;
            case "ENTITY_PLAYER_HURT": return SoundEvents.PLAYER_HURT;
            case "BLOCK_ANVIL_LAND": return SoundEvents.ANVIL_LAND;
            case "ENTITY_ENDER_DRAGON_HURT": return SoundEvents.ENDER_DRAGON_HURT;
            case "ENTITY_WITHER_HURT": return SoundEvents.WITHER_HURT;
            case "ENTITY_ZOMBIE_HURT": return SoundEvents.ZOMBIE_HURT;
            case "ENTITY_PLAYER_LEVELUP": return SoundEvents.PLAYER_LEVELUP;
            case "OOF": return CustomSounds.OOF;
            default: return SoundEvents.PLAYER_ATTACK_CRIT;
        }
    }
}