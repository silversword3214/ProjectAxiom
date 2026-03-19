package silversword.axiom.client.modules.world;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.mixin.client.accessors.MinecraftClientAccessor;

public class FastPlace extends AxiomMod implements KeybindConfigurable {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    // Halutessasi tee tästä myöhemmin setting
    private final boolean onlyOnGround = true;

    public FastPlace() {
        super("Fast Place", "Removes right click delay while placing blocks.", ModuleCategory.WORLD);
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    public void onTick() {
        if (!isEnabled() || mc.player == null || mc.world == null) return;

        // Yarn 1.21.x: useKey (ei keyUse) [web:151]
        if (!mc.options.useKey.isPressed()) return;

        if (onlyOnGround && !mc.player.isOnGround()) return;

        ItemStack stack = mc.player.getMainHandStack();
        if (!(stack.getItem() instanceof BlockItem)) return;

        // Poista placing-viive
        ((MinecraftClientAccessor) mc).axiom$setItemUseCooldown(0);
    }
}
