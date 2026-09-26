package silversword.axiom.client.rendersystem.axiomrenderer.engine;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.GpuFence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Renkaanmuotoinen vertex-puskurien hallinta.
 *
 * Käyttää SLOT_COUNT kappaletta isoja puskureita. Jokaisen slotin sisällä
 * on lineaarinen kirjoitusosoitin. Kun slotti täyttyy, siirrytään seuraavaan.
 * Fence odotetaan vain kun slottiin palataan (eli edellisen framen draw't
 * on saatava valmiiksi ennen ylikirjoitusta). Näin vältetään
 * yksi-puskuri-per-batch -ongelma ja 16 ms blokki.
 */
public final class VertexBufferManager implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(VertexBufferManager.class);

    private static final int SLOT_COUNT = 8;
    private static final int SLOT_SIZE  = 8 * 1024 * 1024; // 8 MB / slotti => 64 MB total

    private final GpuBuffer[] buffers      = new GpuBuffer[SLOT_COUNT];
    private final int[]       capacities   = new int[SLOT_COUNT];
    private final int[]       writeOffsets = new int[SLOT_COUNT];
    private final GpuFence[]  fences       = new GpuFence[SLOT_COUNT];

    private int currentSlot = 0;

    private final int[] usedSlots = new int[SLOT_COUNT];
    private int usedSlotCount = 0;

    public VertexBufferManager() {}

    public void beginFrame() {
        usedSlotCount = 0;
    }

    /**
     * Allokoi {@code size} tavua nykyisestä slotista, kirjoittaa {@code src}:n
     * (position=0, limit=size) ja palauttaa slicen.
     */
    public GpuBufferSlice upload(ByteBuffer src, int size, CommandEncoder encoder) {
        if (size <= 0) throw new IllegalArgumentException("size <= 0");

        while (true) {
            if (buffers[currentSlot] == null) {
                allocateSlot(currentSlot, size);
            }

            int off = writeOffsets[currentSlot];
            if (off + size <= capacities[currentSlot]) {
                GpuBufferSlice slice = buffers[currentSlot].slice(off, size);
                encoder.writeToBuffer(slice, src);
                writeOffsets[currentSlot] = off + size;
                trackSlot(currentSlot);
                return slice;
            }
            advanceSlot(size);
        }
    }

    private void trackSlot(int slot) {
        for (int i = 0; i < usedSlotCount; i++) {
            if (usedSlots[i] == slot) return;
        }
        if (usedSlotCount < SLOT_COUNT) usedSlots[usedSlotCount++] = slot;
    }

    private void allocateSlot(int slot, int required) {
        int cap = Math.max(required, SLOT_SIZE);
        buffers[slot]     = createBuffer(cap, slot);
        capacities[slot]  = cap;
        writeOffsets[slot] = 0;
    }

    private void advanceSlot(int required) {
        currentSlot = (currentSlot + 1) % SLOT_COUNT;

        if ((usedSlotCount == SLOT_COUNT) && usedSlots[SLOT_COUNT - 1] == currentSlot) {
            LOGGER.warn("Vertex buffer frame exceeded {} MB — data corruption possible",
                    (SLOT_COUNT * SLOT_SIZE) / (1024 * 1024));
        }

        GpuFence f = fences[currentSlot];
        if (f != null) {
            if (!f.awaitCompletion(0)) {
                f.awaitCompletion(50_000_000L);
            }
            f.close();
            fences[currentSlot] = null;
        }
        writeOffsets[currentSlot] = 0;

        if (buffers[currentSlot] == null) {
            allocateSlot(currentSlot, required);
        } else if (capacities[currentSlot] < required) {
            int cap = Math.max(required, capacities[currentSlot] * 2);
            GpuBuffer old = buffers[currentSlot];
            buffers[currentSlot]    = createBuffer(cap, currentSlot);
            capacities[currentSlot] = cap;
            if (old != null) old.close();
        }
    }

    private GpuBuffer createBuffer(int size, int index) {
        return RenderSystem.getDevice().createBuffer(
                () -> "axiomrenderer_vertex_buffer_" + index,
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_COPY_DST,
                size);
    }

    public void setFenceAll(GpuFence fence) {
        for (int i = 0; i < usedSlotCount; i++) {
            fences[usedSlots[i]] = fence;
        }
    }

    @Override
    public void close() {
        long timeout = 1_000_000_000L;
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (fences[i] != null) {
                fences[i].awaitCompletion(timeout);
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