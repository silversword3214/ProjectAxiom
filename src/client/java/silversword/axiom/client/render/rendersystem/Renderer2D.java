package silversword.axiom.client.render.rendersystem;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.textures.GpuSampler;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.misc.PreInit;
import silversword.axiom.client.render.rendersystem.utils.texture.TextureRegion;

public class Renderer2D {
    public static Renderer2D COLOR;
    public static Renderer2D TEXTURE;

    private final boolean textured;

    public final VertexBufferBuilder triangles;
    public final VertexBufferBuilder lines;

    public Renderer2D(boolean textured) {
        this.textured = textured;

        triangles = new VertexBufferBuilder(textured ? CustomRenderingPipelineProvider.UI_TEXTURED : CustomRenderingPipelineProvider.UI_COLORED);
        lines = new VertexBufferBuilder(CustomRenderingPipelineProvider.UI_COLORED_LINES);
    }

    @PreInit
    public static void init() {
        COLOR = new Renderer2D(false);
        TEXTURE = new Renderer2D(true);
    }

    public void setAlpha(double alpha) {
        triangles.alpha = alpha;
    }

    public void begin() {
        triangles.begin();
        lines.begin();
    }

    public void end() {
        if (triangles.isBuilding()) triangles.end();
        if (lines.isBuilding()) lines.end();
    }

    public void render() {
        render(null, null, null);
    }

    public void render(GpuTextureView textureView, GpuSampler sampler) {
        if (!textured)
            throw new IllegalStateException("Tried to render with a texture with a non-textured Renderer2D");

        render("u_Texture", textureView, sampler);
    }



    public void render(String samplerName, GpuTextureView samplerView, GpuSampler sampler) {
        if (lines.isBuilding()) lines.end();
        if (triangles.isBuilding()) triangles.end();

        BufferRenderer.begin()
                .attachments(Minecraft.getInstance().getMainRenderTarget())
                .pipeline(CustomRenderingPipelineProvider.UI_COLORED_LINES)
                .mesh(lines)
                .end();

        BufferRenderer.begin()
                .attachments(Minecraft.getInstance().getMainRenderTarget())
                .pipeline(textured ? CustomRenderingPipelineProvider.UI_TEXTURED : CustomRenderingPipelineProvider.UI_COLORED)
                .mesh(triangles)
                .sampler(samplerName, samplerView, sampler)
                .end();
    }

    // Triangles
    public void triangle(double x1, double y1, double x2, double y2, double x3, double y3, Color color) {
        triangles.ensureTriCapacity();
        triangles.triangle(
                triangles.vec2(x1, y1).color(color).next(),
                triangles.vec2(x2, y2).color(color).next(),
                triangles.vec2(x3, y3).color(color).next()
        );
    }

    // Lines
    public void line(double x1, double y1, double x2, double y2, Color color) {
        lines.ensureLineCapacity();
        lines.line(
                lines.vec2(x1, y1).color(color).next(),
                lines.vec2(x2, y2).color(color).next()
        );
    }


    public void lineThick(double x1, double y1, double x2, double y2, double thickness, Color color) {
        if (color.getAlpha() == 0 || thickness <= 0) return;
        double dx = x2 - x1;
        double dy = y2 - y1;
        double len = Math.sqrt(dx*dx + dy*dy);
        if (len < 1e-6) return;
        double nx = -dy / len;
        double ny = dx / len;
        double half = thickness / 2.0;
        double tx = nx * half;
        double ty = ny * half;

        double x1a = x1 + tx;
        double y1a = y1 + ty;
        double x1b = x1 - tx;
        double y1b = y1 - ty;
        double x2a = x2 + tx;
        double y2a = y2 + ty;
        double x2b = x2 - tx;
        double y2b = y2 - ty;

        triangles.ensureQuadCapacity();
        int i1 = triangles.vec2(x1a, y1a).color(color).next();
        int i2 = triangles.vec2(x1b, y1b).color(color).next();
        int i3 = triangles.vec2(x2b, y2b).color(color).next();
        int i4 = triangles.vec2(x2a, y2a).color(color).next();
        triangles.quad(i1, i2, i3, i4);
    }

    public void boxLines(double x, double y, double width, double height, Color color) {
        lines.ensureCapacity(4, 4);
        int i1 = lines.vec2(x, y).color(color).next();
        int i2 = lines.vec2(x, y + height).color(color).next();
        int i3 = lines.vec2(x + width, y + height).color(color).next();
        int i4 = lines.vec2(x + width, y).color(color).next();
        lines.line(i1, i2);
        lines.line(i2, i3);
        lines.line(i3, i4);
        lines.line(i4, i1);
    }

    // Quads
    public void quad(double x, double y, double width, double height, Color cTopLeft, Color cTopRight, Color cBottomRight, Color cBottomLeft) {
        triangles.ensureQuadCapacity();
        triangles.quad(
                triangles.vec2(x, y).color(cTopLeft).next(),
                triangles.vec2(x, y + height).color(cBottomLeft).next(),
                triangles.vec2(x + width, y + height).color(cBottomRight).next(),
                triangles.vec2(x + width, y).color(cTopRight).next()
        );
    }

    public void quad(double x, double y, double width, double height, Color color) {
        quad(x, y, width, height, color, color, color, color);
    }

    // Textured quads
    public void texQuad(double x, double y, double width, double height, Color color) {
        triangles.ensureQuadCapacity();
        triangles.quad(
                triangles.vec2(x, y).vec2(0, 0).color(color).next(),
                triangles.vec2(x, y + height).vec2(0, 1).color(color).next(),
                triangles.vec2(x + width, y + height).vec2(1, 1).color(color).next(),
                triangles.vec2(x + width, y).vec2(1, 0).color(color).next()
        );
    }

    public void texQuad(double x, double y, double width, double height, TextureRegion texture, Color color) {
        triangles.ensureQuadCapacity();
        triangles.quad(
                triangles.vec2(x, y).vec2(texture.x1, texture.y1).color(color).next(),
                triangles.vec2(x, y + height).vec2(texture.x1, texture.y2).color(color).next(),
                triangles.vec2(x + width, y + height).vec2(texture.x2, texture.y2).color(color).next(),
                triangles.vec2(x + width, y).vec2(texture.x2, texture.y1).color(color).next()
        );
    }

    public void texQuad(double x, double y, double width, double height, double rotation, double texX1, double texY1, double texX2, double texY2, Color color) {
        triangles.ensureQuadCapacity();
        double rad = Math.toRadians(rotation);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double oX = x + width / 2;
        double oY = y + height / 2;

        double _x1 = ((x - oX) * cos) - ((y - oY) * sin) + oX;
        double _y1 = ((y - oY) * cos) + ((x - oX) * sin) + oY;
        int i1 = triangles.vec2(_x1, _y1).vec2(texX1, texY1).color(color).next();

        double _x2 = ((x - oX) * cos) - ((y + height - oY) * sin) + oX;
        double _y2 = ((y + height - oY) * cos) + ((x - oX) * sin) + oY;
        int i2 = triangles.vec2(_x2, _y2).vec2(texX1, texY2).color(color).next();

        double _x3 = ((x + width - oX) * cos) - ((y + height - oY) * sin) + oX;
        double _y3 = ((y + height - oY) * cos) + ((x + width - oX) * sin) + oY;
        int i3 = triangles.vec2(_x3, _y3).vec2(texX2, texY2).color(color).next();

        double _x4 = ((x + width - oX) * cos) - ((y - oY) * sin) + oX;
        double _y4 = ((y - oY) * cos) + ((x + width - oX) * sin) + oY;
        int i4 = triangles.vec2(_x4, _y4).vec2(texX2, texY1).color(color).next();

        triangles.quad(i1, i2, i3, i4);
    }

    public void drawCircle(double cx, double cy, double radius, Color color) {
        if (radius <= 0) return;

        // Lasketaan tarvittava määrä segmenttejä siten, että max 0.5 pikseliä per kaari
        double circumference = 2 * Math.PI * radius;
        int segments = Math.max(12, (int) (circumference / 0.5)); // 0.5px tarkkuus

        double angleStep = 2 * Math.PI / segments;
        for (int i = 0; i < segments; i++) {
            double angle1 = i * angleStep;
            double angle2 = (i + 1) * angleStep;
            double x1 = cx + radius * Math.cos(angle1);
            double y1 = cy + radius * Math.sin(angle1);
            double x2 = cx + radius * Math.cos(angle2);
            double y2 = cy + radius * Math.sin(angle2);
            triangle(cx, cy, x1, y1, x2, y2, color);
        }
    }

    public void drawCircleOutline(double cx, double cy, double radius, Color color, double thickness) {
        if (color.getAlpha() == 0 || radius <= 0 || thickness <= 0) return;

        // Lasketaan tarvittava määrä segmenttejä siten, että kaari näyttää sileältä
        double circumference = 2 * Math.PI * radius;
        int segments = Math.max(24, (int) (circumference / 0.5)); // 0.5px tarkkuus

        double angleStep = 2 * Math.PI / segments;
        for (int i = 0; i < segments; i++) {
            double angle1 = i * angleStep;
            double angle2 = (i + 1) * angleStep;
            double x1 = cx + radius * Math.cos(angle1);
            double y1 = cy + radius * Math.sin(angle1);
            double x2 = cx + radius * Math.cos(angle2);
            double y2 = cy + radius * Math.sin(angle2);
            lineThick(x1, y1, x2, y2, thickness, color);
        }
    }

    public void drawRoundedRect(double x, double y, double w, double h, double radius, Color color) {

        if (radius <= 0.2) {
            quad(x, y, w, h, color);
            return;
        }

        radius = Math.min(radius, Math.min(w / 2, h / 2));

        quad(x + radius, y, w - 2 * radius, radius, color); // yläreuna
        quad(x + radius, y + h - radius, w - 2 * radius, radius, color); // alareuna
        quad(x, y + radius, w, h - 2 * radius, color); // keskiosa

        // Corners
        double arcLength = (Math.PI * radius) / 2; // 1/4
        int segments = Math.max(6, (int) (arcLength / 0.5)); // Segments

        double angleStep = Math.PI / 2 / segments;

        for (int i = 0; i < segments; i++) {
            double angle1 = Math.PI + i * angleStep;
            double angle2 = Math.PI + (i + 1) * angleStep;
            double x1 = x + radius + radius * Math.cos(angle1);
            double y1 = y + radius + radius * Math.sin(angle1);
            double x2 = x + radius + radius * Math.cos(angle2);
            double y2 = y + radius + radius * Math.sin(angle2);
            triangle(x + radius, y + radius, x1, y1, x2, y2, color);
        }

        for (int i = 0; i < segments; i++) {
            double angle1 = Math.PI * 1.5 + i * angleStep;
            double angle2 = Math.PI * 1.5 + (i + 1) * angleStep;
            double x1 = x + w - radius + radius * Math.cos(angle1);
            double y1 = y + radius + radius * Math.sin(angle1);
            double x2 = x + w - radius + radius * Math.cos(angle2);
            double y2 = y + radius + radius * Math.sin(angle2);
            triangle(x + w - radius, y + radius, x1, y1, x2, y2, color);
        }

        for (int i = 0; i < segments; i++) {
            double angle1 = i * angleStep;
            double angle2 = (i + 1) * angleStep;
            double x1 = x + w - radius + radius * Math.cos(angle1);
            double y1 = y + h - radius + radius * Math.sin(angle1);
            double x2 = x + w - radius + radius * Math.cos(angle2);
            double y2 = y + h - radius + radius * Math.sin(angle2);
            triangle(x + w - radius, y + h - radius, x1, y1, x2, y2, color);
        }

        for (int i = 0; i < segments; i++) {
            double angle1 = Math.PI / 2 + i * angleStep;
            double angle2 = Math.PI / 2 + (i + 1) * angleStep;
            double x1 = x + radius + radius * Math.cos(angle1);
            double y1 = y + h - radius + radius * Math.sin(angle1);
            double x2 = x + radius + radius * Math.cos(angle2);
            double y2 = y + h - radius + radius * Math.sin(angle2);
            triangle(x + radius, y + h - radius, x1, y1, x2, y2, color);
        }
    }

    public void drawRoundedRectCustom(double x, double y, double w, double h, double radius, Color color,
                                      boolean topLeft, boolean topRight, boolean bottomRight, boolean bottomLeft) {
        if (w <= 0 || h <= 0) return;

        // Jos pyöristyssäde on liian pieni tai yhtään kulmaa ei pyöristetä, piirrä tavallinen neliö
        if (radius <= 0.5 || (!topLeft && !topRight && !bottomRight && !bottomLeft)) {
            quad(x, y, w, h, color);
            return;
        }

        // Rajoitetaan radius, jottei se ylitä puolta sivun pituudesta
        radius = Math.min(radius, Math.min(w / 2, h / 2));

        // Määritetään efektiiviset säteet kullekin sivulle
        double rTop = (topLeft || topRight) ? radius : 0;
        double rBottom = (bottomLeft || bottomRight) ? radius : 0;
        double rLeft = (topLeft || bottomLeft) ? radius : 0;
        double rRight = (topRight || bottomRight) ? radius : 0;

        // Keskiosa
        double centerX = x + rLeft;
        double centerY = y + rTop;
        double centerW = w - rLeft - rRight;
        double centerH = h - rTop - rBottom;
        if (centerW > 0 && centerH > 0) {
            quad(centerX, centerY, centerW, centerH, color);
        }

        // Yläreuna (jos yläkulmia pyöristetään)
        if (rTop > 0) {
            double topX = x + rLeft;
            double topY = y;
            double topW = w - rLeft - rRight;
            double topH = rTop;
            if (topW > 0 && topH > 0) {
                quad(topX, topY, topW, topH, color);
            }
        }

        // Alareuna
        if (rBottom > 0) {
            double bottomX = x + rLeft;
            double bottomY = y + h - rBottom;
            double bottomW = w - rLeft - rRight;
            double bottomH = rBottom;
            if (bottomW > 0 && bottomH > 0) {
                quad(bottomX, bottomY, bottomW, bottomH, color);
            }
        }

        // Vasen reuna
        if (rLeft > 0) {
            double leftX = x;
            double leftY = y + rTop;
            double leftW = rLeft;
            double leftH = h - rTop - rBottom;
            if (leftW > 0 && leftH > 0) {
                quad(leftX, leftY, leftW, leftH, color);
            }
        }

        // Oikea reuna
        if (rRight > 0) {
            double rightX = x + w - rRight;
            double rightY = y + rTop;
            double rightW = rRight;
            double rightH = h - rTop - rBottom;
            if (rightW > 0 && rightH > 0) {
                quad(rightX, rightY, rightW, rightH, color);
            }
        }

        // Kulmat – piirretään ympyrän neljänneksinä
        int segments = Math.max(8, (int) (radius * 2));
        double angleStep = Math.PI / 2 / segments;

        // Vasen yläkulma
        if (topLeft) {
            for (int i = 0; i < segments; i++) {
                double angle1 = Math.PI + i * angleStep;
                double angle2 = Math.PI + (i + 1) * angleStep;
                double cx = x + radius;
                double cy = y + radius;
                double x1 = cx + radius * Math.cos(angle1);
                double y1 = cy + radius * Math.sin(angle1);
                double x2 = cx + radius * Math.cos(angle2);
                double y2 = cy + radius * Math.sin(angle2);
                triangle(cx, cy, x1, y1, x2, y2, color);
            }
        }

        // Oikea yläkulma
        if (topRight) {
            for (int i = 0; i < segments; i++) {
                double angle1 = Math.PI * 1.5 + i * angleStep;
                double angle2 = Math.PI * 1.5 + (i + 1) * angleStep;
                double cx = x + w - radius;
                double cy = y + radius;
                double x1 = cx + radius * Math.cos(angle1);
                double y1 = cy + radius * Math.sin(angle1);
                double x2 = cx + radius * Math.cos(angle2);
                double y2 = cy + radius * Math.sin(angle2);
                triangle(cx, cy, x1, y1, x2, y2, color);
            }
        }

        // Oikea alakulma
        if (bottomRight) {
            for (int i = 0; i < segments; i++) {
                double angle1 = i * angleStep;
                double angle2 = (i + 1) * angleStep;
                double cx = x + w - radius;
                double cy = y + h - radius;
                double x1 = cx + radius * Math.cos(angle1);
                double y1 = cy + radius * Math.sin(angle1);
                double x2 = cx + radius * Math.cos(angle2);
                double y2 = cy + radius * Math.sin(angle2);
                triangle(cx, cy, x1, y1, x2, y2, color);
            }
        }

        // Vasen alakulma
        if (bottomLeft) {
            for (int i = 0; i < segments; i++) {
                double angle1 = Math.PI / 2 + i * angleStep;
                double angle2 = Math.PI / 2 + (i + 1) * angleStep;
                double cx = x + radius;
                double cy = y + h - radius;
                double x1 = cx + radius * Math.cos(angle1);
                double y1 = cy + radius * Math.sin(angle1);
                double x2 = cx + radius * Math.cos(angle2);
                double y2 = cy + radius * Math.sin(angle2);
                triangle(cx, cy, x1, y1, x2, y2, color);
            }
        }
    }


    public void drawRoundedRectOutline(double x, double y, double width, double height, double radius, Color color, double thickness) {
        if (color.getAlpha() == 0 || thickness <= 0) return;
        radius = Math.min(radius, Math.min(width / 2, height / 2));

        int segments = Math.max(8, (int) (radius * 2));
        double angleStep = Math.PI / 2 / segments;

        // Piirretään kaaret
        // Vasen yläkulma
        double cx1 = x + radius;
        double cy1 = y + radius;
        for (int i = 0; i < segments; i++) {
            double angle1 = Math.PI + i * angleStep;
            double angle2 = Math.PI + (i + 1) * angleStep;
            double x1 = cx1 + radius * Math.cos(angle1);
            double y1 = cy1 + radius * Math.sin(angle1);
            double x2 = cx1 + radius * Math.cos(angle2);
            double y2 = cy1 + radius * Math.sin(angle2);
            lineThick(x1, y1, x2, y2, thickness, color);
        }

        // Oikea yläkulma
        double cx2 = x + width - radius;
        double cy2 = y + radius;
        for (int i = 0; i < segments; i++) {
            double angle1 = Math.PI * 1.5 + i * angleStep;
            double angle2 = Math.PI * 1.5 + (i + 1) * angleStep;
            double x1 = cx2 + radius * Math.cos(angle1);
            double y1 = cy2 + radius * Math.sin(angle1);
            double x2 = cx2 + radius * Math.cos(angle2);
            double y2 = cy2 + radius * Math.sin(angle2);
            lineThick(x1, y1, x2, y2, thickness, color);
        }

        // Oikea alakulma
        double cx3 = x + width - radius;
        double cy3 = y + height - radius;
        for (int i = 0; i < segments; i++) {
            double angle1 = i * angleStep;
            double angle2 = (i + 1) * angleStep;
            double x1 = cx3 + radius * Math.cos(angle1);
            double y1 = cy3 + radius * Math.sin(angle1);
            double x2 = cx3 + radius * Math.cos(angle2);
            double y2 = cy3 + radius * Math.sin(angle2);
            lineThick(x1, y1, x2, y2, thickness, color);
        }

        // Vasen alakulma
        double cx4 = x + radius;
        double cy4 = y + height - radius;
        for (int i = 0; i < segments; i++) {
            double angle1 = Math.PI / 2 + i * angleStep;
            double angle2 = Math.PI / 2 + (i + 1) * angleStep;
            double x1 = cx4 + radius * Math.cos(angle1);
            double y1 = cy4 + radius * Math.sin(angle1);
            double x2 = cx4 + radius * Math.cos(angle2);
            double y2 = cy4 + radius * Math.sin(angle2);
            lineThick(x1, y1, x2, y2, thickness, color);
        }

        // Piirretään suorat osat
        // Yläreuna
        lineThick(x + radius, y, x + width - radius, y, thickness, color);
        // Oikea reuna
        lineThick(x + width, y + radius, x + width, y + height - radius, thickness, color);
        // Alareuna
        lineThick(x + width - radius, y + height, x + radius, y + height, thickness, color);
        // Vasen reuna
        lineThick(x, y + height - radius, x, y + radius, thickness, color);
    }

    public void texQuad(double x, double y, double width, double height, double rotation, TextureRegion region, Color color) {
        texQuad(x, y, width, height, rotation, region.x1, region.y1, region.x2, region.y2, color);
    }
}