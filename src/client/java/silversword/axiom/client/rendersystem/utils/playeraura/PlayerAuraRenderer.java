package silversword.axiom.client.rendersystem.utils.playeraura;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.rendersystem.axiomrenderer.api.event.Render3DEvent;
import silversword.axiom.client.modules.render.PlayerAura;
import silversword.axiom.client.rendersystem.utils.color.Color;

import java.util.HashMap;
import java.util.Map;
public final class PlayerAuraRenderer {

    private static final int SEGMENTS = 128;

    private static final int MAX_RINGS = 6;

    private static final Map<Integer, Long> entityTimeOffsets = new HashMap<>();

    private PlayerAuraRenderer() {}

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

            double x = Mth.lerp(event.getTickDelta(), e.xOld, e.getX());
            double y = Mth.lerp(event.getTickDelta(), e.yOld, e.getY())
                    + le.getBbHeight() * 0.5;
            double z = Mth.lerp(event.getTickDelta(), e.zOld, e.getZ());

            double distSq = camPos.distanceToSqr(x, y, z);
            if (distSq > maxDistSq) continue;

            float distFade = (float) (1.0 - Math.min(1.0, Math.sqrt(distSq) / maxDist));
            if (distFade < 0.05f) continue;

            float hpPct = le.getHealth() / Math.max(1f, le.getMaxHealth());
            int baseColor = module.resolveColor(le, hpPct, now);

            Long off = entityTimeOffsets.get(le.getId());
            if (off == null) {
                off = (long) (Math.random() * 100000);
                entityTimeOffsets.put(le.getId(), off);
            }
            long t = now + off;

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


    private static void drawRing(Render3DEvent event,
                                 double cx, double cy, double cz,
                                 double radius, int ringIndex, int ringCount,
                                 long time, Vec3 camPos,
                                 int baseColor, float pulse, float distFade,
                                 PlayerAura module) {

        double speed = module.getRotationSpeed() * (1.0 + ringIndex * 0.18);
        double timeSec = time / 1000.0;

        double yaw = timeSec * speed + ringIndex * (Math.PI / ringCount);
        double tilt = Math.PI * 0.5 * (ringIndex / (double) Math.max(1, ringCount - 1));
        if (ringCount == 1) tilt = Math.PI * 0.35;

        double tiltPhase = Math.sin(timeSec * 0.7 + ringIndex) * 0.15;
        tilt += tiltPhase;

        double cosT = Math.cos(tilt), sinT = Math.sin(tilt);
        double cosY = Math.cos(yaw), sinY = Math.sin(yaw);

        for (int i = 0; i < SEGMENTS; i++) {
            double a1 = (i / (double) SEGMENTS) * Math.PI * 2;
            double a2 = ((i + 1) / (double) SEGMENTS) * Math.PI * 2;

            double[] p1 = ringPoint(cx, cy, cz, radius, a1, cosT, sinT, cosY, sinY);
            double[] p2 = ringPoint(cx, cy, cz, radius, a2, cosT, sinT, cosY, sinY);

            double mx = (p1[0] + p2[0]) * 0.5;
            double my = (p1[1] + p2[1]) * 0.5;
            double mz = (p1[2] + p2[2]) * 0.5;

            double vx = camPos.x - mx;
            double vy = camPos.y - my;
            double vz = camPos.z - mz;
            double vLen = Math.sqrt(vx * vx + vy * vy + vz * vz);
            if (vLen < 1e-4) continue;
            vx /= vLen; vy /= vLen; vz /= vLen;

            double dx = p2[0] - p1[0];
            double dy = p2[1] - p1[1];
            double dz = p2[2] - p1[2];
            double dLen = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dLen < 1e-4) continue;
            dx /= dLen; dy /= dLen; dz /= dLen;

            double dot = Math.abs(dx * vx + dy * vy + dz * vz);
            double fresnel = Math.pow(1.0 - dot, module.getFresnelPower());

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

    private static double[] ringPoint(double cx, double cy, double cz,
                                      double r, double angle,
                                      double cosT, double sinT,
                                      double cosY, double sinY) {
        double lx = Math.cos(angle) * r;
        double ly = Math.sin(angle) * r;
        double lz = 0;

        double y1 = ly * cosT - lz * sinT;
        double z1 = ly * sinT + lz * cosT;
        double x1 = lx;

        double x2 = x1 * cosY + z1 * sinY;
        double z2 = -x1 * sinY + z1 * cosY;
        double y2 = y1;

        return new double[]{cx + x2, cy + y2, cz + z2};
    }
}