package silversword.axiom.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import silversword.axiom.client.config.HudConfigManager;
import silversword.axiom.client.gui.core.Rect;
import silversword.axiom.client.gui.core.UiContext;
import silversword.axiom.client.gui.window.WindowFactory;
import silversword.axiom.client.gui.screen.ClickGuiScreen;
import silversword.axiom.client.hud.HudElement;
import silversword.axiom.client.hud.HudManager;
import silversword.axiom.client.render.rendersystem.utils.texture.Texture;
import silversword.axiom.client.render.rendersystem.utils.texture.TextureManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HudComponentsList implements UiComponent {
    private static final Identifier GEAR_TEXTURE = Identifier.fromNamespaceAndPath("projectaxiom", "textures/icons/gear.png");
    private static Texture gearTexture;

    private Rect bounds = new Rect(0, 0, 200, 300);
    private final List<HudElement> elements;
    private final List<Toggle> toggles = new ArrayList<>();
    private final List<Runnable> gearActions = new ArrayList<>();
    private final List<Rect> gearRects = new ArrayList<>();
    private final List<Float> gearRotations = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int ROW_HEIGHT = 20;
    private static final int PADDING = 4;
    private static final int GEAR_SIZE = 16;

    public HudComponentsList() {
        this.elements = HudManager.get().elements().stream()
                .filter(e -> !e.isModuleControlled())
                .collect(Collectors.toList());

        if (gearTexture == null) {
            gearTexture = TextureManager.getTexture(GEAR_TEXTURE);
        }

        for (HudElement e : elements) {
            Toggle toggle = new Toggle(
                    e.id(),
                    e::enabled,
                    enabled -> {
                        e.setEnabled(enabled);
                        HudConfigManager.save(HudManager.get());
                    }
            );
            toggles.add(toggle);

            // GEAR ACTION: Tämän korjaus varmistaa, että asetusikkuna aukeaa oikein
            gearActions.add(() -> {
                Minecraft mc = Minecraft.getInstance();

                // 1. Vaihdetaan näyttö ENSIN ClickGuiScreeniin
                ClickGuiScreen clickGui = new ClickGuiScreen();
                mc.setScreen(clickGui);

                // 2. Haetaan factory vasta nyt (uusi näyttö on asettanut sen)
                WindowFactory factory = ClickGuiScreen.lastFactory;

                if (factory != null) {
                    HudComponentSettingsPanel panel = new HudComponentSettingsPanel(e);

                    int screenW = mc.getWindow().getGuiScaledWidth();
                    int screenH = mc.getWindow().getGuiScaledHeight();

                    // 3. Avataan ikkuna vasta kun factory on varmasti valmis
                    factory.openCustomWindow(
                            "hud_settings_" + e.id(),
                            e.id() + " Settings",
                            screenW,
                            screenH,
                            panel
                    );
                }
            });

            gearRotations.add(0f);
        }
    }

    @Override
    public Rect getBounds() { return bounds; }

    @Override
    public void setBounds(Rect bounds) { this.bounds = bounds; }

    @Override
    public int getPreferredHeight() { return 200; }

    private void layoutGearRects() {
        gearRects.clear();
        int x = bounds.x + PADDING;
        int y = bounds.y + PADDING - scrollOffset;
        int toggleWidth = bounds.w - 2 * PADDING - GEAR_SIZE - 4;

        for (int i = 0; i < toggles.size(); i++) {
            int rowY = y + i * ROW_HEIGHT;
            Toggle toggle = toggles.get(i);
            toggle.setBounds(new Rect(x, rowY, toggleWidth, ROW_HEIGHT));

            int gearX = x + toggleWidth + 4;
            int gearY = rowY + (ROW_HEIGHT - GEAR_SIZE) / 2;
            gearRects.add(new Rect(gearX, gearY, GEAR_SIZE, GEAR_SIZE));
        }
    }

    @Override
    public void render(UiContext ui, int mouseX, int mouseY, float delta) {
        layoutGearRects();
        ui.enableScissor(bounds.x, bounds.y, bounds.w, bounds.h);

        for (int i = 0; i < toggles.size(); i++) {
            Toggle toggle = toggles.get(i);
            Rect r = toggle.getBounds();

            // Renderöidään vain jos rivi on näkyvissä
            if (r.y + r.h >= bounds.y && r.y <= bounds.y + bounds.h) {
                toggle.render(ui, mouseX, mouseY, delta);

                Rect gearRect = gearRects.get(i);
                boolean gearHover = gearRect.contains(mouseX, mouseY);

                float rotation = gearRotations.get(i);
                if (gearHover) {
                    rotation += delta * 150f; // Nopeampi animaatio hoverissa
                } else if (rotation > 0) {
                    rotation = Math.max(0, rotation - delta * 200f);
                }
                rotation %= 360f;
                gearRotations.set(i, rotation);

                ui.fillRounded(gearRect, gearHover ? ui.theme.buttonHover : ui.theme.panel, 3);

                if (gearTexture != null) {
                    // Huom: Käytetään projektisi omaa renderöintitapaa tekstuurille
                    ui.renderer.core.addRotatedTexture(GEAR_TEXTURE, gearRect.x + 2, gearRect.y + 2, gearRect.w - 4, gearRect.h - 4, rotation, 0xFFFFFFFF);
                } else {
                    ui.text("*", gearRect.x + 5, gearRect.y + 4, ui.theme.textDim);
                }
            }
        }

        ui.disableScissor();

        // Scrollbar renderöinti
        int totalHeight = elements.size() * ROW_HEIGHT;
        int visibleHeight = bounds.h - 2 * PADDING;
        if (totalHeight > visibleHeight) {
            int scrollBarHeight = (int) ((float) visibleHeight / totalHeight * visibleHeight);
            int scrollBarY = bounds.y + PADDING + (int) ((float) scrollOffset / totalHeight * visibleHeight);
            ui.fill(bounds.x + bounds.w - 4, scrollBarY, 2, scrollBarHeight, ui.theme.accent);
        }
    }

    @Override
    public boolean mouseClicked(UiContext ui, double mouseX, double mouseY, int button) {
        if (!bounds.contains(mouseX, mouseY)) return false;

        for (int i = 0; i < toggles.size(); i++) {
            Toggle toggle = toggles.get(i);
            Rect r = toggle.getBounds();
            if (r.y + r.h >= bounds.y && r.y <= bounds.y + bounds.h) {
                if (r.contains(mouseX, mouseY)) {
                    return toggle.mouseClicked(ui, mouseX, mouseY, button);
                }
                Rect gearRect = gearRects.get(i);
                if (gearRect.contains(mouseX, mouseY)) {
                    gearActions.get(i).run();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(UiContext ui, double mouseX, double mouseY, double amount) {
        if (!bounds.contains(mouseX, mouseY)) return false;
        int totalHeight = elements.size() * ROW_HEIGHT;
        int visibleHeight = bounds.h - 2 * PADDING;
        if (totalHeight > visibleHeight) {
            scrollOffset -= amount * 20;
            scrollOffset = Math.max(0, Math.min(totalHeight - visibleHeight, scrollOffset));
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(UiContext ui, double mouseX, double mouseY, int button) {
        for (Toggle toggle : toggles) {
            toggle.mouseReleased(ui, mouseX, mouseY, button);
        }
    }

    @Override
    public boolean mouseDragged(UiContext ui, double mouseX, double mouseY, int button, double dx, double dy) {
        for (Toggle toggle : toggles) {
            Rect r = toggle.getBounds();
            if (r.y + r.h >= bounds.y && r.y <= bounds.y + bounds.h && r.contains(mouseX, mouseY)) {
                return toggle.mouseDragged(ui, mouseX, mouseY, button, dx, dy);
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(UiContext ui, int keyCode, int scanCode, int modifiers) {
        for (Toggle toggle : toggles) {
            if (toggle.keyPressed(ui, keyCode, scanCode, modifiers)) return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(UiContext ui, char chr, int modifiers) {
        for (Toggle toggle : toggles) {
            if (toggle.charTyped(ui, chr, modifiers)) return true;
        }
        return false;
    }
}