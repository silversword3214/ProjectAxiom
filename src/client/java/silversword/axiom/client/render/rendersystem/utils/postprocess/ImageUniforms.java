package silversword.axiom.client.render.rendersystem.utils.postprocess;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import net.minecraft.client.gl.DynamicUniformStorage;

import java.nio.ByteBuffer;

public class ImageUniforms {
    private static final int UNIFORM_SIZE = new Std140SizeCalculator()
            .putVec4()
            .get();

    private static final DynamicUniformStorage<Data> STORAGE = new DynamicUniformStorage<>("Obsidian - Image UBO", UNIFORM_SIZE, 16);

    public static void flipFrame() {
        STORAGE.clear();
    }

    public static GpuBufferSlice write(float r, float g, float b, float a) {
        return STORAGE.write(new Data(r, g, b, a));
    }

    private record Data(float r, float g, float b, float a) implements DynamicUniformStorage.Uploadable {
        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                    .putVec4(r, g, b, a);
        }
    }
}