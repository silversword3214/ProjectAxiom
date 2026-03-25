package silversword.axiom.client.render.rendersystem.axiomrenderer.core;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;

import static com.mojang.text2speech.Narrator.LOGGER;

/**
 * Hallinnoi pyöreää puskuria (ring buffer) vertex-datalle.
 * Useat puskurit ja aitaus (fence) estävät GPU:n ja CPU:n törmäyksen.
 * Koko skaalautuu automaattisesti tarvittaessa.
 */
public class VertexBufferManager implements AutoCloseable {
    private static final int BUFFER_COUNT = 4;
    private static final String[] BUFFER_NAMES = {
            "axiomrenderer_vertex_buffer_0",
            "axiomrenderer_vertex_buffer_1",
            "axiomrenderer_vertex_buffer_3",
            "axiomrenderer_vertex_buffer_4",
            "axiomrenderer_vertex_buffer_5",
    };

    private final GpuBuffer[] buffers = new GpuBuffer[BUFFER_COUNT];
    private final int[] bufferSizes = new int[BUFFER_COUNT];
    private final GpuFence[] fences = new GpuFence[BUFFER_COUNT];
    private int currentIndex = 0;

    public VertexBufferManager() {
        // Alustetaan fence-taulukko null-arvoilla
        for (int i = 0; i < BUFFER_COUNT; i++) {
            fences[i] = null;
        }
    }

    /**
     * Varmistaa, että nykyinen puskuri on vähintään requiredSize-tavuinen.
     * Odottaa tarvittaessa, kunnes edellinen käyttökerta on valmis (fence).
     * Jos puskuri on liian pieni, luodaan uusi isompi.
     */
    public void ensureCapacity(int requiredSize) {
        // Odota, että edellinen käyttökerta on valmis (blokkaa)
        if (fences[currentIndex] != null) {
            // Try to wait up to 16 milliseconds (1 frame at 60 fps)
            boolean completed = fences[currentIndex].awaitCompletion(16_000_000L); // 16 ms in nanoseconds
            if (!completed) {
                LOGGER.warn("Fence timeout for buffer {}. Proceeding anyway.", currentIndex);
            }
            fences[currentIndex].close();
            fences[currentIndex] = null;
        }

        if (buffers[currentIndex] == null || bufferSizes[currentIndex] < requiredSize) {
            if (buffers[currentIndex] != null) {
                buffers[currentIndex].close();
            }
            int newSize = Math.max(requiredSize, (int)(bufferSizes[currentIndex] * 1.5));
            if (newSize < requiredSize) newSize = requiredSize;
            if (newSize < 1024) newSize = 1024;

            buffers[currentIndex] = RenderSystem.getDevice().createBuffer(
                    () -> BUFFER_NAMES[currentIndex],
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE,
                    newSize
            );
            bufferSizes[currentIndex] = newSize;
        }
    }

    /**
     * Loads given data to current buffer
     * Assumes that buffer got space
     */
    public void upload(ByteBuffer data, int size, CommandEncoder encoder) {
        GpuBuffer currentBuffer = buffers[currentIndex];
        if (currentBuffer == null) {
            throw new IllegalStateException("Buffer not allocated");
        }
        try (GpuBuffer.MappedView mapped = encoder.mapBuffer(currentBuffer.slice(0, size), false, true)) {
            ByteBuffer target = mapped.data();
            data.rewind();
            target.put(data);
        }
    }

    /**
     * Sets a fence for the current buffer
     * Called after buffer using was sent to GPU
     */
    public void setFence(GpuFence fence) {
        fences[currentIndex] = fence;
    }

    /** Moves to the next buffer */
    public void rotate() {
        currentIndex = (currentIndex + 1) % BUFFER_COUNT;
    }

    /** Returns the current buffer */
    public GpuBuffer getCurrentBuffer() {
        return buffers[currentIndex];
    }

    @Override
    public void close() {
        long timeoutNanos = 1_000_000_000L; // 1 second
        for (int i = 0; i < BUFFER_COUNT; i++) {
            if (fences[i] != null) {
                boolean completed = fences[i].awaitCompletion(timeoutNanos);
                if (!completed) {
                    LOGGER.warn("Fence {} did not complete within 1s – forcing close", i);
                }
                fences[i].close();
                fences[i] = null;
            }
            if (buffers[i] != null) {
                buffers[i].close();
                buffers[i] = null;
            }
        }
    }
}