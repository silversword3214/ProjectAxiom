package silversword.axiom.mixin.client;

import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import com.mojang.blaze3d.resource.ResourceHandle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import silversword.axiom.client.mixininterface.IWorldRenderer;


import java.util.ArrayDeque;
import java.util.Deque;


@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin implements IWorldRenderer {
    // Shader ESP

    @Shadow
    private RenderTarget entityOutlineTarget;

    @Shadow @Final private LevelTargetBundle targets;
    @Unique private final Stack<ResourceHandle<RenderTarget>> framebufferHandleStack = new ObjectArrayList<>();
    @Unique
    @Final
    private EntityRenderDispatcher entityRenderManager;

    @Unique
    private final ThreadLocal<Deque<RenderTarget>> framebufferStack = ThreadLocal.withInitial(ArrayDeque::new);

   



    @Override
    public void axiom$pushEntityOutlineFramebuffer(RenderTarget fb) {
        framebufferStack.get().push(this.entityOutlineTarget);
        this.entityOutlineTarget = fb;

        framebufferHandleStack.push(this.targets.entityOutline);
        this.targets.entityOutline = () -> fb;
    }

    @Override
    public void axiom$popEntityOutlineFramebuffer() {
        Deque<RenderTarget> stack = framebufferStack.get();
        if (!stack.isEmpty()) {
            this.entityOutlineTarget = stack.pop();
            this.targets.entityOutline = framebufferHandleStack.pop();
        }
    }
}