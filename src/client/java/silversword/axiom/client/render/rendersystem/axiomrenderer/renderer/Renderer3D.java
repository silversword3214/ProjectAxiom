package silversword.axiom.client.render.rendersystem.axiomrenderer.renderer;


import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import silversword.axiom.client.render.rendersystem.axiomrenderer.core.RenderCore;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.misc.ShapeModeEnum;
import silversword.axiom.client.render.rendersystem.utils.render.RenderUtils;

public class Renderer3D {
    private final RenderCore core;
    private final float tickDelta;
    private final Matrix4f projectionMatrix;
    private final Matrix4f viewMatrix;

    private final float THICKNESS = 2f;

    public Renderer3D(RenderCore core, Matrix4f projection, Matrix4f view, float tickDelta) {
        this.core = core;
        this.tickDelta = tickDelta;
        this.projectionMatrix = projection;
        this.viewMatrix = view;
        core.beginFrame(projection, view);
        RenderUtils.updateScreenCenter(projection, view);
    }

    public float getTickDelta() {
        return tickDelta;
    }

    // ---------- Lines ----------
    public void drawLine(double x1, double y1, double z1,
                         double x2, double y2, double z2,
                         int color, float thickness) {
        core.addLine3D(x1, y1, z1, x2, y2, z2, thickness, color);
    }

    public void drawLine(double x1, double y1, double z1, double x2, double y2, double z2, int color) {
        core.addLine3D(x1, y1, z1, x2, y2, z2, 1.0f, color);
    }

    public void drawLine(double x1, double y1, double z1,
                         double x2, double y2, double z2,
                         Color color, float thickness) {
        drawLine(x1, y1, z1, x2, y2, z2, color.getARGB(), thickness);
    }

    public void drawLine(double x1, double y1, double z1, double x2, double y2, double z2, Color color) {
        drawLine(x1, y1, z1, x2, y2, z2, color.getARGB());
    }

    // ---------- Box Outline ----------
    public void boxOutline(double minX, double minY, double minZ,
                           double maxX, double maxY, double maxZ,
                           int color, int excludeDir) {
        Vector3d[] corners = {
                new Vector3d(minX, minY, minZ), new Vector3d(maxX, minY, minZ),
                new Vector3d(minX, minY, maxZ), new Vector3d(maxX, minY, maxZ),
                new Vector3d(minX, maxY, minZ), new Vector3d(maxX, maxY, minZ),
                new Vector3d(minX, maxY, maxZ), new Vector3d(maxX, maxY, maxZ)
        };

        int[][] edges = {
                {0,1}, {2,3}, {0,2}, {1,3},
                {4,5}, {6,7}, {4,6}, {5,7},
                {0,4}, {1,5}, {2,6}, {3,7}
        };

        for (int[] edge : edges) {
            Vector3d p1 = corners[edge[0]];
            Vector3d p2 = corners[edge[1]];
            drawLine(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, color, THICKNESS);
        }
    }

    public void boxOutline(double minX, double minY, double minZ,
                           double maxX, double maxY, double maxZ,
                           Color color, int excludeDir) {
        boxOutline(minX, minY, minZ, maxX, maxY, maxZ, color.getARGB(), excludeDir);
    }

    // ---------- Box Fill ----------
    public void boxFill(double minX, double minY, double minZ,
                        double maxX, double maxY, double maxZ,
                        int color, int excludeDir) {
        if (excludeDir == 0) {
            core.addQuad(minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ, minX, minY, maxZ, color);
            core.addQuad(maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, color);
            core.addQuad(minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, color);
            core.addQuad(minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, color);
            core.addQuad(minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, color);
            core.addQuad(minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, color);
        } else {
            boxFill(minX, minY, minZ, maxX, maxY, maxZ, color, 0);
        }
    }

    public void boxFill(double minX, double minY, double minZ,
                        double maxX, double maxY, double maxZ,
                        Color color, int excludeDir) {
        boxFill(minX, minY, minZ, maxX, maxY, maxZ, color.getARGB(), excludeDir);
    }

    // ---------- Combined Box ----------
    public void drawBox(double minX, double minY, double minZ,
                        double maxX, double maxY, double maxZ,
                        int sideColor, int lineColor,
                        ShapeModeEnum mode, int excludeDir) {
        if (mode.sides()) {
            boxFill(minX, minY, minZ, maxX, maxY, maxZ, sideColor, excludeDir);
        }
        if (mode.lines()) {
            boxOutline(minX, minY, minZ, maxX, maxY, maxZ, lineColor, excludeDir);
        }
    }

    public void drawBox(double minX, double minY, double minZ,
                        double maxX, double maxY, double maxZ,
                        Color sideColor, Color lineColor,
                        ShapeModeEnum mode, int excludeDir) {
        drawBox(minX, minY, minZ, maxX, maxY, maxZ, sideColor.getARGB(), lineColor.getARGB(), mode, excludeDir);
    }

    // ---------- Quad ----------
    public void quad(double x1, double y1, double z1,
                     double x2, double y2, double z2,
                     double x3, double y3, double z3,
                     double x4, double y4, double z4,
                     int color) {
        core.addQuad(x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, color);
    }

    public void quad(double x1, double y1, double z1,
                     double x2, double y2, double z2,
                     double x3, double y3, double z3,
                     double x4, double y4, double z4,
                     Color color) {
        quad(x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, color.getARGB());
    }

    // Getters
    public Matrix4f getProjectionMatrix() { return projectionMatrix; }
    public Matrix4f getViewMatrix() { return viewMatrix; }

    public void flush() {
        core.flush();
    }
}