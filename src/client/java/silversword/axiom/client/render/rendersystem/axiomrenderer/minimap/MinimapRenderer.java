package silversword.axiom.client.render.rendersystem.axiomrenderer.minimap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MinimapRenderer {

    private static final Logger LOG = LoggerFactory.getLogger("Axiom/Minimap");

    public static final Identifier HUD_TEXTURE_ID =
            Identifier.fromNamespaceAndPath("projectaxiom", "minimap_hud");

    private static MinimapSurface surface;
    private static MinimapColumnCache columnCache;
    private static boolean registered = false;

    private static boolean enabled = false;
    private static boolean rendering = false;

    private static int updateHz = 20;
    private static int surfaceSize = 256;
    private static int pendingResize = -1;
    private static float baseViewRadius = 64f;
    private static float zoom = 1.0f;
    private static boolean rotateWithPlayer = true;
    private static boolean circular = true;
    private static boolean heightShading = true;

    private static long lastRenderMs = 0L;

    private static float borderThickness = 1.5f;
    private static int   borderColor     = 0xFFAAAAAA;

    // Mob
    private static boolean drawPlayers = true;
    private static boolean drawHostile = true;
    private static boolean drawPassive = true;
    private static boolean drawNeutral = true;
    private static boolean drawWater   = true;
    private static boolean drawBoss    = true;

    private static int playerColor  = 0xFF00FFC8;
    private static int hostileColor = 0xFFFF3232;
    private static int passiveColor = 0xFF32FF32;
    private static int neutralColor = 0xFFFFFF00;
    private static int waterColor   = 0xFF3296FF;
    private static int bossColor    = 0xFFC800C8;

    private static float dotScale = 1.0f;

    public static void init() {
        if (surface == null) {
            surface = new MinimapSurface(surfaceSize);
            registered = false;
        }
        if (columnCache == null) {
            columnCache = new MinimapColumnCache();
        }
    }

    private static void ensureRegistered() {
        if (registered) return;
        if (surface == null || surface.getTextureView() == null) return;

        Minecraft.getInstance().getTextureManager()
                .register(HUD_TEXTURE_ID, surface);
        registered = true;
        LOG.info("Minimap surface registered: {}", HUD_TEXTURE_ID);
    }

    public static void renderMinimapView() {
        if (!enabled || rendering) return;

        long now = System.currentTimeMillis();
        long minInterval = 1000L / Math.max(1, updateHz);
        if (now - lastRenderMs < minInterval) return;
        lastRenderMs = now;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        rendering = true;
        try {
            init();

            // Käsittele mahdollinen resize
            if (pendingResize > 0 && pendingResize != surfaceSize) {
                surfaceSize = pendingResize;
                pendingResize = -1;
                registered = false;
                if (surface != null) surface.close();
                surface = new MinimapSurface(surfaceSize);
            }

            ensureRegistered();

            fillSurface(mc, now);
            surface.uploadToGpu();

        } catch (Throwable t) {
            LOG.error("Minimap render failed", t);
        } finally {
            rendering = false;
        }
    }

    private static void fillSurface(Minecraft mc, long now) {
        LocalPlayer player = mc.player;
        Level level = mc.level;

        int size = surface.size();
        int[] pixels = surface.pixels();

        double px = player.getX();
        double pz = player.getZ();

        float viewRadius = baseViewRadius / Math.max(0.1f, zoom);

        float yawRad = (float) Math.toRadians(player.getYRot());
        float theta = rotateWithPlayer ? (float) (Math.PI - yawRad) : 0f;
        float cosT = (float) Math.cos(theta);
        float sinT = (float) Math.sin(theta);

        float halfSize = size / 2f;
        float pixelsPerBlock = Math.max(1.0f, halfSize / viewRadius);
        float invPixelsPerBlock = 1.0f / pixelsPerBlock;

        float radiusSq = halfSize * halfSize;

        // Tyhjennä puskuri — läpinäkyvä tausta (alpha=0 → näkyy HUD-taustan läpi)
        java.util.Arrays.fill(pixels, 0);

        // REVERSE-MAPPING: käydään läpi JOKAINEN pikseli, katsotaan mikä block
        // on sen kohdalla. Ei saumoja, ei puuttuvia pikseleitä.
        for (int py = 0; py < size; py++) {
            float localZ = (py - halfSize) * invPixelsPerBlock;

            for (int pxi = 0; pxi < size; pxi++) {
                float localX = (pxi - halfSize) * invPixelsPerBlock;

                // Pyöreä maski
                if (circular) {
                    float dx = pxi - halfSize;
                    float dy = py - halfSize;
                    if (dx * dx + dy * dy > radiusSq) continue;
                }

                // Muunna takaisin maailma-koordinaateiksi
                float relX = localX * cosT + localZ * sinT;
                float relZ = -localX * sinT + localZ * cosT;

                int wx = (int) Math.floor(px + relX);
                int wz = (int) Math.floor(pz + relZ);

                MinimapColumnCache.Column c = columnCache.get(mc, level, wx, wz, now);
                if ((c.rawColor >>> 24) == 0) continue;

                int color = heightShading ? c.shadedColor : c.rawColor;
                pixels[py * size + pxi] = color;
            }


        }

        drawEntities(mc, size, pixels, px, pz, cosT, sinT,
                halfSize, pixelsPerBlock, viewRadius);
    }

    // ─── Mobien piirto ─────────────────────────────────────────────────────

    private static void drawEntities(Minecraft mc, int size, int[] pixels,
                                     double px, double pz,
                                     float cosT, float sinT,
                                     float halfSize, float pixelsPerBlock,
                                     float viewRadius) {

        float viewRadiusSq = viewRadius * viewRadius;

        for (var entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player) continue;
            if (!entity.isAlive()) continue;

            silversword.axiom.client.modules.moduleutils.TargetGroup group =
                    silversword.axiom.client.modules.moduleutils.TargetGroup.getGroup(entity);

            if (!shouldDrawGroup(group)) continue;

            double dx = entity.getX() - px;
            double dz = entity.getZ() - pz;
            double distSq = dx * dx + dz * dz;
            if (distSq > viewRadiusSq) continue;

            // Muunna maailma-koordinaatit minimap-pikseleiksi
            float localX = (float) dx * cosT - (float) dz * sinT;
            float localZ = (float) dx * sinT + (float) dz * cosT;

            int mx = (int) (halfSize + localX * pixelsPerBlock);
            int my = (int) (halfSize + localZ * pixelsPerBlock);

            // Pyöreä maski
            if (circular) {
                int fdx = mx - (size / 2);
                int fdy = my - (size / 2);
                if (fdx * fdx + fdy * fdy > viewRadiusSq * pixelsPerBlock * pixelsPerBlock) continue;
            }

            // Rajaa minimapin sisään
            if (mx < 0 || my < 0 || mx >= size || my >= size) continue;

            int color = getGroupColor(group);

            // Dotin koko: skaalautuu pixelsPerBlockin mukaan, min 2 px
            int dotRadius = Math.max(2, Math.round(pixelsPerBlock * 0.35f * dotScale));

            MinimapSurface.fillCirclePixels(pixels, size, mx, my, dotRadius, color);
        }
    }

    private static boolean shouldDrawGroup(
            silversword.axiom.client.modules.moduleutils.TargetGroup group) {
        return switch (group) {
            case PLAYER  -> drawPlayers;
            case HOSTILE -> drawHostile;
            case PASSIVE -> drawPassive;
            case NEUTRAL -> drawNeutral;
            case WATER   -> drawWater;
            case BOSS    -> drawBoss;
            default      -> false;
        };
    }

    private static int getGroupColor(
            silversword.axiom.client.modules.moduleutils.TargetGroup group) {
        return switch (group) {
            case PLAYER  -> playerColor;
            case HOSTILE -> hostileColor;
            case PASSIVE -> passiveColor;
            case NEUTRAL -> neutralColor;
            case WATER   -> waterColor;
            case BOSS    -> bossColor;
            default      -> playerColor;
        };
    }

    public static void setBorderThickness(float t) {
        borderThickness = Math.max(0f, Math.min(6f, t));
    }

    public static void setBorderColor(int c) { borderColor = c; }
    public static float getBorderThickness() { return borderThickness; }
    public static int   getBorderColor()     { return borderColor; }
    public static MinimapSurface getSurface() { return surface; }
    public static void setEnabled(boolean v) { enabled = v; }
    public static boolean isEnabled() { return enabled; }
    public static void setUpdateHz(int hz) { updateHz = Math.max(1, Math.min(120, hz)); }
    public static void setRtSize(int s) { pendingResize = Math.max(64, Math.min(1024, s)); }
    public static void setViewRadius(float r) { baseViewRadius = Math.max(16f, Math.min(512f, r)); }
    public static void setZoom(float z) { zoom = Math.max(0.25f, Math.min(4.0f, z)); }
    public static void setRotateWithPlayer(boolean v) { rotateWithPlayer = v; }
    public static void setCircular(boolean v) { circular = v; }
    public static void setHeightShading(boolean v) { heightShading = v; }
    public static boolean isCircular() { return circular; }
    public static boolean isRotateWithPlayer() { return rotateWithPlayer; }
    public static float getZoom() { return zoom; }

    // Mob
    public static void setDrawPlayers(boolean v) { drawPlayers = v; }
    public static void setDrawHostile(boolean v) { drawHostile = v; }
    public static void setDrawPassive(boolean v) { drawPassive = v; }
    public static void setDrawNeutral(boolean v) { drawNeutral = v; }
    public static void setDrawWater(boolean v)   { drawWater = v; }
    public static void setDrawBoss(boolean v)    { drawBoss = v; }

    public static void setPlayerColor(int c)  { playerColor = c; }
    public static void setHostileColor(int c) { hostileColor = c; }
    public static void setPassiveColor(int c) { passiveColor = c; }
    public static void setNeutralColor(int c) { neutralColor = c; }
    public static void setWaterColor(int c)   { waterColor = c; }
    public static void setBossColor(int c)    { bossColor = c; }

    public static void setDotScale(float s) { dotScale = Math.max(0.5f, Math.min(3.0f, s)); }

    public static void onLevelChange() {
        if (columnCache != null) columnCache.invalidateAll();
    }
}