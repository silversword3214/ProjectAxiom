package silversword.axiom.client.render.rendersystem;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.BufferUtils;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.render.RenderUtils;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.*;
import static silversword.axiom.client.main.AxiomInitialize.mc;

public class VertexBufferBuilder {
    private final VertexFormat format;
    private final int primitiveVerticesSize;

    private ByteBuffer vertices;
    private long verticesPointerStart, verticesPointer;

    private ByteBuffer indices;
    private long indicesPointer;

    private int vertexI, indicesCount;
    private boolean building;
    private double cameraX, cameraY, cameraZ;

    private static final boolean DEBUG = FabricLoader.getInstance().isDevelopmentEnvironment() || Boolean.getBoolean("obsidian.render.debug");

    public double alpha = 1;

    public VertexBufferBuilder(RenderPipeline pipeline) {
        this.format = pipeline.getVertexFormat();
        this.primitiveVerticesSize = format.getVertexSize();
        allocateBuffers(256 * 4, 512 * 4);
    }

    public VertexBufferBuilder(VertexFormat format, VertexFormat.Mode drawMode, int maxVertices, int maxIndices) {
        this.format = format;
        this.primitiveVerticesSize = format.getVertexSize();
        allocateBuffers(maxVertices, maxIndices);
    }

    public void begin() {
        building = true;
        verticesPointer = verticesPointerStart;
        vertexI = 0;
        indicesCount = 0;

        if (RenderUtils.rendering3D) {
            Vec3 camera = mc.gameRenderer.getMainCamera().position();
            cameraX = camera.x;
            cameraY = camera.y;
            cameraZ = camera.z;
        } else {
            cameraX = 0;
            cameraY = 0;
            cameraZ = 0;
        }
    }

    public VertexBufferBuilder uv(float u, float v) {
        ensureCapacity(1, 0);
        long p = verticesPointer;
        memPutFloat(p, u);
        memPutFloat(p + 4, v);
        verticesPointer += 8;
        return this;
    }

    public VertexBufferBuilder vec3(double x, double y, double z) {
        ensureCapacity(1, 0);
        long p = verticesPointer;

        memPutFloat(p, (float) (x - cameraX));
        memPutFloat(p + 4, (float) (y - cameraY));
        memPutFloat(p + 8, (float) (z - cameraZ));

        verticesPointer += 12;
        return this;
    }

    public VertexBufferBuilder vec2(double x, double y) {
        ensureCapacity(1, 0);
        long p = verticesPointer;
        memPutFloat(p, (float) x);
        memPutFloat(p + 4, (float) y);
        verticesPointer += 8;
        return this;
    }

    public VertexBufferBuilder color(Color c) {
        ensureCapacity(1, 0);
        long p = verticesPointer;
        memPutByte(p, (byte) c.r);
        memPutByte(p + 1, (byte) c.g);
        memPutByte(p + 2, (byte) c.b);
        memPutByte(p + 3, (byte) Math.round(c.a * alpha));
        verticesPointer += 4;
        return this;
    }

    public int next() {
        return vertexI++;
    }

    public void line(int i1, int i2) {
        ensureCapacity(0, 2);
        long p = indicesPointer + (long) indicesCount * 4;
        memPutInt(p, i1);
        memPutInt(p + 4, i2);
        indicesCount += 2;
    }

    public void triangle(int i1, int i2, int i3) {
        ensureCapacity(0, 3);
        long p = indicesPointer + (long) indicesCount * 4;
        memPutInt(p, i1);
        memPutInt(p + 4, i2);
        memPutInt(p + 8, i3);
        indicesCount += 3;
    }

    public void quad(int i1, int i2, int i3, int i4) {
        ensureCapacity(0, 6);
        long p = indicesPointer + (long) indicesCount * 4;
        memPutInt(p, i1);
        memPutInt(p + 4, i2);
        memPutInt(p + 8, i3);
        memPutInt(p + 12, i3);
        memPutInt(p + 16, i4);
        memPutInt(p + 20, i1);
        indicesCount += 6;
    }

    public void ensureQuadCapacity() {
        ensureCapacity(4, 6);
    }

    public void ensureTriCapacity() {
        ensureCapacity(3, 3);
    }

    public void ensureLineCapacity() {
        ensureCapacity(2, 2);
    }

    public void ensureCapacity(int vertexCount, int indexCount) {
        if (vertices == null || (vertexI + vertexCount) * primitiveVerticesSize >= vertices.capacity()) {
            int currentCap = vertices == null ? 0 : vertices.capacity();
            int newSize = Math.max(2048, currentCap * 2);
            ByteBuffer newBuf = BufferUtils.createByteBuffer(newSize);
            if (vertices != null) {
                memCopy(memAddress(vertices), memAddress(newBuf), (long) vertexI * primitiveVerticesSize);
            }
            vertices = newBuf;
            verticesPointerStart = memAddress(vertices);
            verticesPointer = verticesPointerStart + (long) vertexI * primitiveVerticesSize;
        }

        if (indices == null || (indicesCount + indexCount) * 4 >= indices.capacity()) {
            int currentCap = indices == null ? 0 : indices.capacity();
            int newSize = Math.max(2048, currentCap * 2);
            ByteBuffer newBuf = BufferUtils.createByteBuffer(newSize);
            if (indices != null) {
                memCopy(memAddress(indices), memAddress(newBuf), (long) indicesCount * 4);
            }
            indices = newBuf;
            indicesPointer = memAddress(indices);
        }
    }

    private void allocateBuffers(int vCount, int iCount) {
        vertices = BufferUtils.createByteBuffer(vCount * primitiveVerticesSize);
        verticesPointerStart = verticesPointer = memAddress(vertices);
        indices = BufferUtils.createByteBuffer(iCount * 4);
        indicesPointer = memAddress(indices);
    }

    public void end() {
        building = false;
    }

    public GpuBuffer getVertexBuffer() {
        if (vertices == null || vertexI == 0) return null;
        ByteBuffer slice = vertices.duplicate();
        slice.position(0);
        slice.limit(vertexI * primitiveVerticesSize);
        return format.uploadImmediateVertexBuffer(slice);
    }

    public GpuBuffer getIndexBuffer() {
        if (indices == null || indicesCount == 0) return null;
        ByteBuffer slice = indices.duplicate();
        slice.position(0);
        slice.limit(indicesCount * 4);
        return format.uploadImmediateIndexBuffer(slice);
    }

    public int getIndicesCount() { return indicesCount; }
    public boolean isBuilding() { return building; }
    public VertexFormat getFormat() { return format; }

    public int getVertexCount() { return vertexI; }
}