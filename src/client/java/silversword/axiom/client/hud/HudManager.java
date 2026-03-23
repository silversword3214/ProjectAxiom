package silversword.axiom.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import silversword.axiom.client.gui.core.Theme;
import silversword.axiom.client.gui.core.ThemeManager;
import silversword.axiom.client.gui.screen.ClickGuiScreen;
import silversword.axiom.client.hud.core.HudContext;
import silversword.axiom.client.render.rendersystem.Renderer2D;

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
        if (mc == null || mc.player == null) return;
        if (mc.screen instanceof ClickGuiScreen) return;

        Theme theme = ThemeManager.getCurrentTheme();
        float delta = tickCounter.getGameTimeDeltaTicks();
        HudContext ctx = new HudContext(mc, draw, theme, delta);

        // Aloita fillien keräys
        Renderer2D.COLOR.begin();

        // Kerää fillit ja tekstit (tekstit tallennetaan ctx:n listaan)
        for (HudElement e : elements) {
            if (!e.enabled()) continue;
            e.render(ctx, tickCounter);
        }

        // Renderöi fillit
        Renderer2D.COLOR.render();

        // Piirrä tallennetut tekstit fillien päälle
        ctx.renderTexts();
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