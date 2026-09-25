package silversword.axiom.client.render.rendersystem.axiomrenderer.esp2d;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.config.ClickGuiConfigManager;
import silversword.axiom.client.event.render.Render2DEvent;
import silversword.axiom.client.modules.render.Esp2D;
import silversword.axiom.client.render.font.CustomTextRenderer;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.rainbow.RainbowPalette;
import silversword.axiom.client.render.rendersystem.utils.render.NametagUtils;

public final class Esp2DRenderer {

    private Esp2DRenderer() {}

    public static void render(Render2DEvent event, Esp2D module) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        int screenW = event.getScreenWidth();
        int screenH = event.getScreenHeight();
        var camera = mc.gameRenderer.mainCamera();
        Vec3 camPos = camera.position();
        org.joml.Vector3fc fwd = camera.forwardVector();
        Vec3 camDir = new Vec3(fwd.x(), fwd.y(), fwd.z());
        double maxDist = module.getRenderDistance();
        double maxDistSq = maxDist * maxDist;
        float tickDelta = event.tickDelta;

        long now = System.currentTimeMillis();

        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity le)) continue;
            if (!le.isAlive()) continue;
            if (e == mc.player && !module.includeSelf()) continue;
            if (!module.isTarget(le)) continue;

            double distSq = e.distanceToSqr(camPos);
            if (distSq > maxDistSq) continue;

            // Kameran edessä?
            Vec3 entCenter = e.position().add(0, e.getBbHeight() * 0.5, 0);
            double dot = entCenter.subtract(camPos).dot(camDir);
            if (dot < 0.1) continue;

            // Interpoloitu positio — käytetään AABB:n laskennassa
            double ix = Mth.lerp(tickDelta, e.xOld, e.getX());
            double iy = Mth.lerp(tickDelta, e.yOld, e.getY());
            double iz = Mth.lerp(tickDelta, e.zOld, e.getZ());

            AABB box = e.getBoundingBox();
            // Käytä interpoloitua offsetia (e.position() vs AABB voi olla sama tickillä)
            double dx = ix - e.getX();
            double dy = iy - e.getY();
            double dz = iz - e.getZ();
            AABB interpBox = box.move(dx, dy, dz);

            // Projektoi 8 kulmaa
            double sxMin = Double.MAX_VALUE, syMin = Double.MAX_VALUE;
            double sxMax = -Double.MAX_VALUE, syMax = -Double.MAX_VALUE;
            boolean any = false;

            for (int i = 0; i < 8; i++) {
                double cx = (i & 1) == 0 ? interpBox.minX : interpBox.maxX;
                double cy = (i & 2) == 0 ? interpBox.minY : interpBox.maxY;
                double cz = (i & 4) == 0 ? interpBox.minZ : interpBox.maxZ;
                Vec3 p = NametagUtils.worldToScreen(new Vec3(cx, cy, cz), screenW, screenH);
                if (p == null) continue;
                any = true;
                if (p.x < sxMin) sxMin = p.x;
                if (p.x > sxMax) sxMax = p.x;
                if (p.y < syMin) syMin = p.y;
                if (p.y > syMax) syMax = p.y;
            }
            if (!any) continue;

            double boxW = sxMax - sxMin;
            double boxH = syMax - syMin;
            if (boxW < 1 || boxH < 1) continue;

            // Väri
            float hpPct = le.getHealth() / Math.max(1f, le.getMaxHealth());
            int color = module.resolveColor(le, hpPct, now);

            // Piirto
            drawBox(event, module, sxMin, syMin, boxW, boxH, color);

            if (module.showHealthBar()) {
                drawHealthBar(event, module, sxMin, syMin, boxH, hpPct);
            }

            if (module.showName() || module.showDistance() || module.showHpText()) {
                drawLabels(event, module, le, sxMin, syMin, boxW, hpPct);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  BOX
    // ═══════════════════════════════════════════════════════════════

    private static void drawBox(Render2DEvent event, Esp2D module,
                                double x, double y, double w, double h, int color) {
        var r = event.getRenderer();
        float thickness = (float) module.getThickness();
        String mode = module.getBoxMode();
        boolean rounded = module.isRounded();

        if ("Corners".equals(mode)) {
            // 4 kulmasulkaa
            float cs = (float) module.getCornerSize();
            if (cs > w / 2) cs = (float) (w / 2);
            if (cs > h / 2) cs = (float) (h / 2);

            // Top-left
            r.drawRect((float) x, (float) y, cs, thickness, color);
            r.drawRect((float) x, (float) y, thickness, cs, color);
            // Top-right
            r.drawRect((float) (x + w - cs), (float) y, cs, thickness, color);
            r.drawRect((float) (x + w - thickness), (float) y, thickness, cs, color);
            // Bottom-left
            r.drawRect((float) x, (float) (y + h - thickness), cs, thickness, color);
            r.drawRect((float) x, (float) (y + h - cs), thickness, cs, color);
            // Bottom-right
            r.drawRect((float) (x + w - cs), (float) (y + h - thickness), cs, thickness, color);
            r.drawRect((float) (x + w - thickness), (float) (y + h - cs), thickness, cs, color);
        } else {
            // Täysi boxi
            if (rounded) {
                double radius = Math.min(4.0, Math.min(w, h) * 0.15);
                r.drawRoundedRectOutline(x, y, w, h, radius, color, thickness);
            } else {
                r.drawRectOutline((float) x, (float) y, (float) w, (float) h,
                        thickness, color);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  HEALTH BAR
    // ═══════════════════════════════════════════════════════════════

    private static void drawHealthBar(Render2DEvent event, Esp2D module,
                                      double boxX, double boxY, double boxH,
                                      float hpPct) {
        var r = event.getRenderer();
        float barW = (float) module.getBarWidth();
        float gap = 3.0f;

        double bx = boxX - gap - barW;
        double by = boxY;

        // Tausta
        r.drawRect((float) bx, (float) by, barW, (float) boxH, 0xFF1A1A1A);

        // Täyttö pohjasta ylös
        float fillH = (float) boxH * hpPct;
        float fillY = (float) (by + boxH - fillH);
        int hpColor = hpColor(hpPct);
        r.drawRect((float) bx, fillY, barW, fillH, hpColor);

        // Reunus
        r.drawRectOutline((float) bx, (float) by, barW, (float) boxH, 1.0f, 0xCC000000);
    }

    private static int hpColor(float pct) {
        if (pct >= 0.66f) return 0xFF44DD44;
        if (pct >= 0.33f) return 0xFFFFCC00;
        return 0xFFFF3344;
    }

    // ═══════════════════════════════════════════════════════════════
    //  LABELS
    // ═══════════════════════════════════════════════════════════════

    private static void drawLabels(Render2DEvent event, Esp2D module,
                                   LivingEntity le, double boxX, double boxY,
                                   double boxW, float hpPct) {
        var g = event.getGuiGraphics();
        float scale = (float) module.getTextScale();

        String name = le.getName().getString();
        String distText = module.showDistance()
                ? String.format("%.1fm", Minecraft.getInstance().player.distanceTo(le))
                : null;
        String hpText = module.showHpText()
                ? String.format("%.0f", le.getHealth())
                : null;

        // Yläpuolen teksti: nimi + (valinnainen) dist
        StringBuilder top = new StringBuilder();
        if (module.showName()) top.append(name);
        if (distText != null) {
            if (top.length() > 0) top.append("  ");
            top.append(distText);
        }

        double yCursor = boxY;
        if (top.length() > 0) {
            yCursor -= 12 * scale;
            double w = measureWidth(top.toString(), scale);
            double x = boxX + (boxW - w) / 2.0;
            drawText(event, top.toString(), x, yCursor,
                    module.getNameColor(), true, scale);
        }

        // HP-teksti boxin sisällä (yläkulmassa)
        if (hpText != null) {
            double w = measureWidth(hpText, scale);
            double x = boxX + (boxW - w) / 2.0;
            double y = boxY - 12 * scale - (top.length() > 0 ? 10 * scale : 0);
            drawText(event, hpText, x, y, hpColor(hpPct), true, scale);
        }
    }

    private static double measureWidth(String text, float scale) {
        TextRenderer tr = TextRenderer.get();
        tr.begin(scale, false, true);
        try {
            return tr.getWidth(text, false);
        } finally {
            tr.end();
        }
    }

    private static void drawText(Render2DEvent event, String text,
                                 double x, double y, int color,
                                 boolean shadow, float scale) {
        var g = event.getGuiGraphics();
        TextRenderer tr = TextRenderer.get();
        tr.begin(scale, false, true);
        try {
            if (tr instanceof CustomTextRenderer ctr) {
                ctr.render(g, text, x, y, new Color(color), shadow);
            } else {
                tr.render(text, x, y, new Color(color), shadow);
            }
        } finally {
            tr.end();
        }
    }
}