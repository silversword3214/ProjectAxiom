package silversword.axiom.client.modules.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingNumber;
import silversword.axiom.client.setting.SettingBoolean;

public final class ChestStealer extends AxiomMod implements KeybindConfigurable {
    public static ChestStealer INSTANCE;

    private final MinecraftClient mc = MinecraftClient.getInstance();

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);
    private final SettingNumber delay; // ms
    private final SettingBoolean silent;
    private final SettingBoolean stealAll;

    private long lastStealTime = 0;

    public ChestStealer() {
        super("ChestStealer", "Adds a steal button to chests", ModuleCategory.PLAYER);
        INSTANCE = this;

        delay = new SettingNumber("Delay", 0, 500, 1, 1);
        silent = new SettingBoolean("Silent", true);
        stealAll = new SettingBoolean("Steal All", true);

        addHiddenSetting(toggleKey);
        addSetting(delay);
        addSetting(silent);
        addSetting(stealAll);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    public void stealAll(ScreenHandler handler) {
        if (!isEnabled()) return;
        int containerSize = 0;
        if (handler instanceof GenericContainerScreenHandler) {
            containerSize = ((GenericContainerScreenHandler) handler).getRows() * 9;
        } else if (handler instanceof ShulkerBoxScreenHandler) {
            containerSize = 27; // Shulker on aina 3x9
        } else {
            return;
        }
        for (int i = 0; i < containerSize; i++) {
            if (!handler.getSlot(i).getStack().isEmpty()) {
                mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
            }
        }
    }

    @Override
    protected void onTick() {

    }
}