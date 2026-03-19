package silversword.axiom.client.modules.player;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.Camera;
import net.minecraft.item.BlockItem;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;
import org.lwjgl.glfw.GLFW;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class ClickTP extends AxiomMod implements KeybindConfigurable {
    private final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", GLFW.GLFW_KEY_UNKNOWN);

    public ClickTP() {
        super("ClickTP", "Teleports you to the block you click on", ModuleCategory.MOVEMENT);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {}

    @Override
    protected void onTick() {
        if (mc.player == null || mc.world == null) return;
        if (!mc.options.useKey.isPressed()) return;

        // Jos osoitetaan entityyn ja interaktio onnistuu, älä teleporttaa
        if (mc.crosshairTarget != null) {
            if (mc.crosshairTarget.getType() == HitResult.Type.ENTITY &&
                    mc.player.interact(((EntityHitResult) mc.crosshairTarget).getEntity(), Hand.MAIN_HAND) != ActionResult.PASS) {
                return;
            }
            // Jos osoitetaan blockiin ja kädessä on BlockItem, älä teleporttaa (anna sijoittaa blocki)
            if (mc.crosshairTarget.getType() == HitResult.Type.BLOCK &&
                    mc.player.getMainHandStack().getItem() instanceof BlockItem) {
                return;
            }
        }

        // Kamera ja raycast
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPos = camera.getCameraPos();

        // Lasketaan katsesuunta ja kerrotaan 210:llä (max kantama)
        Vec3d direction = Vec3d.fromPolar(camera.getPitch(), camera.getYaw()).multiply(210);
        Vec3d targetPos = cameraPos.add(direction);

        RaycastContext context = new RaycastContext(
                cameraPos,
                targetPos,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                mc.player
        );

        BlockHitResult hitResult = mc.world.raycast(context);

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hitResult.getBlockPos();
            Direction side = hitResult.getSide();

            if (mc.world.getBlockState(pos).onUse(mc.world, mc.player, hitResult) != ActionResult.PASS) return;

            BlockState state = mc.world.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(mc.world, pos);
            if (shape.isEmpty()) shape = state.getOutlineShape(mc.world, pos);

            double height = shape.isEmpty() ? 1 : shape.getMax(Direction.Axis.Y);

            Vec3d newPos = new Vec3d(
                    pos.getX() + 0.5 + side.getOffsetX(),
                    pos.getY() + height,
                    pos.getZ() + 0.5 + side.getOffsetZ()
            );


            double distance = mc.player.getEntityPos().distanceTo(newPos);
            int packetsRequired = (int) Math.ceil(distance / 10) - 1;
            if (packetsRequired > 19) packetsRequired = 0;

            for (int i = 0; i < packetsRequired; i++) {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, true));
            }

            // Lähetetään varsinainen liikepaketti
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(newPos.x, newPos.y, newPos.z, true, true));
            mc.player.setPosition(newPos);
        }
    }
}