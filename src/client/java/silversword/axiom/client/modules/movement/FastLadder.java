package silversword.axiom.client.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PowderSnowBlock;
import silversword.axiom.client.event.player.PreMotionEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingNumber;

public class FastLadder extends AxiomMod implements KeybindConfigurable {

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    private final SettingNumber climbSpeed = new SettingNumber("Climb Speed", 0.1, 1.0, 0.01, 0.4);

    public FastLadder() {
        super("Fast Ladder", "Climb ladders and vines faster", ModuleCategory.MOVEMENT);
        addHiddenSetting(toggleKey);
        addSetting(climbSpeed);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onTick() {
        if (!isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Player player = mc.player;

        // Tarkistetaan onko pelaaja tikkailla tai köynnöksellä
        boolean isClimbing = player.onClimbable();
        // Tarkistetaan onko pelaaja lumihangessa (jossa voi myös "kiivetä")
        boolean inPowderSnow = player.getBlockStateOn().is(Blocks.POWDER_SNOW) && PowderSnowBlock.canEntityWalkOnPowderSnow(player);
        // Tarkistetaan onko pelaaja seinää vasten tai hyppäämässä (kuten Meteorin FastClimb)
        boolean canClimbFast = (player.horizontalCollision || player.isJumping()) && (isClimbing || inPowderSnow);

        if (canClimbFast) {
            double speed = climbSpeed.getValue();
            player.setDeltaMovement(player.getDeltaMovement().x, speed, player.getDeltaMovement().z);
        }
    }
}