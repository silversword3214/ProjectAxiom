package silversword.axiom.client.render.rendersystem.axiomrenderer.core;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;

/**
 * Hallinnoi pyöreää puskuria (ring buffer) vertex-datalle.
 * Useat puskurit ja aitaus (fence) estävät GPU:n ja CPU:n törmäyksen.
 * Koko skaalautuu automaattisesti tarvittaessa.
 */
public class VertexBufferManager implements AutoCloseable {
    private static final int BUFFER_COUNT = 2;
    private static final String[] BUFFER_NAMES = {
            "axiomrenderer_vertex_buffer_0",
            "axiomrenderer_vertex_buffer_1",
            "axiomrenderer_vertex_buffer_3"
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
            // Odota ikuisesti, kunnes GPU on valmis
            while (!fences[currentIndex].awaitCompletion(Long.MAX_VALUE)) {
                // odota, kunnes fence saavutetaan (ei pitäisi palauttaa false koska timeout on maksimi)
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
     * Lataa annetun datan nykyiseen puskuriin.
     * Oletetaan, että puskurissa on riittävästi tilaa (ensureCapacity kutsuttu).
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
     * Asettaa aitauksen (fence) nykyiselle puskurille.
     * Kutsutaan sen jälkeen, kun puskurin käyttö on lähetetty GPU:lle.
     */
    public void setFence(GpuFence fence) {
        fences[currentIndex] = fence;
    }

    /** Siirtyy seuraavaan puskuriin (ring buffer -kierto). */
    public void rotate() {
        currentIndex = (currentIndex + 1) % BUFFER_COUNT;
    }

    /** Palauttaa nykyisen puskurin (se, johon juuri kirjoitettiin). */
    public GpuBuffer getCurrentBuffer() {
        return buffers[currentIndex];
    }

    @Override
    public void close() {
        for (int i = 0; i < BUFFER_COUNT; i++) {
            if (fences[i] != null) {
                fences[i].awaitCompletion(0);
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