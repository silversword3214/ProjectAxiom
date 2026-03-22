package silversword.axiom.client.modules.render;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.eventbus.AxiomEvent;
import silversword.axiom.client.gui.components.ColorCustomizerView;
import silversword.axiom.client.gui.components.UiComponent;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ColorConfigurable;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.NamedColor;
import silversword.axiom.client.render.rendersystem.ShapeMode;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingSlider;

import java.util.Arrays;
import java.util.List;

public final class BlockOutline extends AxiomMod implements ColorConfigurable, KeybindConfigurable {
    private final Minecraft mc = Minecraft.getInstance();

    // Väri
    private final SettingColor color;

    // Asetukset
    private final SettingMode boxMode; // "Outline", "Filled", "Both"
    private final SettingSlider renderDistance;
    private final SettingBoolean onlyInteractable; // piirrä vain blokit joihin voi kohdistaa interaktion
    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    public BlockOutline() {
        super("Block Outline", "Draws an outline around the block you're looking at", ModuleCategory.RENDER);

        color = new SettingColor("Color", new Color(255, 255, 255, 180));

        boxMode = new SettingMode("Box Mode", new String[]{"Outline", "Filled", "Both"}, "Outline");
        renderDistance = new SettingSlider("Render Distance", new double[]{16, 32, 64, 96, 128, 256}, 64);
        onlyInteractable = new SettingBoolean("Only Interactable", true);

        // Piilotetut
        addHiddenSetting(color.getSetting());
        addHiddenSetting(toggleKey);

        // Näkyvät
        addSetting(boxMode);
        addSetting(renderDistance);
        addSetting(onlyInteractable);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @AxiomEvent
    private void onRender(Render3DEvent event) {
        if (!isEnabled() || mc.player == null || mc.level == null) return;

        // Katsotaan mihin pelaaja osoittaa
        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult blockHit)) return;

        BlockPos pos = blockHit.getBlockPos();
        double distance = mc.player.getEyePosition().distanceTo(pos.getCenter());
        if (distance > renderDistance.getValue()) return;

        BlockState state = mc.level.getBlockState(pos);
        if (state.isAir()) return;

        // Jos onlyInteractable on päällä, tarkista onko blokki interaktiivinen
        if (onlyInteractable.get() && !state.isSolid()) return; // Yksinkertaistus: interaktiiviset blokit eivät aina ole solid, mutta käytetään solidia alustavasti

        // Haetaan blokin tarkka muoto (voxelshape) – se antaa tarkat rajat esim. puolilohkoille
        AABB box = state.getShape(mc.level, pos).bounds();
        // Muunnetaan maailmankoordinaatteihin
        double x1 = pos.getX() + box.minX;
        double y1 = pos.getY() + box.minY;
        double z1 = pos.getZ() + box.minZ;
        double x2 = pos.getX() + box.maxX;
        double y2 = pos.getY() + box.maxY;
        double z2 = pos.getZ() + box.maxZ;

        Color currentColor = color.getCurrentColor();
        Color sideColor = new Color(currentColor.r, currentColor.g, currentColor.b, 50); // läpinäkyvämpi täytölle
        Color lineColor = currentColor;

        ShapeMode mode = switch (boxMode.getMode()) {
            case "Filled" -> ShapeMode.Sides;
            case "Both"   -> ShapeMode.Both;
            default       -> ShapeMode.Lines;
        };

        event.render.drawBox(x1, y1, z1, x2, y2, z2, sideColor, lineColor, mode, 0);
    }

    @Override
    public List<NamedColor> getColors() {
        return Arrays.asList(new NamedColor("Block", color));
    }

    public void openColorEditor() {
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        UiComponent content = new ColorCustomizerView(this);
        factory.openCustomWindow("blockoutline_color", "BlockOutline Color Customizer", sw, sh, content);
    }

    @Override
    protected void onTick() {

    }
}