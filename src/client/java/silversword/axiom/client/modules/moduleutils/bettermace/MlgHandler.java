package silversword.axiom.client.modules.moduleutils.bettermace;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.main.AxiomInitialize;

import static silversword.axiom.client.main.AxiomInitialize.mc;

/**
 * Keskitetty MLG (Master Link Golf) -logiikka.
 * Käyttää raycastia tarkkaan etäisyyden laskentaan ja aktivoi MLG-itemin juuri ennen osumaa.
 */
public class MlgHandler {

    private static final double MAX_FALL_SPEED = 30.0;

    private boolean mlgTriggered = false;
    private int mlgCooldownTicks = 0;

    public boolean tick() {
        if (mc.player == null || mc.level == null || mc.player.onGround()) return false;

        if (mlgCooldownTicks > 0) {
            mlgCooldownTicks--;
            return false;
        }

        if (hasActiveTarget()) return false;

        double fallSpeed = -mc.player.getDeltaMovement().y;
        if (fallSpeed < 0.5) return false;

        double distanceToGround = getDistanceToGround();

        if (distanceToGround <= 3.0 && distanceToGround > 0 && mc.player.fallDistance > 2.5f) {
            int mlgSlot = findBestMlgSlot();
            if (mlgSlot != -1) {
                performMlg(mlgSlot);
                mlgCooldownTicks = 20;
                return true;
            }
        }
        return false;
    }

    private void performMlg(int slot) {
        if (mc.getConnection() == null || mc.player == null) return;

        mc.player.setXRot(90f);

        mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(slot));
        mc.player.getInventory().selected = slot;

        mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundUseItemPacket(
                net.minecraft.world.InteractionHand.MAIN_HAND,
                0,
                mc.player.getYRot(),
                90f
        ));

        mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        mlgTriggered = true;
    }

    public boolean isMlgActive() {
        return mlgTriggered || mlgCooldownTicks > 0;
    }

    private double getDistanceToGround() {
        Vec3 start = mc.player.getEyePosition(1.0f);
        Vec3 end = start.add(0, -MAX_FALL_SPEED, 0);
        BlockHitResult result = mc.level.clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        if (result.getType() == HitResult.Type.BLOCK) {
            return start.y - result.getLocation().y;
        }
        return MAX_FALL_SPEED;
    }

    private int findBestMlgSlot() {
        for (int i = 0; i < 9; i++) {
            Item item = mc.player.getInventory().getItem(i).getItem();
            if (item == Items.WATER_BUCKET) return i;
        }
        for (int i = 0; i < 9; i++) {
            Item item = mc.player.getInventory().getItem(i).getItem();
            if (item == Items.POWDER_SNOW_BUCKET) return i;
        }
        for (int i = 0; i < 9; i++) {
            Item item = mc.player.getInventory().getItem(i).getItem();
            if (item == Items.TWISTING_VINES || item == Items.WEEPING_VINES) return i;
        }
        return -1;
    }

    private boolean hasActiveTarget() {
        return false;
    }

    public void reset() {
        mlgTriggered = false;
        mlgCooldownTicks = 0;
    }
}