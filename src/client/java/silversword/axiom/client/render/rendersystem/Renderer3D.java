package silversword.axiom.client.render.rendersystem;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.render.RenderUtils;
import silversword.axiom.client.render.rendersystem.world.Dir;

public class Renderer3D {
    public final VertexBufferBuilder lines;
    public final VertexBufferBuilder triangles;
    private final RenderPipeline linesPipeline;
    private final RenderPipeline trianglesPipeline;

    public Renderer3D(RenderPipeline lines, RenderPipeline triangles) {
        this.lines = new VertexBufferBuilder(lines);
        this.triangles = new VertexBufferBuilder(triangles);
        this.linesPipeline = lines;
        this.trianglesPipeline = triangles;
    }

    public void begin() {
        lines.begin();
        triangles.begin();
    }

    public void render(PoseStack matrices) {
        // Asetetaan view-matriisi modelview-stäkkiin
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().identity();
        RenderSystem.getModelViewStack().mul(RenderUtils.view);

        if (lines.getIndicesCount() > 0) {
            BufferRenderer.begin()
                    .attachments(Minecraft.getInstance().getMainRenderTarget())
                    .pipeline(linesPipeline)
                    .mesh(lines)
                    .end();
        }

        if (triangles.getIndicesCount() > 0) {
            BufferRenderer.begin()
                    .attachments(Minecraft.getInstance().getMainRenderTarget())
                    .pipeline(trianglesPipeline)
                    .mesh(triangles)
                    .end();
        }

        RenderSystem.getModelViewStack().popMatrix();
    }

    // --- Line Methods ---

    public void drawLine(double x1, double y1, double z1, double x2, double y2, double z2, Color color1, Color color2) {
        lines.ensureLineCapacity();
        lines.line(
                lines.vec3(x1, y1, z1).color(color1).next(),
                lines.vec3(x2, y2, z2).color(color2).next()
        );
    }

    public void drawLine(double x1, double y1, double z1, double x2, double y2, double z2, Color color) {
        drawLine(x1, y1, z1, x2, y2, z2, color, color);
    }

    public void boxOutline(double x1, double y1, double z1, double x2, double y2, double z2, Color color, int excludeDir) {
        lines.ensureCapacity(8, 24);

        int blb = lines.vec3(x1, y1, z1).color(color).next();
        int blf = lines.vec3(x1, y1, z2).color(color).next();
        int brb = lines.vec3(x2, y1, z1).color(color).next();
        int brf = lines.vec3(x2, y1, z2).color(color).next();
        int tlb = lines.vec3(x1, y2, z1).color(color).next();
        int tlf = lines.vec3(x1, y2, z2).color(color).next();
        int trb = lines.vec3(x2, y2, z1).color(color).next();
        int trf = lines.vec3(x2, y2, z2).color(color).next();

        if (excludeDir == 0) {
            lines.line(blb, tlb); lines.line(blf, tlf); lines.line(brb, trb); lines.line(brf, trf);
            lines.line(blb, blf); lines.line(brb, brf); lines.line(blb, brb); lines.line(blf, brf);
            lines.line(tlb, tlf); lines.line(trb, trf); lines.line(tlb, trb); lines.line(tlf, trf);
        } else {
            if (Dir.isNot(excludeDir, Dir.WEST) && Dir.isNot(excludeDir, Dir.NORTH)) lines.line(blb, tlb);
            if (Dir.isNot(excludeDir, Dir.WEST) && Dir.isNot(excludeDir, Dir.SOUTH)) lines.line(blf, tlf);
            if (Dir.isNot(excludeDir, Dir.EAST) && Dir.isNot(excludeDir, Dir.NORTH)) lines.line(brb, trb);
            if (Dir.isNot(excludeDir, Dir.EAST) && Dir.isNot(excludeDir, Dir.SOUTH)) lines.line(brf, trf);

            if (Dir.isNot(excludeDir, Dir.WEST) && Dir.isNot(excludeDir, Dir.DOWN)) lines.line(blb, blf);
            if (Dir.isNot(excludeDir, Dir.EAST) && Dir.isNot(excludeDir, Dir.DOWN)) lines.line(brb, brf);
            if (Dir.isNot(excludeDir, Dir.NORTH) && Dir.isNot(excludeDir, Dir.DOWN)) lines.line(blb, brb);
            if (Dir.isNot(excludeDir, Dir.SOUTH) && Dir.isNot(excludeDir, Dir.DOWN)) lines.line(blf, brf);

            if (Dir.isNot(excludeDir, Dir.WEST) && Dir.isNot(excludeDir, Dir.UP)) lines.line(tlb, tlf);
            if (Dir.isNot(excludeDir, Dir.EAST) && Dir.isNot(excludeDir, Dir.UP)) lines.line(trb, trf);
            if (Dir.isNot(excludeDir, Dir.NORTH) && Dir.isNot(excludeDir, Dir.UP)) lines.line(tlb, trb);
            if (Dir.isNot(excludeDir, Dir.SOUTH) && Dir.isNot(excludeDir, Dir.UP)) lines.line(tlf, trf);
        }
    }

    // --- Quad & Side Methods ---

    public void quad(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, Color color) {
        triangles.ensureQuadCapacity();
        triangles.quad(
                triangles.vec3(x1, y1, z1).color(color).next(),
                triangles.vec3(x2, y2, z2).color(color).next(),
                triangles.vec3(x3, y3, z3).color(color).next(),
                triangles.vec3(x4, y4, z4).color(color).next()
        );
    }

    public void boxFill(double x1, double y1, double z1, double x2, double y2, double z2, Color color, int excludeDir) {
        triangles.ensureCapacity(8, 36);
        int blb = triangles.vec3(x1, y1, z1).color(color).next();
        int blf = triangles.vec3(x1, y1, z2).color(color).next();
        int brb = triangles.vec3(x2, y1, z1).color(color).next();
        int brf = triangles.vec3(x2, y1, z2).color(color).next();
        int tlb = triangles.vec3(x1, y2, z1).color(color).next();
        int tlf = triangles.vec3(x1, y2, z2).color(color).next();
        int trb = triangles.vec3(x2, y2, z1).color(color).next();
        int trf = triangles.vec3(x2, y2, z2).color(color).next();

        if (excludeDir == 0) {
            triangles.quad(blb, blf, tlf, tlb);
            triangles.quad(brb, trb, trf, brf);
            triangles.quad(blb, tlb, trb, brb);
            triangles.quad(blf, brf, trf, tlf);
            triangles.quad(blb, brb, brf, blf);
            triangles.quad(tlb, tlf, trf, trb);
        } else {
            if (Dir.isNot(excludeDir, Dir.WEST)) triangles.quad(blb, blf, tlf, tlb);
            if (Dir.isNot(excludeDir, Dir.EAST)) triangles.quad(brb, trb, trf, brf);
            if (Dir.isNot(excludeDir, Dir.NORTH)) triangles.quad(blb, tlb, trb, brb);
            if (Dir.isNot(excludeDir, Dir.SOUTH)) triangles.quad(blf, brf, trf, tlf);
            if (Dir.isNot(excludeDir, Dir.DOWN)) triangles.quad(blb, brb, brf, blf);
            if (Dir.isNot(excludeDir, Dir.UP)) triangles.quad(tlb, tlf, trf, trb);
        }
    }

    public void drawBox(double x1, double y1, double z1, double x2, double y2, double z2, Color sideColor, Color lineColor, ShapeMode mode, int excludeDir) {
        if (mode.lines()) boxOutline(x1, y1, z1, x2, y2, z2, lineColor, excludeDir);
        if (mode.sides()) boxFill(x1, y1, z1, x2, y2, z2, sideColor, excludeDir);
    }
}
