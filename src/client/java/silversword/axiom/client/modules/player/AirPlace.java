package silversword.axiom.client.modules.player;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ArmorStandItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.moduleutils.InteractItemEvent;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.main.AxiomInitialize;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.eventbus.AxiomEvent;
import silversword.axiom.client.render.rendersystem.ShapeMode;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingSlider;

import static silversword.axiom.client.main.AxiomInitialize.mc;

public class AirPlace extends AxiomMod implements KeybindConfigurable {

    public static AirPlace INSTANCE;

    // Asetukset
    private final SettingBoolean render = new SettingBoolean("Render", true);
    private final SettingMode shapeMode = new SettingMode("Shape Mode", new String[]{"Lines", "Sides", "Both"}, "Both");
    private final SettingColor sideColor = new SettingColor("Side Color", new Color(204, 0, 0, 10));
    private final SettingColor lineColor = new SettingColor("Line Color", new Color(204, 0, 0, 255));
    private final SettingBoolean customRange = new SettingBoolean("Custom Range", true);
    private final SettingSlider range = new SettingSlider("Range", new double[]{1, 2, 3, 4, 5, 6}, 5);
    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    private HitResult hitResult;

    public AirPlace() {
        super("Air Place", "Places a block where your crosshair is pointing at", ModuleCategory.PLAYER);
        INSTANCE = this;

        addSetting(render);
        addSetting(shapeMode);
        addSetting(customRange);
        addSetting(range);

        addHiddenSetting(sideColor.getSetting());
        addHiddenSetting(lineColor.getSetting());
        addHiddenSetting(toggleKey);

        AxiomInitialize.EVENT_BUS.subscribe(this);
    }

    @Override
    public void onTick() {
        if (!isActive()) return;
        if (!isHoldingPlaceable()) return;
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.MISS) return;

        double r = customRange.get() ? range.getValue() : mc.player.blockInteractionRange();
        hitResult = mc.getCameraEntity().pick(r, 0, false);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @AxiomEvent
    private void onInteractItem(InteractItemEvent event) {
        if (!isActive()) return;
        if (!(hitResult instanceof BlockHitResult bhr)) return;
        if (!isHoldingPlaceable()) return;

        InteractionHand hand = event.hand;
        ItemStack stack = mc.player.getItemInHand(hand);
        BlockPos pos = bhr.getBlockPos();

        // Tarkistetaan, voidaanko paikkaan asettaa lohko
        if (!mc.level.getBlockState(pos).canBeReplaced()) return;

        // Varmistetaan, että item on asetettava
        if (!isPlaceableItem(stack.getItem())) return;

        // Suoritetaan asettaminen
        Vec3 hitPos = Vec3.atCenterOf(pos);
        BlockHitResult newBhr = new BlockHitResult(hitPos, mc.player.getMotionDirection().getOpposite(), pos, false);
        InteractionResult result = mc.gameMode.useItemOn(mc.player, hand, newBhr);
        if (result.consumesAction()) {
            event.toReturn = InteractionResult.SUCCESS;
        }
    }

    @AxiomEvent
    private void onRender(Render3DEvent event) {
        if (!isActive()) return;
        if (!render.get()) return;
        if (!(hitResult instanceof BlockHitResult bhr)) return;
        if (mc.hitResult != null && mc.hitResult.getType() != HitResult.Type.MISS) return;
        if (!mc.level.getBlockState(bhr.getBlockPos()).canBeReplaced()) return;
        if (!isHoldingPlaceable()) return;

        BlockPos pos = bhr.getBlockPos();
        double x1 = pos.getX();
        double y1 = pos.getY();
        double z1 = pos.getZ();
        double x2 = x1 + 1;
        double y2 = y1 + 1;
        double z2 = z1 + 1;

        // Muunnetaan asetusmerkkijono ShapeMode-enumiksi
        ShapeMode mode = ShapeMode.valueOf(shapeMode.getMode());
        Color side = sideColor.getCurrentColor();   // Tukee rainbow-tilaa
        Color line = lineColor.getCurrentColor();

        event.render.drawBox(x1, y1, z1, x2, y2, z2, side, line, mode, 0);
    }

    private boolean isHoldingPlaceable() {
        if (mc.player == null) return false;
        ItemStack stack = mc.player.getMainHandItem();
        if (stack.isEmpty()) return false;
        return isPlaceableItem(stack.getItem());
    }

    private boolean isPlaceableItem(Item item) {
        return item instanceof BlockItem
                || item instanceof SpawnEggItem
                || item instanceof ArmorStandItem
                || item instanceof FireworkRocketItem;
    }

    public boolean isActive() {
        return this.isEnabled();
    }
}