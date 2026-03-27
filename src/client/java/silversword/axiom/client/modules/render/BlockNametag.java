package silversword.axiom.client.modules.render;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import silversword.axiom.client.event.render.Render2DEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.gui.components.ColorCustomizerView;
import silversword.axiom.client.gui.components.UiComponent;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ColorConfigurable;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.NamedColor;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.axiomrenderer.RenderAPI;
import silversword.axiom.client.render.rendersystem.axiomrenderer.core.RenderCore;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.render.rendersystem.utils.render.NametagUtils;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingNumber;
import silversword.axiom.client.setting.SettingSlider;
import silversword.axiom.client.utils.render.TextUtils;

import java.util.Arrays;
import java.util.List;

public final class BlockNametag extends AxiomMod implements ColorConfigurable, KeybindConfigurable {
    private final Minecraft mc = Minecraft.getInstance();
    private final RenderCore core = RenderAPI.getInstance().getCore();

    // --- Settings -------------------------------------------------
    private final SettingNumber scale;
    private final SettingSlider renderDistance;
    private final SettingNumber nameOffset;

    // Colors
    final SettingColor textColor;
    final SettingColor background;
    final SettingColor outline;

    // Background mode
    private final SettingMode bgMode;

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    // --------------------------------------------------------------
    private BlockPos currentBlockPos = null;
    private String currentBlockName = null;

    public BlockNametag() {
        super("BlockNametag", "Shows the name of the block you're looking at", ModuleCategory.RENDER);

        scale = new SettingNumber("Scale", 0.1, 3.0, 0.1, 1.5);
        renderDistance = new SettingSlider("Render Distance", new double[]{16, 32, 64, 96, 128, 256, 512}, 96);
        nameOffset = new SettingNumber("Name Offset", -1.0, 5.0, 0.1, 0.5);

        textColor = new SettingColor("Text Color", new Color(255, 255, 255, 255));
        background = new SettingColor("Background", new Color(0, 0, 0, 75));
        outline = new SettingColor("Outline", new Color(255, 255, 255, 255));

        bgMode = new SettingMode("Background", new String[]{"None", "Filled", "Outline", "Rounded"}, "Filled");

        addHiddenSetting(textColor.getSetting());
        addHiddenSetting(background.getSetting());
        addHiddenSetting(outline.getSetting());
        addHiddenSetting(toggleKey);

        addSetting(nameOffset);
        addSetting(scale);
        addSetting(renderDistance);
        addSetting(bgMode);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    // --------------------------- Tick ------------------------------
    @Override
    protected void onTick() {
        if (!isEnabled() || mc.level == null || mc.player == null) {
            currentBlockPos = null;
            currentBlockName = null;
            return;
        }

        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult blockHit)) {
            currentBlockPos = null;
            currentBlockName = null;
            return;
        }

        BlockPos pos = blockHit.getBlockPos();
        double dist = mc.player.getEyePosition().distanceTo(Vec3.atCenterOf(pos));
        if (dist > renderDistance.getValue()) {
            currentBlockPos = null;
            currentBlockName = null;
            return;
        }

        BlockState state = mc.level.getBlockState(pos);
        if (state.isAir()) {
            currentBlockPos = null;
            currentBlockName = null;
            return;
        }

        currentBlockPos = pos;
        currentBlockName = Component.translatable(state.getBlock().getDescriptionId()).getString();
    }

    // --------------------------- 2D rendering -----------------------
    @Subscribe
    private void onRender2D(Render2DEvent event) {
        if (event.getGuiGraphics() == null) return;
        if (!isEnabled() || currentBlockPos == null || currentBlockName == null) return;

        double x = currentBlockPos.getX() + 0.5;
        double y = currentBlockPos.getY() + 0.5 + nameOffset.getValue();
        double z = currentBlockPos.getZ() + 0.5;
        Vec3 worldPos = new Vec3(x, y, z);

        Vec3 screenPos = NametagUtils.worldToScreen(worldPos);
        if (screenPos == null) return;

        double screenX = screenPos.x;
        double screenY = screenPos.y;
        double finalScale = scale.getValue();

        TextRenderer text = TextRenderer.get();
        text.begin(1.0, false, true);
        double textWidth = text.getWidth(currentBlockName, false) * finalScale;
        double textHeight = text.getHeight(false) * finalScale;
        text.end();

        double padding = 2.0 * finalScale;
        double bgWidth = textWidth + padding * 2;
        double bgHeight = textHeight + padding * 2;
        double bgX = screenX - bgWidth / 2;
        double bgY = screenY - bgHeight / 2;

        drawBackground(bgX, bgY, bgWidth, bgHeight, finalScale);

        text.begin(1.0, false, true);
        text.render(currentBlockName, bgX + padding, bgY + padding, textColor.getCurrentColor(), false);
        text.end();
    }

    private void drawBackground(double x, double y, double width, double height, double finalScale) {
        String mode = bgMode.getMode();
        if (mode.equals("None")) return;

        int bgArgb = background.getCurrentColor().getARGB();
        int outlineArgb = outline.getCurrentColor().getARGB();
        double radius = 3.0 * finalScale;
        double thickness = Math.max(1.0, finalScale);

        if (mode.equals("Filled")) {
            core.addRect2D((float) x, (float) y, (float) width, (float) height, bgArgb);
        } else if (mode.equals("Outline")) {
            // Piirretään reunus neljänä viivana
            core.addRectOutline2D((float) x, (float) y, (float) width, (float) height, (float) thickness, outlineArgb);
        } else if (mode.equals("Rounded")) {
            core.addRoundedRect((float) x, (float) y, (float) width, (float) height, (float) radius, bgArgb);
        }

        if (!mode.equals("Outline") && outline.getCurrentColor().getAlpha() > 0) {
            core.addRoundedRectOutline((float) x, (float) y, (float) width, (float) height, (float) radius, (float) thickness, outlineArgb);
        }
    }

    // --------------------------- Color Config ------------------------------
    @Override
    public List<NamedColor> getColors() {
        return Arrays.asList(
                new NamedColor("Text", textColor),
                new NamedColor("Background", background),
                new NamedColor("Outline", outline)
        );
    }

    public void openColorEditor() {
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        UiComponent content = new ColorCustomizerView(this);
        factory.openCustomWindow("blocknametag_color", "BlockNametag Color Customizer", sw, sh, content);
    }
}