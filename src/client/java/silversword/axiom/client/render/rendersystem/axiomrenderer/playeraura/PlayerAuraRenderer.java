package silversword.axiom.client.render.rendersystem.axiomrenderer.playeraura;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.modules.render.PlayerAura;
import silversword.axiom.client.render.rendersystem.utils.color.Color;

import java.util.HashMap;
import java.util.Map;

/**
 * Player Aura -renderöijä. Piirtää jokaisen kohteen ympärille 3-6 rengasta
 * jotka pyörivät eri akseleilla. Jokainen rengassegmentti saa alphansa
 * fresnel-kaavalla → reunat hehkuvat, keskiosa himmenee.
 */
public final class PlayerAuraRenderer {

    /** Segmenttejä per rengas. 128 = sileä, 96 = OK, 64 = nopea. */
    private static final int SEGMENTS = 128;

    /** Maksimi renkaiden määrä (per pelaaja). */
    private static final int MAX_RINGS = 6;

    /** Per-entity rotaatio-offset jotta eri pelaajien renkaat eivät ole synkassa. */
    private static final Map<Integer, Long> entityTimeOffsets = new HashMap<>();

    private PlayerAuraRenderer() {}

    // ═══════════════════════════════════════════════════════════════
    //  RENDER — kutsutaan LevelRendererMixin:stä render3D-eventillä
    // ═══════════════════════════════════════════════════════════════

    public static void render(Render3DEvent event, PlayerAura module) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Vec3 camPos = event.getCameraPos();
        double maxDist = module.getRenderDistance();
        double maxDistSq = maxDist * maxDist;

        long now = System.currentTimeMillis();

        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity le)) continue;
            if (!le.isAlive()) continue;
            if (!module.isTarget(le)) continue;

            // Interpoloitu positio
            double x = Mth.lerp(event.getTickDelta(), e.xOld, e.getX());
            double y = Mth.lerp(event.getTickDelta(), e.yOld, e.getY())
                    + le.getBbHeight() * 0.5;
            double z = Mth.lerp(event.getTickDelta(), e.zOld, e.getZ());

            double distSq = camPos.distanceToSqr(x, y, z);
            if (distSq > maxDistSq) continue;

            // Etäisyys-fade — himmenee kaukana
            float distFade = (float) (1.0 - Math.min(1.0, Math.sqrt(distSq) / maxDist));
            if (distFade < 0.05f) continue;

            // HP-pohjainen sävy tai kiinteä väri
            float hpPct = le.getHealth() / Math.max(1f, le.getMaxHealth());
            int baseColor = module.resolveColor(le, hpPct, now);

            // Per-entity time offset → ei synkronointia
            Long off = entityTimeOffsets.get(le.getId());
            if (off == null) {
                off = (long) (Math.random() * 100000);
                entityTimeOffsets.put(le.getId(), off);
            }
            long t = now + off;

            // Koko + pulssi
            double baseRadius = module.getRadius();
            double radius = baseRadius * le.getBbHeight() * 0.75;
            float pulse = (float) (0.75 + 0.25 * Math.sin(t / 350.0));

            int ringCount = Math.max(1, Math.min(MAX_RINGS, module.getRingCount()));

            for (int ring = 0; ring < ringCount; ring++) {
                drawRing(event, x, y, z, radius, ring, ringCount,
                        t, camPos, baseColor, pulse, distFade, module);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  RING GENERATION
    // ═══════════════════════════════════════════════════════════════

    private static void drawRing(Render3DEvent event,
                                 double cx, double cy, double cz,
                                 double radius, int ringIndex, int ringCount,
                                 long time, Vec3 camPos,
                                 int baseColor, float pulse, float distFade,
                                 PlayerAura module) {

        // Rotaationopeus vaihtelee per rengas
        double speed = module.getRotationSpeed() * (1.0 + ringIndex * 0.18);
        double timeSec = time / 1000.0;

        // Kaksi rotaatiokulmaa: yaw ja tilt
        double yaw = timeSec * speed + ringIndex * (Math.PI / ringCount);
        double tilt = Math.PI * 0.5 * (ringIndex / (double) Math.max(1, ringCount - 1));
        if (ringCount == 1) tilt = Math.PI * 0.35;

        // Tilt-offset kierron mukaan
        double tiltPhase = Math.sin(timeSec * 0.7 + ringIndex) * 0.15;
        tilt += tiltPhase;

        // Renkaan 3D-kanta: eteenpäin menevä ympyrä, kierretty yaw + tilt
        // Alkeisympyrä XY-tasossa: (cos(a), sin(a), 0)
        // Sitten: rotate tilt ympäri X, rotate yaw ympäri Y
        double cosT = Math.cos(tilt), sinT = Math.sin(tilt);
        double cosY = Math.cos(yaw), sinY = Math.sin(yaw);

        for (int i = 0; i < SEGMENTS; i++) {
            double a1 = (i / (double) SEGMENTS) * Math.PI * 2;
            double a2 = ((i + 1) / (double) SEGMENTS) * Math.PI * 2;

            // Piste 1
            double[] p1 = ringPoint(cx, cy, cz, radius, a1, cosT, sinT, cosY, sinY);
            // Piste 2
            double[] p2 = ringPoint(cx, cy, cz, radius, a2, cosT, sinT, cosY, sinY);

            // Keskipiste
            double mx = (p1[0] + p2[0]) * 0.5;
            double my = (p1[1] + p2[1]) * 0.5;
            double mz = (p1[2] + p2[2]) * 0.5;

            // View direction: kamera → piste
            double vx = camPos.x - mx;
            double vy = camPos.y - my;
            double vz = camPos.z - mz;
            double vLen = Math.sqrt(vx * vx + vy * vy + vz * vz);
            if (vLen < 1e-4) continue;
            vx /= vLen; vy /= vLen; vz /= vLen;

            // Segmentin suunta
            double dx = p2[0] - p1[0];
            double dy = p2[1] - p1[1];
            double dz = p2[2] - p1[2];
            double dLen = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dLen < 1e-4) continue;
            dx /= dLen; dy /= dLen; dz /= dLen;

            // Fresnel: 1 kun segmentti on kohtisuorassa katsetta vasten (edge-on)
            //          0 kun segmentti on yhdensuuntainen katseen kanssa
            double dot = Math.abs(dx * vx + dy * vy + dz * vz);
            double fresnel = Math.pow(1.0 - dot, module.getFresnelPower());

            // Yhdistä alpha-kertoimet
            double alpha = fresnel * pulse * distFade;
            if (alpha < 0.03) continue;

            int a = (int) Math.min(255, Math.round(alpha * 255));
            int color = (a << 24) | (baseColor & 0x00FFFFFF);

            event.getRenderer().drawLine(
                    p1[0], p1[1], p1[2],
                    p2[0], p2[1], p2[2],
                    new Color(color)
            );
        }
    }

    /**
     * Laskee pisteen renkaalla. Alkeisympyrä XY-tasossa,
     * kierretty X-akselin ympäri (tilt) ja Y-akselin ympäri (yaw).
     */
    private static double[] ringPoint(double cx, double cy, double cz,
                                      double r, double angle,
                                      double cosT, double sinT,
                                      double cosY, double sinY) {
        // Perus: (cos(a), sin(a), 0)
        double lx = Math.cos(angle) * r;
        double ly = Math.sin(angle) * r;
        double lz = 0;

        // Rotate X (tilt):  y' = y*cosT - z*sinT, z' = y*sinT + z*cosT
        double y1 = ly * cosT - lz * sinT;
        double z1 = ly * sinT + lz * cosT;
        double x1 = lx;

        // Rotate Y (yaw):  x'' = x*cosY + z*sinY, z'' = -x*sinY + z*cosY
        double x2 = x1 * cosY + z1 * sinY;
        double z2 = -x1 * sinY + z1 * cosY;
        double y2 = y1;

        return new double[]{cx + x2, cy + y2, cz + z2};
    }
}