package silversword.axiom.client.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.event.render.Render3DEvent;
import silversword.axiom.client.eventbus.Subscribe;
import silversword.axiom.client.gui.components.ColorCustomizerView;
import silversword.axiom.client.gui.components.UiComponent;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.modules.ColorConfigurable;
import silversword.axiom.client.modules.KeybindConfigurable;
import silversword.axiom.client.modules.ModuleCategory;
import silversword.axiom.client.modules.NamedColor;

import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer3D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.setting.SettingBoolean;
import silversword.axiom.client.setting.SettingKeybind;
import silversword.axiom.client.setting.SettingNumber;

import java.util.Arrays;
import java.util.List;

public final class ChunkBorders extends AxiomMod implements ColorConfigurable, KeybindConfigurable {
    private final Minecraft mc = Minecraft.getInstance();

    // Värit
    final SettingColor gridColor;
    final SettingColor highlightColor;

    // Asetukset (ilman modea)
    private final SettingNumber renderDistance;
    private final SettingBoolean highlightCurrentChunk;
    private final SettingBoolean verticalLines;
    private final SettingBoolean horizontalLines;
    private final SettingBoolean highlightOnlyFromPlayer;
    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    public ChunkBorders() {
        super("Chunk Borders", "Draws highly customizable chunk borders", ModuleCategory.RENDER);

        gridColor       = new SettingColor("Grid Color",    new Color(150, 150, 255, 150));
        highlightColor  = new SettingColor("Highlight Color", new Color(255, 255, 0, 255));

        renderDistance = new SettingNumber("Render Distance (chunks)", 1, 32, 1, 2);
        highlightCurrentChunk = new SettingBoolean("Highlight Current Chunk", true);
        verticalLines = new SettingBoolean("Vertical Lines", true);
        horizontalLines = new SettingBoolean("Horizontal Lines", true);
        highlightOnlyFromPlayer = new SettingBoolean("Highlight only from player", false);

        addHiddenSetting(gridColor.getSetting());
        addHiddenSetting(highlightColor.getSetting());
        addHiddenSetting(toggleKey);

        addSetting(renderDistance);
        addSetting(highlightCurrentChunk);
        addSetting(verticalLines);
        addSetting(horizontalLines);
        addSetting(highlightOnlyFromPlayer);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    @Override
    protected void onTick() {}

    @Subscribe
    private void onRender(Render3DEvent event) {
        if (!isEnabled() || mc.player == null || mc.level == null) return;

        int playerChunkX = mc.player.chunkPosition().x;
        int playerChunkZ = mc.player.chunkPosition().z;
        int radius = (int) renderDistance.getValue();

        Vec3 camera = mc.gameRenderer.getMainCamera().position();
        double minY = mc.level.getMinY();
        double maxY = mc.level.getMaxY();
        double playerY = mc.player.getY();

        Renderer3D renderer = event.getRenderer();

        for (int cx = playerChunkX - radius; cx <= playerChunkX + radius; cx++) {
            for (int cz = playerChunkZ - radius; cz <= playerChunkZ + radius; cz++) {
                double x1 = cx * 16.0;
                double z1 = cz * 16.0;
                double x2 = x1 + 16.0;
                double z2 = z1 + 16.0;

                double centerX = x1 + 8;
                double centerZ = z1 + 8;
                double distSq = (centerX - camera.x) * (centerX - camera.x) + (centerZ - camera.z) * (centerZ - camera.z);
                if (distSq > (radius * 16.0) * (radius * 16.0)) continue;

                boolean isCurrentChunk = (cx == playerChunkX && cz == playerChunkZ) && highlightCurrentChunk.get();

                if (isCurrentChunk) {
                    // Piirrä highlight-chunk
                    int hlColor = highlightColor.getCurrentColor().getARGB();
                    double fromY = highlightOnlyFromPlayer.get() ? playerY : minY;
                    double toY = maxY;

                    // Pystysuorat viivat (vertical lines) – joka toinen palikka reunalla
                    if (verticalLines.get()) {
                        for (int i = 0; i <= 16; i += 2) { // step 2
                            double xPos = x1 + i;
                            double zPos = z1 + i;

                            // x-akselin suuntaiset reunat (z = z1 ja z = z2)
                            renderer.drawLine(xPos, fromY, z1, xPos, toY, z1, hlColor);
                            renderer.drawLine(xPos, fromY, z2, xPos, toY, z2, hlColor);

                            // z-akselin suuntaiset reunat (x = x1 ja x = x2)
                            renderer.drawLine(x1, fromY, zPos, x1, toY, zPos, hlColor);
                            renderer.drawLine(x2, fromY, zPos, x2, toY, zPos, hlColor);
                        }
                    }

                    // Vaakasuorat viivat (horizontal lines) – joka toinen lohko y-akselilla
                    if (horizontalLines.get()) {
                        double yStart = fromY;
                        double yEnd = toY;
                        // Piirretään vaakaviivat chunkin reunoille tietyin välein y-akselilla
                        // Viivat kulkevat x-suunnassa ja z-suunnassa (kuten grid)
                        for (double y = yStart; y <= yEnd; y += 2) {
                            // x-suuntaiset viivat (z = z1 ja z = z2)
                            renderer.drawLine(x1, y, z1, x2, y, z1, hlColor);
                            renderer.drawLine(x1, y, z2, x2, y, z2, hlColor);
                            // z-suuntaiset viivat (x = x1 ja x = x2)
                            renderer.drawLine(x1, y, z1, x1, y, z2, hlColor);
                            renderer.drawLine(x2, y, z1, x2, y, z2, hlColor);
                        }
                    }
                } else {
                    // Piirrä muut chunkit – vain ulkoreunat (koko korkeus)
                    int useColor = gridColor.getCurrentColor().getARGB();

                    // Pystyviivat (neljä kulmaa)
                    renderer.drawLine(x1, minY, z1, x1, maxY, z1, useColor);
                    renderer.drawLine(x2, minY, z1, x2, maxY, z1, useColor);
                    renderer.drawLine(x1, minY, z2, x1, maxY, z2, useColor);
                    renderer.drawLine(x2, minY, z2, x2, maxY, z2, useColor);

                    // Vaakaviivat alhaalla ja ylhäällä
                    renderer.drawLine(x1, minY, z1, x2, minY, z1, useColor);
                    renderer.drawLine(x1, minY, z2, x2, minY, z2, useColor);
                    renderer.drawLine(x1, minY, z1, x1, minY, z2, useColor);
                    renderer.drawLine(x2, minY, z1, x2, minY, z2, useColor);

                    renderer.drawLine(x1, maxY, z1, x2, maxY, z1, useColor);
                    renderer.drawLine(x1, maxY, z2, x2, maxY, z2, useColor);
                    renderer.drawLine(x1, maxY, z1, x1, maxY, z2, useColor);
                    renderer.drawLine(x2, maxY, z1, x2, maxY, z2, useColor);
                }
            }
        }
    }

    @Override
    public List<NamedColor> getColors() {
        return Arrays.asList(
                new NamedColor("Grid", gridColor),
                new NamedColor("Highlight", highlightColor)
        );
    }

    public void openColorEditor() {
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        UiComponent content = new ColorCustomizerView(this);
        factory.openCustomWindow("chunkborders_color", "ChunkBorders Color Customizer", sw, sh, content);
    }
}