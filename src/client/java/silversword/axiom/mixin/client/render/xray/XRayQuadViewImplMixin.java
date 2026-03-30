package silversword.axiom.mixin.client.render.xray;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.QuadViewImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import silversword.axiom.client.modules.render.XRay;

@Mixin(QuadViewImpl.class)
public class XRayQuadViewImplMixin {

    @ModifyReturnValue(method = "color(I)I", at = @At("RETURN"))
    private int modifyColor(int originalColor) {
        if (XRay.isCurrentBlockHidden()) {
            float hiddenAlpha = XRay.getHiddenAlpha();
            if (hiddenAlpha > 0.0f && hiddenAlpha < 1.0f) {
                int alpha = (int)(hiddenAlpha * 255);
                return (originalColor & 0x00FFFFFF) | (alpha << 24);
            }
        }
        return originalColor;
    }
}