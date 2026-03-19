package silversword.axiom.client.modules.world;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingSlider;
import silversword.axiom.mixin.client.accessors.ClientPlayerInteractionManagerAccessor;

public class FastBreak extends AxiomMod implements KeybindConfigurable {

    private final MinecraftClient mc = MinecraftClient.getInstance();

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
        if (mc.player == null || mc.world == null) return;
        if (!(mc.crosshairTarget instanceof BlockHitResult hit)) return;

        ClientPlayerInteractionManager manager = mc.interactionManager;
        if (manager == null) return;

        // 🟢 Legit mode = vain cooldown pois
        ((ClientPlayerInteractionManagerAccessor) manager)
                .setBlockBreakingCooldown(0);

        if (legitMode.get()) return;

        BlockPos pos = hit.getBlockPos();
        Direction dir = hit.getSide();

        int speedLevel = (int) speed.getValue();

        // Älä spämmi joka tick samalla blokilla matalilla speed-arvoilla
        tickCounter++;
        if (tickCounter < (6 - speedLevel)) return;
        tickCounter = 0;

        if (pos.equals(lastBlock) && speedLevel <= 2) return;

        lastBlock = pos;

        mc.getNetworkHandler().sendPacket(
                new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
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
