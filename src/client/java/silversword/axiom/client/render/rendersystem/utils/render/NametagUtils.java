package silversword.axiom.client.render.rendersystem.utils.render;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.modules.render.NoHurtCam;
import silversword.axiom.client.modules.render.NoViewBobbingTilt;

public final class NametagUtils {
    private static final Minecraft mc = Minecraft.getInstance();

    public static Vec3 worldToScreen(Vec3 worldPos) {
        var camera = mc.gameRenderer.getMainCamera();
        float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

        // 1. Luodaan Bobbing/Hurt -korjausmatriisi
        Matrix4f bobCorrection = new Matrix4f();
        applyBobbing(bobCorrection, tickDelta);

        // 2. Perusnäkymämatriisi (Kameran rotaatio)
        Matrix4f view = new Matrix4f().rotation(camera.rotation());


        bobCorrection.mul(view);

        // 3. Haetaan projektio
        Matrix4f proj = RenderUtils.getProjectionMatrix(tickDelta);

        // 4. Lasketaan suhteellinen sijainti (Maailma -> Kamera-avaruus)
        Vector4f clip = new Vector4f(
                (float) (worldPos.x - camera.position().x),
                (float) (worldPos.y - camera.position().y),
                (float) (worldPos.z - camera.position().z),
                1.0f
        );

        // 5. Muunnos: Clip-avaruuteen
        clip.mul(bobCorrection); // Sisältää nyt sekä heilunnan että kameran suunnan
        clip.mul(proj);

        if (clip.w <= 0.0f) return null;

        // 6. Normalisointi ja skaalaus ruudulle
        float invW = 1.0f / clip.w;
        float ndcX = clip.x * invW;
        float ndcY = clip.y * invW;

        int scaledWidth = mc.getWindow().getGuiScaledWidth();
        int scaledHeight = mc.getWindow().getGuiScaledHeight();

        float screenX = (ndcX * 0.5f + 0.5f) * scaledWidth;
        float screenY = (1.0f - (ndcY * 0.5f + 0.5f)) * scaledHeight;

        return new Vec3(screenX, screenY, clip.w);
    }

    private static void applyBobbing(Matrix4f matrix, float tickDelta) {
        NoViewBobbingTilt bobMod = ModuleManager.getInstance().getModule(NoViewBobbingTilt.class);
        NoHurtCam hurtMod = ModuleManager.getInstance().getModule(NoHurtCam.class);

        boolean bobbingCancelled = (bobMod != null && bobMod.isEnabled());
        boolean hurtCancelled = (hurtMod != null && hurtMod.isEnabled()) || (bobMod != null && bobMod.isEnabled() && bobMod.disableHurtTilt.get());

        if (mc.getCameraEntity() instanceof net.minecraft.client.player.AbstractClientPlayer player) {
            // VIEW BOBBING
            if (mc.options.bobView().get() && !bobbingCancelled) {
                var state = player.avatarState();
                float g = state.getBackwardsInterpolatedWalkDistance(tickDelta);
                float h = state.getInterpolatedBob(tickDelta);

                float translateX = (float) Math.sin(g * (float) Math.PI) * h * 0.5F;
                float translateY = Math.abs((float) Math.cos(g * (float) Math.PI) * h);
                float rotateZ = (float) Math.sin(g * (float) Math.PI) * h * 3.0F;
                float rotateX = Math.abs((float) Math.cos(g * (float) Math.PI - 0.2F) * h) * 5.0F;

                matrix.rotateX(rotateX * 0.017453292F);
                matrix.rotateZ(rotateZ * 0.017453292F);
                matrix.translate(translateX, -translateY, 0.0F);
            }

            // HURT TILT
            if (mc.options.damageTiltStrength().get() > 0 && !hurtCancelled) {
                float g = (float) player.hurtTime - tickDelta;
                if (g >= 0.0F) {
                    g /= (float) player.hurtDuration;
                    g = net.minecraft.util.Mth.sin(g * g * g * g * (float) Math.PI);
                    float h = player.getHurtDir();

                    // LISÄTTY mc.options.damageTiltStrength().get().floatValue() kertoimeksi
                    float tiltStrength = mc.options.damageTiltStrength().get().floatValue();

                    matrix.rotateY(-h * 0.017453292F);
                    matrix.rotateZ(-g * 14.0F * tiltStrength * 0.017453292F);
                    matrix.rotateY(h * 0.017453292F);
                }
            }
        }
    }
}