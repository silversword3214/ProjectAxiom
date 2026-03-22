package silversword.axiom.client.modules.world;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingSlider;
import silversword.axiom.mixin.client.accessors.MultiPlayerGameModeAccessor;

public class FastBreak extends AxiomMod implements KeybindConfigurable {

    private final Minecraft mc = Minecraft.getInstance();

    // ---- SETTINGS ----

    private final SettingSlider speed = new SettingSlider(
            "Speed",
            new double[]{1, 2, 3, 4, 5}, // aggressiivisuus tasot
            2
    );

    private final SettingBoolean legitMode = new SettingBoolean(
            "Legit Mode",
            false
    );

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    private BlockPos lastBlock;
    private int tickCounter = 0;

    public FastBreak() {
        super("Fast Break", "Speeds up block breaking and removes the delay.", ModuleCategory.WORLD);

        addSetting(speed);
        addSetting(legitMode);

        addHiddenSetting(toggleKey);

    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }


    @Override
    protected void onTick() {
        if (mc.player == null || mc.level == null) return;
        if (!(mc.hitResult instanceof BlockHitResult hit)) return;

        MultiPlayerGameMode manager = mc.gameMode;
        if (manager == null) return;

        // 🟢 Legit mode = vain cooldown pois
        ((MultiPlayerGameModeAccessor) manager)
                .setDestroyDelay(0);

        if (legitMode.get()) return;

        BlockPos pos = hit.getBlockPos();
        Direction dir = hit.getDirection();

        int speedLevel = (int) speed.getValue();

        // Älä spämmi joka tick samalla blokilla matalilla speed-arvoilla
        tickCounter++;
        if (tickCounter < (6 - speedLevel)) return;
        tickCounter = 0;

        if (pos.equals(lastBlock) && speedLevel <= 2) return;

        lastBlock = pos;

        mc.getConnection().send(
                new ServerboundPlayerActionPacket(
                        ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                        pos,
                        dir
                )
        );
    }

    @Override
    protected void onDisable() {
        lastBlock = null;
        tickCounter = 0;
    }
}
