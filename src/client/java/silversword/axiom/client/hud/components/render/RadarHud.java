package silversword.axiom.client.hud.components.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.hud.BaseHudElement;
import silversword.axiom.client.hud.core.HudContext;
import silversword.axiom.client.modules.moduleutils.TargetGroup;
import silversword.axiom.client.modules.render.RadarModule;
import silversword.axiom.client.render.rendersystem.Renderer2D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public final class RadarHud extends BaseHudElement {
    private final RadarModule module;
    private int size = 120;
    private double renderDistance = 64;
    private double dotSize = 2.0;
    private float radarScale = 1.0f;
    private float textScale = 1.0f;
    private float dotScale = 1.0f;

    // Suodatus
    private boolean drawPlayers = true;
    private boolean drawHostile = true;
    private boolean drawPassive = true;
    private boolean drawNeutral = true;
    private boolean drawWater = true;
    private boolean drawBoss = true;

    // Muoto
    private String radarShape = "SQUARE";
    private boolean showEntityCircles = false;
    private double entityCircleSize = 8.0;

    // Korkeusindikaattori
    private String heightIndicator = "OFF";
    private double heightRange = 32;
    private int aboveColor = 0xFFFF6464;
    private int belowColor = 0xFF6464FF;
    private int sameLevelColor = 0xFFFFFFFF;

    // Kompassi
    private boolean showCompass = true; // UUSI

    // Värit ryhmille
    private int playerColor = 0xFF00FFC8;
    private int hostileColor = 0xFFFF3232;
    private int passiveColor = 0xFF32FF32;
    private int neutralColor = 0xFFFFFF00;
    private int waterColor = 0xFF3296FF;
    private int bossColor = 0xFFC800C8;

    // Kiinteät arvot
    private static final int BASE_PADDING = 4;
    private static final int BASE_CORNER_RADIUS = 4;
    private static final int BASE_LINE_COLOR = 0x40FFFFFF;
    private static final int BASE_BORDER_COLOR = 0xFFAAAAAA;

    public RadarHud(RadarModule module) {
        super("Radar", 10, 10);
        this.module = module;
    }

    // Setterit
    public void setSize(int size) { this.size = size; }
    public void setRenderDistance(double d) { renderDistance = d; }
    public void setDotSize(double d) { dotSize = d; }
    public void setRadarScale(float s) { radarScale = Math.max(0.5f, Math.min(2.0f, s)); }
    public void setTextScale(float s) { textScale = Math.max(0.5f, Math.min(2.0f, s)); }
    public void setDotScale(float s) { dotScale = Math.max(0.5f, Math.min(2.0f, s)); }
    public void setDrawPlayers(boolean b) { drawPlayers = b; }
    public void setDrawHostile(boolean b) { drawHostile = b; }
    public void setDrawPassive(boolean b) { drawPassive = b; }
    public void setDrawNeutral(boolean b) { drawNeutral = b; }
    public void setDrawWater(boolean b) { drawWater = b; }
    public void setDrawBoss(boolean b) { drawBoss = b; }
    public void setRadarShape(String s) { radarShape = s; }
    public void setShowEntityCircles(boolean b) { showEntityCircles = b; }
    public void setEntityCircleSize(double d) { entityCircleSize = Math.max(1, d); }
    public void setHeightIndicator(String s) { heightIndicator = s; }
    public void setHeightRange(double d) { heightRange = Math.max(1, d); }
    public void setAboveColor(int c) { aboveColor = c; }
    public void setBelowColor(int c) { belowColor = c; }
    public void setSameLevelColor(int c) { sameLevelColor = c; }
    public void setShowCompass(boolean b) { showCompass = b; } // UUSI
    public void setPlayerColor(int c) { playerColor = c; }
    public void setHostileColor(int c) { hostileColor = c; }
    public void setPassiveColor(int c) { passiveColor = c; }
    public void setNeutralColor(int c) { neutralColor = c; }
    public void setWaterColor(int c) { waterColor = c; }
    public void setBossColor(int c) { bossColor = c; }

    @Override public boolean isModuleControlled() { return true; }
    @Override public int width(Minecraft mc) { return (int) (size * radarScale); }
    @Override public int height(Minecraft mc) { return (int) (size * radarScale); }

    @Override
    public void render(HudContext ctx, DeltaTracker tickCounter) {
        if (mc.player == null || mc.level == null) return;

        float scaledSize = size * radarScale;
        int centerX = (int) (x + scaledSize / 2);
        int centerY = (int) (y + scaledSize / 2);
        double scale = (scaledSize / 2.0) / renderDistance;
        int radius = (int) (scaledSize / 2);

        // Piirrä tausta ja reunus
        Color bgColor = new Color(0x80000000);
        Color borderCol = new Color(BASE_BORDER_COLOR);
        if ("CIRCLE".equals(radarShape)) {
            Renderer2D.COLOR.drawCircle(centerX, centerY, radius, bgColor);
            Renderer2D.COLOR.drawCircleOutline(centerX, centerY, radius, borderCol, 1.0);
        } else {
            Renderer2D.COLOR.drawRoundedRect(x, y, scaledSize, scaledSize, BASE_CORNER_RADIUS, bgColor);
            Renderer2D.COLOR.drawRoundedRectOutline(x, y, scaledSize, scaledSize, BASE_CORNER_RADIUS, borderCol, 1.0);
        }

        // Ristikko
        Color axisCol = new Color(BASE_LINE_COLOR);
        Renderer2D.COLOR.line(x, centerY, x + scaledSize, centerY, axisCol);
        Renderer2D.COLOR.line(centerX, y, centerX, y + scaledSize, axisCol);

        // Suuntavektorit
        Vec3 playerPos = mc.player.position();
        Vec3 forward = mc.player.getViewVector(1.0f);
        forward = new Vec3(forward.x, 0, forward.z).normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();

        // Kompassi (vain jos päällä)
        if (showCompass) {
            drawCompass(ctx, centerX, centerY, radius, textScale, forward, right);
        }

        // Entiteetit
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || !entity.isAlive()) continue;
            TargetGroup group = TargetGroup.getGroup(entity);
            if (!shouldDrawGroup(group)) continue;

            double dx = entity.getX() - playerPos.x;
            double dz = entity.getZ() - playerPos.z;
            double dy = entity.getY() - playerPos.y;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > renderDistance) continue;

            double relForward = dx * forward.x + dz * forward.z;
            double relRight = dx * right.x + dz * right.z;

            int dotX = centerX + (int) (relRight * scale);
            int dotY = centerY - (int) (relForward * scale);

            // Ympyrän leikkaus
            if ("CIRCLE".equals(radarShape)) {
                double dxCenter = dotX - centerX;
                double dyCenter = dotY - centerY;
                if (dxCenter * dxCenter + dyCenter * dyCenter > radius * radius) {
                    continue;
                }
            }

            int baseColor = getGroupColor(group);
            float alpha = 1.0f;
            if ("OPACITY".equals(heightIndicator) && heightRange > 0) {
                float factor = (float) Math.min(1.0, Math.abs(dy) / heightRange);
                alpha = 1.0f - factor * 0.7f; // tummenee korkeuden mukaan
            }
            int finalColor = applyAlpha(baseColor, alpha);

            // Piirrä ympyrä entiteetin ympärille (vain fill, ei outlinea)
            if (showEntityCircles) {
                double circleRad = entityCircleSize * dotScale;
                int fillColor = (finalColor & 0x00FFFFFF) | (0x80 << 24); // 50% läpinäkyvä
                Renderer2D.COLOR.drawCircle(dotX, dotY, circleRad, new Color(fillColor));
            }

            // Piirrä itse piste
            int finalDotSize = (int) (dotSize * dotScale);
            Renderer2D.COLOR.quad(dotX - finalDotSize/2, dotY - finalDotSize/2, finalDotSize, finalDotSize, new Color(finalColor));

            // LINE-indikaattori
            if ("LINE".equals(heightIndicator) && dy != 0) {
                int lineLength = (int) Math.min(20, Math.abs(dy) / heightRange * 20);
                if (dy > 0) {
                    Renderer2D.COLOR.line(dotX, dotY - finalDotSize/2, dotX, dotY - finalDotSize/2 - lineLength, new Color(aboveColor));
                } else {
                    Renderer2D.COLOR.line(dotX, dotY + finalDotSize/2, dotX, dotY + finalDotSize/2 + lineLength, new Color(belowColor));
                }
            }
        }
    }

    private void drawCompass(HudContext ctx, int centerX, int centerY, int radius, float textScale, Vec3 forward, Vec3 right) {
        Vec3 north = new Vec3(0, 0, -1);
        Vec3 south = new Vec3(0, 0, 1);
        Vec3 west = new Vec3(-1, 0, 0);
        Vec3 east = new Vec3(1, 0, 0);

        int offset = (int) (10 * textScale);
        int r = radius - offset;

        int northX = centerX + (int) (north.dot(right) * r);
        int northY = centerY - (int) (north.dot(forward) * r);
        int southX = centerX + (int) (south.dot(right) * r);
        int southY = centerY - (int) (south.dot(forward) * r);
        int westX = centerX + (int) (west.dot(right) * r);
        int westY = centerY - (int) (west.dot(forward) * r);
        int eastX = centerX + (int) (east.dot(right) * r);
        int eastY = centerY - (int) (east.dot(forward) * r);

        int color = BASE_BORDER_COLOR;
        ctx.drawScaledText("N", northX - (int) (ctx.textWidth("N") * textScale / 2), northY - (int) (ctx.fontHeight() * textScale / 2), color, true, textScale);
        ctx.drawScaledText("S", southX - (int) (ctx.textWidth("S") * textScale / 2), southY - (int) (ctx.fontHeight() * textScale / 2), color, true, textScale);
        ctx.drawScaledText("W", westX - (int) (ctx.textWidth("W") * textScale / 2), westY - (int) (ctx.fontHeight() * textScale / 2), color, true, textScale);
        ctx.drawScaledText("E", eastX - (int) (ctx.textWidth("E") * textScale / 2), eastY - (int) (ctx.fontHeight() * textScale / 2), color, true, textScale);
    }

    private int getGroupColor(TargetGroup group) {
        return switch (group) {
            case PLAYER -> playerColor;
            case HOSTILE -> hostileColor;
            case PASSIVE -> passiveColor;
            case NEUTRAL -> neutralColor;
            case WATER -> waterColor;
            case BOSS -> bossColor;
            default -> playerColor;
        };
    }

    private int applyAlpha(int color, float alpha) {
        int a = (color >>> 24) & 0xFF;
        int na = (int) (a * alpha);
        if (na < 0) na = 0;
        if (na > 255) na = 255;
        return (na << 24) | (color & 0x00FFFFFF);
    }

    private boolean shouldDrawGroup(TargetGroup group) {
        return switch (group) {
            case PLAYER -> drawPlayers;
            case HOSTILE -> drawHostile;
            case PASSIVE -> drawPassive;
            case NEUTRAL -> drawNeutral;
            case WATER -> drawWater;
            case BOSS -> drawBoss;
            default -> true;
        };
    }

    @Override
    public void renderEdit(HudContext ctx) {
        float scaledSize = size * radarScale;
        Renderer2D.COLOR.drawRoundedRect(x, y, scaledSize, scaledSize, BASE_CORNER_RADIUS, new Color(0x80000000));
        Renderer2D.COLOR.drawRoundedRectOutline(x, y, scaledSize, scaledSize, BASE_CORNER_RADIUS, new Color(BASE_BORDER_COLOR), 1.0);
        ctx.drawScaledText("Radar", x + 4, y + 4, 0xFFFFFFFF, true, textScale);
    }
}