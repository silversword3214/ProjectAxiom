package silversword.axiom.client.render.rendersystem.axiomrenderer.renderer;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.render.rendersystem.axiomrenderer.core.RenderCore;
import silversword.axiom.client.utils.render.CapturedModelState;

public class PlayerWireframe {

    private static final AABB LIMB_BOX = new AABB(-0.125, -0.75, -0.125, 0.125, 0, 0.125);
    private static final AABB BODY_BOX = new AABB(-0.25, -0.75, -0.125, 0.25, 0, 0.125);
    private static final AABB HEAD_BOX = new AABB(-0.25, 0, -0.25, 0.25, 0.5, 0.25);
    private static final float MODEL_SCALE = 1f;

    private static final int GREEN_FACING = 0x3300FF00;

    public static void render(CapturedModelState state, Vec3 pos, float yaw,
                              RenderCore core, int lineColor, int fillColor, float thickness) {
        if (state == null) return;

        double yawRad = Math.toRadians(yaw);
        double yawCos = Math.cos(yawRad);
        double yawSin = Math.sin(yawRad);
        float scale = MODEL_SCALE;
        float mcScale = 1.0f / 16.0f;

        Vec3 headPivot = new Vec3(
                state.head.x * mcScale,
                -state.head.y * mcScale,
                state.head.z * mcScale
        );

        renderRotatedBox(
                core,
                HEAD_BOX,
                headPivot,
                state.head.xRot,
                -state.head.yRot,
                state.head.zRot,
                lineColor,
                fillColor,
                thickness,
                yawCos,
                yawSin,
                scale,
                pos,
                true
        );

        renderPart(core, state.body, BODY_BOX, pos, yawCos, yawSin, scale, lineColor, fillColor, thickness);
        renderPart(core, state.leftArm, LIMB_BOX, pos, yawCos, yawSin, scale, lineColor, fillColor, thickness);
        renderPart(core, state.rightArm, LIMB_BOX, pos, yawCos, yawSin, scale, lineColor, fillColor, thickness);
        renderPart(core, state.leftLeg, LIMB_BOX, pos, yawCos, yawSin, scale, lineColor, fillColor, thickness);
        renderPart(core, state.rightLeg, LIMB_BOX, pos, yawCos, yawSin, scale, lineColor, fillColor, thickness);
    }

    private static void renderPart(RenderCore core,
                                   CapturedModelState.PartState partState,
                                   AABB localBox,
                                   Vec3 pos,
                                   double yawCos, double yawSin,
                                   float scale,
                                   int lineColor, int fillColor, float thickness) {

        float mcScale = 1.0f / 16.0f;
        Vec3 pivot = new Vec3(
                partState.x * mcScale,
                -partState.y * mcScale,
                partState.z * mcScale
        );

        renderRotatedBox(
                core,
                localBox,
                pivot,
                partState.xRot,
                partState.yRot,
                partState.zRot,
                lineColor,
                fillColor,
                thickness,
                yawCos,
                yawSin,
                scale,
                pos,
                false // Not head
        );
    }

    private static void renderRotatedBox(RenderCore core, AABB box, Vec3 pivot,
                                         float xRotRad, float yRotRad, float zRotRad,
                                         int lineColor, int fillColor, float thickness,
                                         double yawCos, double yawSin, float scale,
                                         Vec3 basePos,
                                         boolean isHead) {
        Vec3[] corners = getCorners(box);
        Vec3[] transformed = new Vec3[8];

        for (int i = 0; i < 8; i++) {
            double dx = corners[i].x;
            double dy = corners[i].y;
            double dz = corners[i].z;

            // 1) Z-rot (Roll)
            if (zRotRad != 0) {
                double cosZ = Math.cos(zRotRad);
                double sinZ = Math.sin(zRotRad);
                double rx = dx * cosZ - dy * sinZ;
                double ry = dx * sinZ + dy * cosZ;
                dx = rx; dy = ry;
            }

            //  Y-rot (Yaw)
            if (yRotRad != 0) {
                double cosY = Math.cos(yRotRad);
                double sinY = Math.sin(yRotRad);
                double rx = dx * cosY + dz * sinY;
                double rz = -dx * sinY + dz * cosY;
                dx = rx; dz = rz;
            }

            // 3) X-rot (Pitch)
            if (xRotRad != 0) {
                double cosX = Math.cos(xRotRad);
                double sinX = Math.sin(xRotRad);
                double ry = dy * cosX - dz * sinX;
                double rz = dy * sinX + dz * cosX;
                dy = ry; dz = rz;
            }

            double x = (pivot.x + dx) * scale;
            double y = (pivot.y + dy) * scale;
            double z = (pivot.z + dz) * scale;

            double worldX = basePos.x + (x * yawCos - z * yawSin);
            double worldZ = basePos.z + (x * yawSin + z * yawCos);
            double worldY = basePos.y + y + 1.501;

            transformed[i] = new Vec3(worldX, worldY, worldZ);
        }


        int[][] faces = {
                {0,1,2,3}, // minX
                {4,5,6,7}, // maxY
                {0,3,7,4}, // minX
                {1,2,6,5}, // maxX
                {0,1,5,4}, // minZ
                {3,2,6,7}  // maxZ
        };

        for (int j = 0; j < faces.length; j++) {
            int[] face = faces[j];
            Vec3 a = transformed[face[0]];
            Vec3 b = transformed[face[1]];
            Vec3 c = transformed[face[2]];
            Vec3 d = transformed[face[3]];

            int currentFillColor = (isHead && j == 5) ? GREEN_FACING : fillColor;

            core.addQuad(a.x, a.y, a.z, b.x, b.y, b.z, c.x, c.y, c.z, d.x, d.y, d.z, currentFillColor);
        }

        int[][] edges = {
                {0,1}, {1,2}, {2,3}, {3,0},
                {4,5}, {5,6}, {6,7}, {7,4},
                {0,4}, {1,5}, {2,6}, {3,7}
        };
        for (int[] e : edges) {
            Vec3 p1 = transformed[e[0]];
            Vec3 p2 = transformed[e[1]];
            core.addLine3D(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, thickness, lineColor);
        }
    }

    private static Vec3[] getCorners(AABB box) {
        return new Vec3[]{
                new Vec3(box.minX, box.minY, box.minZ),
                new Vec3(box.maxX, box.minY, box.minZ),
                new Vec3(box.maxX, box.minY, box.maxZ),
                new Vec3(box.minX, box.minY, box.maxZ),
                new Vec3(box.minX, box.maxY, box.minZ),
                new Vec3(box.maxX, box.maxY, box.minZ),
                new Vec3(box.maxX, box.maxY, box.maxZ),
                new Vec3(box.minX, box.maxY, box.maxZ)
        };
    }
}