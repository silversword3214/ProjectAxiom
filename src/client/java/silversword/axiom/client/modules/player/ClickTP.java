package silversword.axiom.client.modules.player;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Camera;
import net.minecraft.world.item.BlockItem;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.ClipContext;
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
        if (mc.player == null || mc.level == null) return;
        if (!mc.options.keyUse.isDown()) return;

        // Jos osoitetaan entityyn ja interaktio onnistuu, älä teleporttaa
        if (mc.hitResult != null) {
            if (mc.hitResult.getType() == HitResult.Type.ENTITY &&
                    mc.player.interactOn(((EntityHitResult) mc.hitResult).getEntity(), InteractionHand.MAIN_HAND) != InteractionResult.PASS) {
                return;
            }
            // Jos osoitetaan blockiin ja kädessä on BlockItem, älä teleporttaa (anna sijoittaa blocki)
            if (mc.hitResult.getType() == HitResult.Type.BLOCK &&
                    mc.player.getMainHandItem().getItem() instanceof BlockItem) {
                return;
            }
        }

        // Kamera ja raycast
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();

        // Lasketaan katsesuunta ja kerrotaan 210:llä (max kantama)
        Vec3 direction = Vec3.directionFromRotation(camera.xRot(), camera.yRot()).scale(210);
        Vec3 targetPos = cameraPos.add(direction);

        ClipContext context = new ClipContext(
                cameraPos,
                targetPos,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                mc.player
        );

        BlockHitResult hitResult = mc.level.clip(context);

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hitResult.getBlockPos();
            Direction side = hitResult.getDirection();

            if (mc.level.getBlockState(pos).useWithoutItem(mc.level, mc.player, hitResult) != InteractionResult.PASS) return;

            BlockState state = mc.level.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(mc.level, pos);
            if (shape.isEmpty()) shape = state.getShape(mc.level, pos);

            double height = shape.isEmpty() ? 1 : shape.max(Direction.Axis.Y);

            Vec3 newPos = new Vec3(
                    pos.getX() + 0.5 + side.getStepX(),
                    pos.getY() + height,
                    pos.getZ() + 0.5 + side.getStepZ()
            );


            double distance = mc.player.position().distanceTo(newPos);
            int packetsRequired = (int) Math.ceil(distance / 10) - 1;
            if (packetsRequired > 19) packetsRequired = 0;

            for (int i = 0; i < packetsRequired; i++) {
                mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true, true));
            }

            // Lähetetään varsinainen liikepaketti
            mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(newPos.x, newPos.y, newPos.z, true, true));
            mc.player.setPos(newPos);
        }
    }
}