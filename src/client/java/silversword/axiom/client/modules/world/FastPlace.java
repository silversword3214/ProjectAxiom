package silversword.axiom.client.modules.world;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.mixin.client.accessors.MinecraftAccessor;

public class FastPlace extends AxiomMod implements KeybindConfigurable {
    private final Minecraft mc = Minecraft.getInstance();

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
        if (!isEnabled() || mc.player == null || mc.level == null) return;

        // Yarn 1.21.x: useKey (ei keyUse) [web:151]
        if (!mc.options.keyUse.isDown()) return;

        if (onlyOnGround && !mc.player.onGround()) return;

        ItemStack stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof BlockItem)) return;

        // Poista placing-viive
        ((MinecraftAccessor) mc).axiom$setRightClickDelay(0);
    }
}
