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

    // ------------------------------------------------------------------------
// Sphere rendering
// ------------------------------------------------------------------------

    /**
     * Draws a 3D sphere (filled and/or outline) at the given position.
     *
     * @param x       center X
     * @param y       center Y
     * @param z       center Z
     * @param radius  sphere radius
     * @param color   color (for both sides and lines)
     * @param mode    which parts to draw (sides, lines, or both)
     * @param lonSegments number of longitudinal segments (around the equator), min 3
     * @param latSegments number of latitudinal segments (from north to south), min 2
     */
    public void drawSphere(double x, double y, double z, double radius,
                           int color, ShapeModeEnum mode,
                           int lonSegments, int latSegments) {
        if (mode.sides()) {
            drawSphereFill(x, y, z, radius, color, lonSegments, latSegments);
        }
        if (mode.lines()) {
            drawSphereOutline(x, y, z, radius, color, lonSegments, latSegments);
        }
    }

    /**
     * Draws a filled sphere using quads (and triangles near poles).
     */
    private void drawSphereFill(double cx, double cy, double cz, double radius,
                                int color, int lonSegments, int latSegments) {
        // ensure enough segments
        lonSegments = Math.max(3, lonSegments);
        latSegments = Math.max(2, latSegments);

        double lonStep = 2 * Math.PI / lonSegments;
        double latStep = Math.PI / latSegments;

        for (int i = 0; i < latSegments; i++) {
            double phi1 = i * latStep;               // current latitude angle
            double phi2 = (i + 1) * latStep;         // next latitude angle

            double sinPhi1 = Math.sin(phi1);
            double cosPhi1 = Math.cos(phi1);
            double sinPhi2 = Math.sin(phi2);
            double cosPhi2 = Math.cos(phi2);

            for (int j = 0; j < lonSegments; j++) {
                double theta1 = j * lonStep;
                double theta2 = (j + 1) % lonSegments == 0 ? 2 * Math.PI : (j + 1) * lonStep;

                double sinTheta1 = Math.sin(theta1);
                double cosTheta1 = Math.cos(theta1);
                double sinTheta2 = Math.sin(theta2);
                double cosTheta2 = Math.cos(theta2);

                // Four corners of the quad (in sphere coordinates)
                double x1 = radius * sinPhi1 * cosTheta1;
                double y1 = radius * cosPhi1;
                double z1 = radius * sinPhi1 * sinTheta1;

                double x2 = radius * sinPhi1 * cosTheta2;
                double y2 = radius * cosPhi1;
                double z2 = radius * sinPhi1 * sinTheta2;

                double x3 = radius * sinPhi2 * cosTheta2;
                double y3 = radius * cosPhi2;
                double z3 = radius * sinPhi2 * sinTheta2;

                double x4 = radius * sinPhi2 * cosTheta1;
                double y4 = radius * cosPhi2;
                double z4 = radius * sinPhi2 * sinTheta1;

                // Translate to world coordinates
                x1 += cx; y1 += cy; z1 += cz;
                x2 += cx; y2 += cy; z2 += cz;
                x3 += cx; y3 += cy; z3 += cz;
                x4 += cx; y4 += cy; z4 += cz;

                // Handle poles (phi1 == 0 or phi2 == PI) – degenerate quad -> triangle
                if (phi1 == 0) {
                    // north pole triangle: (x1,y1,z1) is the pole itself
                    quad(x2, y2, z2, x3, y3, z3, x4, y4, z4, x4, y4, z4, color);
                } else if (phi2 == Math.PI) {
                    // south pole triangle: (x3,y3,z3) is the pole
                    quad(x1, y1, z1, x2, y2, z2, x4, y4, z4, x4, y4, z4, color);
                } else {
                    // normal quad
                    quad(x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, color);
                }
            }
        }
    }

    /**
     * Draws a wireframe sphere using lines of latitude and longitude.
     */
    private void drawSphereOutline(double cx, double cy, double cz, double radius,
                                   int color, int lonSegments, int latSegments) {
        lonSegments = Math.max(3, lonSegments);
        latSegments = Math.max(2, latSegments);

        double lonStep = 2 * Math.PI / lonSegments;
        double latStep = Math.PI / latSegments;

        // Longitude lines (meridians) – fixed theta, vary phi
        for (int i = 0; i < lonSegments; i++) {
            double theta = i * lonStep;
            double sinTheta = Math.sin(theta);
            double cosTheta = Math.cos(theta);

            double prevX = 0, prevY = 0, prevZ = 0;
            for (int j = 0; j <= latSegments; j++) {
                double phi = j * latStep;
                double sinPhi = Math.sin(phi);
                double cosPhi = Math.cos(phi);

                double x = radius * sinPhi * cosTheta + cx;
                double y = radius * cosPhi + cy;
                double z = radius * sinPhi * sinTheta + cz;

                if (j > 0) {
                    drawLine(prevX, prevY, prevZ, x, y, z, color, THICKNESS);
                }
                prevX = x; prevY = y; prevZ = z;
            }
        }

        // Latitude lines (parallels) – fixed phi, vary theta
        for (int j = 1; j < latSegments; j++) {  // skip poles (j=0 and j=latSegments)
            double phi = j * latStep;
            double sinPhi = Math.sin(phi);
            double cosPhi = Math.cos(phi);

            double prevX = 0, prevY = 0, prevZ = 0;
            for (int i = 0; i <= lonSegments; i++) {
                double theta = i * lonStep;
                double sinTheta = Math.sin(theta);
                double cosTheta = Math.cos(theta);

                double x = radius * sinPhi * cosTheta + cx;
                double y = radius * cosPhi + cy;
                double z = radius * sinPhi * sinTheta + cz;

                if (i > 0) {
                    drawLine(prevX, prevY, prevZ, x, y, z, color, THICKNESS);
                }
                prevX = x; prevY = y; prevZ = z;
            }
        }
    }

    public void drawSphere(double x, double y, double z, double radius,
                           int fillColor, int outlineColor, ShapeModeEnum mode,
                           int lonSegments, int latSegments) {
        if (mode.sides()) {
            drawSphereFill(x, y, z, radius, fillColor, lonSegments, latSegments);
        }
        if (mode.lines()) {
            drawSphereOutline(x, y, z, radius, outlineColor, lonSegments, latSegments);
        }
    }


}