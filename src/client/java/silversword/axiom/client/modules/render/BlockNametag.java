package silversword.axiom.client.modules.render;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;
import silversword.axiom.client.event.render.Render2DEvent;
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
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.Renderer2D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.render.rendersystem.utils.render.NametagUtils;
import silversword.axiom.client.render.rendersystem.utils.render.RenderUtils;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingMode;
import silversword.axiom.client.setting.SettingNumber;
import silversword.axiom.client.setting.SettingSlider;
import silversword.axiom.client.utils.render.TextUtils;


import java.util.Arrays;
import java.util.List;

public final class BlockNametag extends AxiomMod implements ColorConfigurable, KeybindConfigurable {
    private final MinecraftClient mc = MinecraftClient.getInstance();

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
    private final Vector3d pos = new Vector3d();
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
        if (!isEnabled() || mc.world == null || mc.player == null) {
            currentBlockPos = null;
            currentBlockName = null;
            return;
        }

        HitResult hit = mc.crosshairTarget;
        if (!(hit instanceof BlockHitResult blockHit)) {
            currentBlockPos = null;
            currentBlockName = null;
            return;
        }

        BlockPos pos = blockHit.getBlockPos();
        double dist = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(pos));
        if (dist > renderDistance.getValue()) {
            currentBlockPos = null;
            currentBlockName = null;
            return;
        }

        BlockState state = mc.world.getBlockState(pos);
        if (state.isAir()) {
            currentBlockPos = null;
            currentBlockName = null;
            return;
        }

        currentBlockPos = pos;
        currentBlockName = Text.translatable(state.getBlock().getTranslationKey()).getString();
    }

    // --------------------------- 3D -> 2D conversion -----------------------
    @AxiomEvent
    private void onRender3D(Render3DEvent event) {
        NametagUtils.onRender(RenderUtils.view);
    }

    @AxiomEvent
    private void onRender2D(Render2DEvent event) {
        if (event.drawContext == null) return;
        if (!isEnabled() || currentBlockPos == null || currentBlockName == null) return;

        // Lasketaan blokin keskipiste + offset
        double x = currentBlockPos.getX() + 0.5;
        double y = currentBlockPos.getY() + 0.5 + nameOffset.getValue();
        double z = currentBlockPos.getZ() + 0.5;
        pos.set(x, y, z);

        // Muunnetaan 2D:ksi käyttäen asetettua skaalaa
        if (!NametagUtils.worldToScreen(pos, scale.getValue())) return;

        // Asetetaan skaala
        NametagUtils.scale = scale.getValue();

        NametagUtils.begin(pos, event.drawContext);

        // Piirretään nametag
        renderNametag();

        NametagUtils.end(event.drawContext);
    }

    private void renderNametag() {
        // Lasketaan tekstin leveys ja korkeus TextUtilsin avulla (kuten NameTagsissa)
        int textLength = currentBlockName.length();
        double textWidth = textLength * TextUtils.CHAR_UNIT;
        double textHeight = TextUtils.FONT_HEIGHT;

        double padding = 2.0;
        double bgWidth = textWidth + padding * 2;
        double bgHeight = textHeight + padding * 2;

        // Piirretään tausta
        drawBackground(-bgWidth / 2, -bgHeight / 2, bgWidth, bgHeight);

        // Piirretään teksti
        TextRenderer textRenderer = TextRenderer.get();
        boolean shadow = false;
        textRenderer.begin(1.0, false, true); // scale 1.0, koska NametagUtils on hoitanut skaalauksen
        textRenderer.render(currentBlockName, -textWidth / 2, -textHeight / 2, textColor.getCurrentColor(), shadow);
        textRenderer.end();
    }

    private void drawBackground(double x, double y, double width, double height) {
        String mode = bgMode.getMode();
        if (mode.equals("None")) return;

        Renderer2D.COLOR.begin();

        if (mode.equals("Filled")) {
            Renderer2D.COLOR.quad(x - 1, y - 1, width + 2, height + 2, background.getCurrentColor());
        } else if (mode.equals("Outline")) {
            Renderer2D.COLOR.boxLines(x - 1, y - 1, width + 2, height + 2, background.getCurrentColor());
        } else if (mode.equals("Rounded")) {
            double radius = 3.0;
            Renderer2D.COLOR.drawRoundedRect(x - 1, y - 1, width + 2, height + 2, radius, background.getCurrentColor());
        }

        if (!mode.equals("Outline") && outline.getCurrentColor().getAlpha() > 0) {
            double outlineThickness = 1.0;
            double radius = 3.0;
            Renderer2D.COLOR.drawRoundedRectOutline(x - 1, y - 1, width + 2, height + 2, radius, outline.getCurrentColor(), outlineThickness);
        }

        Renderer2D.COLOR.end();
        Renderer2D.COLOR.render();
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
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        UiComponent content = new ColorCustomizerView(this);
        factory.openCustomWindow("blocknametag_color", "BlockNametag Color Customizer", sw, sh, content);
    }
}