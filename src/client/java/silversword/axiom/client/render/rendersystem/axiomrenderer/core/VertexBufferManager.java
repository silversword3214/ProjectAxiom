package silversword.axiom.client.render.rendersystem.axiomrenderer.core;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import static com.mojang.text2speech.Narrator.LOGGER;

public class VertexBufferManager implements AutoCloseable {
    private static final int BUFFER_COUNT = 64;
    private static final int INITIAL_SIZE = 8 * 1024 * 1024;
    private static final String[] BUFFER_NAMES = {
            "axiomrenderer_vertex_buffer_1",
            "axiomrenderer_vertex_buffer_2",
            "axiomrenderer_vertex_buffer_3",
            "axiomrenderer_vertex_buffer_4",
            "axiomrenderer_vertex_buffer_5",
            "axiomrenderer_vertex_buffer_6",
            "axiomrenderer_vertex_buffer_7",
            "axiomrenderer_vertex_buffer_8",
            "axiomrenderer_vertex_buffer_9",
            "axiomrenderer_vertex_buffer_10",
            "axiomrenderer_vertex_buffer_11",
            "axiomrenderer_vertex_buffer_12",
            "axiomrenderer_vertex_buffer_13",
            "axiomrenderer_vertex_buffer_14",
            "axiomrenderer_vertex_buffer_15",
            "axiomrenderer_vertex_buffer_16",
            "axiomrenderer_vertex_buffer_17",
            "axiomrenderer_vertex_buffer_18",
            "axiomrenderer_vertex_buffer_19",
            "axiomrenderer_vertex_buffer_20",
            "axiomrenderer_vertex_buffer_21",
            "axiomrenderer_vertex_buffer_22",
            "axiomrenderer_vertex_buffer_23",
            "axiomrenderer_vertex_buffer_24",
            "axiomrenderer_vertex_buffer_25",
            "axiomrenderer_vertex_buffer_26",
            "axiomrenderer_vertex_buffer_27",
            "axiomrenderer_vertex_buffer_28",
            "axiomrenderer_vertex_buffer_29",
            "axiomrenderer_vertex_buffer_30",
            "axiomrenderer_vertex_buffer_31",
            "axiomrenderer_vertex_buffer_32",
            "axiomrenderer_vertex_buffer_33",
            "axiomrenderer_vertex_buffer_34",
            "axiomrenderer_vertex_buffer_35",
            "axiomrenderer_vertex_buffer_36",
            "axiomrenderer_vertex_buffer_37",
            "axiomrenderer_vertex_buffer_38",
            "axiomrenderer_vertex_buffer_39",
            "axiomrenderer_vertex_buffer_40",
            "axiomrenderer_vertex_buffer_41",
            "axiomrenderer_vertex_buffer_42",
            "axiomrenderer_vertex_buffer_43",
            "axiomrenderer_vertex_buffer_44",
            "axiomrenderer_vertex_buffer_45",
            "axiomrenderer_vertex_buffer_46",
            "axiomrenderer_vertex_buffer_47",
            "axiomrenderer_vertex_buffer_48",
            "axiomrenderer_vertex_buffer_49",
            "axiomrenderer_vertex_buffer_50",
            "axiomrenderer_vertex_buffer_51",
            "axiomrenderer_vertex_buffer_52",
            "axiomrenderer_vertex_buffer_53",
            "axiomrenderer_vertex_buffer_54",
            "axiomrenderer_vertex_buffer_55",
            "axiomrenderer_vertex_buffer_56",
            "axiomrenderer_vertex_buffer_57",
            "axiomrenderer_vertex_buffer_58",
            "axiomrenderer_vertex_buffer_59",
            "axiomrenderer_vertex_buffer_60",
            "axiomrenderer_vertex_buffer_61",
            "axiomrenderer_vertex_buffer_62",
            "axiomrenderer_vertex_buffer_63",
            "axiomrenderer_vertex_buffer_64",

    };

    private final GpuBuffer[] buffers = new GpuBuffer[BUFFER_COUNT];
    private final int[] bufferSizes = new int[BUFFER_COUNT];
    private final GpuFence[] fences = new GpuFence[BUFFER_COUNT];
    private int currentIndex = 0;

    public VertexBufferManager() {
        for (int i = 0; i < BUFFER_COUNT; i++) {
            buffers[i] = createBuffer(INITIAL_SIZE, i);
            bufferSizes[i] = INITIAL_SIZE;
            fences[i] = null;
        }
    }

    private GpuBuffer createBuffer(int size, int index) {
        return RenderSystem.getDevice().createBuffer(
                () -> BUFFER_NAMES[index],
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE,
                size
        );
    }

    /**
     * Non‑blocking: finds a free buffer and ensures it's big enough.
     * Only blocks if every buffer is still busy (extremely rare).
     */
    public void ensureCapacity(int requiredSize) {
        // Try all buffers for a free one
        for (int i = 0; i < BUFFER_COUNT; i++) {
            int idx = (currentIndex + i) % BUFFER_COUNT;
            if (fences[idx] == null || fences[idx].awaitCompletion(0)) {
                if (fences[idx] != null) {
                    fences[idx].close();
                    fences[idx] = null;
                }
                currentIndex = idx;
                // Resize if needed
                if (bufferSizes[currentIndex] < requiredSize) {
                    int newSize = Math.max(requiredSize, (int)(bufferSizes[currentIndex] * 1.5));
                    if (newSize < 1024) newSize = 1024;
                    GpuBuffer old = buffers[currentIndex];
                    buffers[currentIndex] = createBuffer(newSize, currentIndex);
                    bufferSizes[currentIndex] = newSize;
                    if (old != null) old.close();
                }
                return;
            }
        }

        // All buffers are busy – fallback (should almost never happen)
        LOGGER.warn("All vertex buffers busy, waiting for index {}", currentIndex);
        // Korvaa odottava osa tällä:
        if (fences[currentIndex] != null) {
            fences[currentIndex].awaitCompletion(16_000_000L); // non-blocking
            if (fences[currentIndex].awaitCompletion(0)) {
                fences[currentIndex].close();
                fences[currentIndex] = null;
            } else {
                // Kaikki puskurit varattuja – kasvata tilapäisesti puskurien määrää?
                // Tai yksinkertaisesti lisää BUFFER_COUNT arvoa.
                LOGGER.warn("All vertex buffers busy, consider increasing BUFFER_COUNT");
            }
        }
        // Resize if needed (same logic)
        if (bufferSizes[currentIndex] < requiredSize) {
            int newSize = Math.max(requiredSize, (int)(bufferSizes[currentIndex] * 1.5));
            if (newSize < 1024) newSize = 1024;
            GpuBuffer old = buffers[currentIndex];
            buffers[currentIndex] = createBuffer(newSize, currentIndex);
            bufferSizes[currentIndex] = newSize;
            if (old != null) old.close();
        }
    }

    public void upload(ByteBuffer data, int size, CommandEncoder encoder) {
        GpuBuffer currentBuffer = buffers[currentIndex];
        if (currentBuffer == null) throw new IllegalStateException("Buffer not allocated");
        try (GpuBuffer.MappedView mapped = encoder.mapBuffer(currentBuffer.slice(0, size), false, true)) {
            ByteBuffer target = mapped.data();
            data.rewind();
            target.put(data);
        }
    }

    public void setFence(GpuFence fence) {
        fences[currentIndex] = fence;
    }

    public void rotate() {
        currentIndex = (currentIndex + 1) % BUFFER_COUNT;
    }

    public GpuBuffer getCurrentBuffer() {
        return buffers[currentIndex];
    }

    @Override
    public void close() {
        long timeoutNanos = 1_000_000_000L; // 1 second
        for (int i = 0; i < BUFFER_COUNT; i++) {
            if (fences[i] != null) {
                fences[i].awaitCompletion(timeoutNanos);
                fences[i].close();
            }
            if (buffers[i] != null) buffers[i].close();
        }
    }
}