package silversword.axiom.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import org.joml.Matrix4f;
import silversword.axiom.client.event.render.Render2DEvent;
import silversword.axiom.client.gui.core.Theme;
import silversword.axiom.client.gui.core.ThemeManager;
import silversword.axiom.client.gui.screen.ClickGuiScreen;
import silversword.axiom.client.hud.core.HudContext;
import silversword.axiom.client.main.AxiomInitialize;
import silversword.axiom.client.render.rendersystem.axiomrenderer.RenderAPI;
import silversword.axiom.client.render.rendersystem.axiomrenderer.renderer.Renderer2D;
import silversword.axiom.client.render.rendersystem.utils.render.RenderUtils;
import silversword.axiom.client.utils.render.DrawTexture;

import java.util.*;

public final class HudManager {
    private static HudManager INSTANCE;
    public static HudManager get() {
        if (INSTANCE == null) INSTANCE = new HudManager();
        return INSTANCE;
    }

    private final List<HudElement> elements = new ArrayList<>();
    private final Map<String, HudState> pendingStates = new HashMap<>();

    private HudManager() {}

    public void register(HudElement element) {
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i).id().equals(element.id())) {
                elements.set(i, element);
                applyPending(element);
                return;
            }
        }
        elements.add(element);
        applyPending(element);
    }

    private void applyPending(HudElement element) {
        HudState st = pendingStates.remove(element.id());
        if (st != null) {
            element.setPos(st.x, st.y);
            if (!element.isModuleControlled()) {
                element.setEnabled(st.enabled);
            }
        }
    }

    public void applyOrStore(String id, int x, int y, boolean enabled) {
        for (HudElement e : elements) {
            if (e.id().equals(id)) {
                e.setPos(x, y);
                if (!e.isModuleControlled()) {
                    e.setEnabled(enabled);
                }
                return;
            }
        }
        pendingStates.put(id, new HudState(x, y, enabled));
    }

    public List<HudElement> elements() {
        return Collections.unmodifiableList(elements);
    }

    public void renderAll(GuiGraphics draw, DeltaTracker tickCounter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        float delta = tickCounter.getGameTimeDeltaPartialTick(true);

        // 1. Käytetään skaalattua projektiota, jotta kaikki on samassa koossa
        Matrix4f proj = RenderUtils.getScaledProjection(draw);
        Renderer2D renderer = new Renderer2D(draw, RenderAPI.getInstance().getCore(), proj);

        Theme theme = ThemeManager.getCurrentTheme();

        // 2. LÄHETÄ EVENT TÄSSÄ (Moduulit kuten NameTags piirtävät nyt tässä)
        Render2DEvent event = new Render2DEvent(renderer, delta, draw, draw.guiWidth(), draw.guiHeight());
        AxiomInitialize.EVENT_BUS.post(event);

        // 3. Piirrä HUD-elementit (kuten Watermark, Coordinates)
        HudContext ctx = new HudContext(mc, draw, theme, delta, renderer);
        for (HudElement e : elements) {
            if (!e.enabled()) continue;
            e.render(ctx, tickCounter);
        }

        ctx.renderTexts();

        RenderAPI.getInstance().getCore().flush();

        DrawTexture.renderAll();

        RenderAPI.getInstance().getCore().flush();
    }

    public HudElement hitTest(int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return null;

        for (int i = elements.size() - 1; i >= 0; i--) {
            HudElement e = elements.get(i);
            if (!e.enabled()) continue;
            int x = e.x();
            int y = e.y();
            int w = Math.max(1, e.width(mc));
            int h = Math.max(1, e.height(mc));
            if (mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h) {
                return e;
            }
        }
        return null;
    }

    private static final class HudState {
        final int x, y;
        final boolean enabled;
        private HudState(int x, int y, boolean enabled) {
            this.x = x; this.y = y; this.enabled = enabled;
        }
    }
}