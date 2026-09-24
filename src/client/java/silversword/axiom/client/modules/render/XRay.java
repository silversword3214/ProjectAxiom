package silversword.axiom.client.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingNumber;

import java.util.HashSet;
import java.util.Set;

public final class XRay extends AxiomMod implements KeybindConfigurable {

    private static final Set<Block> XRAY_BLOCKS = new HashSet<>();

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    private static volatile float HIDDEN_ALPHA = 0f;
    private static volatile boolean ENABLED = false;

    private final SettingNumber opacity;

    static {
        // Ores
        add("minecraft:coal_ore");           add("minecraft:deepslate_coal_ore");
        add("minecraft:iron_ore");           add("minecraft:deepslate_iron_ore");
        add("minecraft:copper_ore");         add("minecraft:deepslate_copper_ore");
        add("minecraft:gold_ore");           add("minecraft:deepslate_gold_ore");
        add("minecraft:redstone_ore");       add("minecraft:deepslate_redstone_ore");
        add("minecraft:diamond_ore");        add("minecraft:deepslate_diamond_ore");
        add("minecraft:emerald_ore");        add("minecraft:deepslate_emerald_ore");
        add("minecraft:lapis_ore");          add("minecraft:deepslate_lapis_ore");
        add("minecraft:nether_gold_ore");    add("minecraft:nether_quartz_ore");
        add("minecraft:ancient_debris");

        // ORE BLOCKS
        add("minecraft:coal_block");         add("minecraft:iron_block");
        add("minecraft:copper_block");       add("minecraft:gold_block");
        add("minecraft:redstone_block");     add("minecraft:lapis_block");
        add("minecraft:diamond_block");      add("minecraft:emerald_block");
        add("minecraft:netherite_block");
        add("minecraft:raw_iron_block");     add("minecraft:raw_copper_block");
        add("minecraft:raw_gold_block");

        // STORAGE
        add("minecraft:chest");              add("minecraft:trapped_chest");
        add("minecraft:ender_chest");        add("minecraft:barrel");

        // CONTAINERS
        add("minecraft:hopper");             add("minecraft:dispenser");
        add("minecraft:dropper");            add("minecraft:observer");

        // SHULKERS
        add("minecraft:shulker_box");        add("minecraft:white_shulker_box");
        add("minecraft:orange_shulker_box"); add("minecraft:magenta_shulker_box");
        add("minecraft:light_blue_shulker_box"); add("minecraft:yellow_shulker_box");
        add("minecraft:lime_shulker_box");   add("minecraft:pink_shulker_box");
        add("minecraft:gray_shulker_box");   add("minecraft:light_gray_shulker_box");
        add("minecraft:cyan_shulker_box");   add("minecraft:purple_shulker_box");
        add("minecraft:blue_shulker_box");   add("minecraft:brown_shulker_box");
        add("minecraft:green_shulker_box");  add("minecraft:red_shulker_box");
        add("minecraft:black_shulker_box");

        // NETHER
        add("minecraft:glowstone");          add("minecraft:magma_block");
        add("minecraft:nether_bricks");      add("minecraft:cracked_nether_bricks");
        add("minecraft:red_nether_bricks");

        // SPAWNER
        add("minecraft:spawner");
    }

    private static void add(String id) {
        Block b = BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
        if (b != null) XRAY_BLOCKS.add(b);
    }

    public XRay() {
        super("XRay", "Better than texture pack", ModuleCategory.RENDER);
        opacity = new SettingNumber("Hidden Opacity", 0, 100.0, 1.0, 40.0);
        addHiddenSetting(toggleKey);
        addSetting(opacity);
    }

    @Override
    public SettingKeybind getKeybind() { return toggleKey; }

    @Override
    protected void onEnable() {
        HIDDEN_ALPHA = (float)(opacity.getValue() / 100.0);
        ENABLED = true;
        reloadChunks();
    }

    @Override
    protected void onDisable() {
        HIDDEN_ALPHA = 0f;
        ENABLED = false;
        reloadChunks();
    }

    @Override
    protected void onTick() {
        float newAlpha = (float)(opacity.getValue() / 100.0);
        if (Math.abs(newAlpha - HIDDEN_ALPHA) > 0.01f) {
            HIDDEN_ALPHA = newAlpha;
            reloadChunks();
        }
    }

    private static void reloadChunks() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.levelRenderer != null) {
            mc.levelRenderer.resetLevelRenderData();
        }
    }

    // ─── Public API for mixins ─────────────────────────────────────────────

    public static boolean isXRayEnabled() { return ENABLED; }
    public static float   getHiddenAlpha() { return HIDDEN_ALPHA; }

    public static boolean isXrayVisible(BlockState state) {
        if (state == null) return false;
        return XRAY_BLOCKS.contains(state.getBlock());
    }

    public static boolean isXrayHidden(BlockState state) {
        return ENABLED && !isXrayVisible(state);
    }

    public static boolean shouldRender(BlockState state) {
        return isXrayVisible(state);
    }

    // Backwards-compat
    public static boolean isEnabledGlobal() { return ENABLED; }
    public static boolean isCurrentBlockHidden() { return false; }
    public static void setCurrentBlockHidden(boolean b) {}
}