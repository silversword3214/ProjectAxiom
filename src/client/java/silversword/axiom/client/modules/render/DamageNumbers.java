package silversword.axiom.client.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import silversword.axiom.client.config.ClickGuiConfigManager;
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
import silversword.axiom.client.render.font.CustomTextRenderer;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.render.rendersystem.utils.color.rainbow.RainbowPalette;
import silversword.axiom.client.render.rendersystem.utils.render.NametagUtils;
import silversword.axiom.client.setting.*;

import java.util.*;

public final class DamageNumbers extends AxiomMod implements ColorConfigurable, KeybindConfigurable {

    private static final String KILL_TEXT = "KILL";

    private final Minecraft mc = Minecraft.getInstance();

    // Kaikki aktiiviset numerot
    private final List<DamageEntry> entries = new ArrayList<>();
    // Seuranta: entity id → edellinen health
    private final Map<Integer, Float> lastHealth = new HashMap<>();

    // Maailma, jotta tiedämme milloin resetoida
    private Level lastLevel;

    // ── Settings ─────────────────────────────────────────────────
    private final SettingNumber scale;
    private final SettingNumber durationMs;
    private final SettingNumber riseDist;
    private final SettingNumber maxEntries;
    private final SettingBoolean randomSpread;
    private final SettingBoolean showBigLabel;
    private final SettingNumber bigThreshold;
    private final SettingBoolean showKillText;
    private final SettingBoolean shadow;
    private final SettingNumber popStrength;
    private final SettingBoolean rainbow;
    private final SettingNumber rainbowSpeed;

    final SettingColor normalColor;
    final SettingColor bigColor;
    final SettingColor killColor;

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    public DamageNumbers() {
        super("Damage Numbers", "Floating damage numbers above targets", ModuleCategory.RENDER);

        scale = new SettingNumber("Scale", 0.5, 3.0, 0.1, 1.2);
        durationMs = new SettingNumber("Duration (ms)", 300, 5000, 50, 1200);
        riseDist = new SettingNumber("Rise Distance", 0.0, 3.0, 0.1, 1.0);
        maxEntries = new SettingNumber("Max Entries", 5, 200, 5, 40);
        randomSpread = new SettingBoolean("Random Spread", true);
        popStrength = new SettingNumber("Pop Strength", 0.0, 2.0, 0.1, 0.5);

        showBigLabel = new SettingBoolean("Show Big Marker", true);
        bigThreshold = new SettingNumber("Big Threshold", 1.0, 30.0, 0.5, 8.0);

        showKillText = new SettingBoolean("Show Kill Text", true);
        shadow = new SettingBoolean("Text Shadow", true);

        rainbow = new SettingBoolean("Rainbow", false);
        rainbowSpeed = new SettingNumber("Rainbow Speed", 0.1, 5.0, 0.1, 1.0);

        normalColor = new SettingColor("Normal Color", new Color(0xFFFFFFFF));   // valkoinen
        bigColor    = new SettingColor("Big Color",    new Color(0xFFFFB84C));   // oranssi
        killColor   = new SettingColor("Kill Color",   new Color(0xFFFFD700));   // kultainen

        addSetting(scale);
        addSetting(durationMs);
        addSetting(riseDist);
        addSetting(maxEntries);
        addSetting(randomSpread);
        addSetting(popStrength);
        addSetting(showBigLabel);
        addSetting(bigThreshold);
        addSetting(showKillText);
        addSetting(shadow);
        addSetting(rainbow);
        addSetting(rainbowSpeed);

        addHiddenSetting(normalColor.getSetting());
        addHiddenSetting(bigColor.getSetting());
        addHiddenSetting(killColor.getSetting());
        addHiddenSetting(toggleKey);
    }

    @Override
    public SettingKeybind getKeybind() { return toggleKey; }

    @Override
    protected void onEnable() {
        entries.clear();
        lastHealth.clear();
        lastLevel = null;
    }

    @Override
    protected void onDisable() {
        entries.clear();
        lastHealth.clear();
        lastLevel = null;
    }

    // ── Tick: havaitse damage health-droppien kautta ─────────────



    private void spawnDamage(LivingEntity target, float damage, long now) {
        if (entries.size() >= (int) maxEntries.getValue()) {
            if (!entries.isEmpty()) entries.remove(0);
        }

        Vec3 base = target.position().add(0, target.getBbHeight() * 0.7, 0);

        if (randomSpread.get()) {
            Random r = new Random(target.getId() * 31L + now);
            base = base.add((r.nextDouble() - 0.5) * 0.6, 0, (r.nextDouble() - 0.5) * 0.6);
        }

        boolean big = damage >= bigThreshold.getValue();
        DamageKind kind = big ? DamageKind.BIG : DamageKind.NORMAL;
        String text = formatDamage(damage, big);

        entries.add(new DamageEntry(text, kind, base, now));
    }

    private String formatDamage(float dmg, boolean big) {
        String s;
        if (Math.abs(dmg - Math.round(dmg)) < 0.05f) {
            s = String.valueOf(Math.round(dmg));
        } else {
            s = String.format(Locale.US, "%.1f", dmg);
        }
        if (big && showBigLabel.get()) s += "!";
        return s;
    }

    // ── Render ───────────────────────────────────────────────────

    // ── Tick: poistettu — kaikki logiikka on renderissä ─────────
    @Override
    protected void onTick() {
        // ei mitään — käsitellään renderissä
    }

    // ── Render + havainnointi ────────────────────────────────────

    @Subscribe
    private void onRender2D(Render2DEvent event) {
        if (!isEnabled()) return;
        if (event.getGuiGraphics() == null) return;
        if (mc.level == null || mc.player == null) return;

        detectDamage();

        if (entries.isEmpty()) return;

        long now = System.currentTimeMillis();
        long dur = Math.max(1L, (long) durationMs.getValue());

        for (DamageEntry e : entries) {
            long age = now - e.spawnTime;
            if (age < 0) age = 0;
            float t = (float) age / dur;
            if (t >= 1.0f) continue;

            float ease = 1f - (1f - t) * (1f - t);
            float yOffset = (float) (ease * riseDist.getValue());
            float alpha = t < 0.7f ? 1f : (1f - (t - 0.7f) / 0.3f);
            if (alpha <= 0f) continue;

            float scl = (float) scale.getValue();
            if (e.kind == DamageKind.BIG)  scl *= 1.4f;
            if (e.kind == DamageKind.KILL) scl *= 1.6f;

            float pop = (float) popStrength.getValue();
            if (pop > 0f && t < 0.15f) {
                scl *= 1f + pop * (0.15f - t) / 0.15f;
            }

            Vec3 worldPos = e.pos.add(0, yOffset, 0);
            Vec3 screenPos = NametagUtils.worldToScreen(
                    worldPos,
                    event.getScreenWidth(),
                    event.getScreenHeight()
            );



            if (screenPos == null) continue;

            // Clampaa ruudun sisään — numerot eivät katoa yläreunan yli
            double screenX = screenPos.x;
            double screenY = screenPos.y;
            double margin = 8;
            if (screenY < margin) screenY = margin;
            if (screenY > event.getScreenHeight() - margin)
                screenY = event.getScreenHeight() - margin;
            if (screenX < margin) screenX = margin;
            if (screenX > event.getScreenWidth() - margin)
                screenX = event.getScreenWidth() - margin;

            int color = resolveColor(e, alpha);

            // ──────────────────────────────────────────────────────

            if (color == 0) continue;

            renderCentered(event, e.text, screenX, screenY, color, scl, shadow.get());
        }
    }

    /** Havaitsemislogiikka — kutsutaan joka frame renderistä. */
    private void detectDamage() {
        // Maailman vaihto → reset
        if (mc.level != lastLevel) {
            entries.clear();
            lastHealth.clear();
            lastLevel = mc.level;
        }

        long now = System.currentTimeMillis();
        long maxAge = (long) durationMs.getValue();

        // Poista vanhentuneet
        entries.removeIf(e -> now - e.spawnTime > maxAge);

        // Käy kaikki renderöitävät entityt läpi
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity le)) continue;
            if (e == mc.player) continue;

            int id = e.getId();
            float cur = le.getHealth();
            Float prevObj = lastHealth.get(id);

            if (!le.isAlive()) {
                if (prevObj != null && prevObj > 0 && showKillText.get()) {
                    Vec3 pos = e.position().add(0, e.getBbHeight() + 0.4, 0);
                    entries.add(new DamageEntry(KILL_TEXT, DamageKind.KILL, pos, now));
                }
                lastHealth.remove(id);
                continue;
            }

            if (prevObj != null) {
                float prev = prevObj;
                if (cur < prev - 0.05f) {
                    float dmg = prev - cur;
                    spawnDamage(le, dmg, now);
                }
            }

            lastHealth.put(id, cur);
        }
    }

    private int resolveColor(DamageEntry e, float alpha) {
        int base;
        switch (e.kind) {
            case BIG  -> base = bigColor.getCurrentColor().getARGB();
            case KILL -> base = killColor.getCurrentColor().getARGB();
            default   -> base = normalColor.getCurrentColor().getARGB();
        }

        if (rainbow.get()) {
            try {
                RainbowPalette palette = ClickGuiConfigManager.getRainbowPalette();
                if (palette != null) {
                    long nowMs = System.currentTimeMillis();
                    float period = 5000f / Math.max(0.1f, (float) rainbowSpeed.getValue());
                    float basePos = (nowMs % (long) period) / period;
                    float offset = (e.spawnTime % 1000L) / 1000.0f;
                    float t = (basePos + offset) % 1.0f;
                    if (t < 0f) t += 1.0f;
                    base = palette.getColorInterpolatedLoop(t);
                }
            } catch (Throwable ignored) {}
        }

        return applyAlpha(base, alpha);
    }

    private static int applyAlpha(int argb, float mul) {
        int a = (argb >>> 24) & 0xFF;
        int na = Math.max(0, Math.min(255, Math.round(a * mul)));
        return (na << 24) | (argb & 0x00FFFFFF);
    }

    private void renderCentered(Render2DEvent event, String text,
                                double cx, double cy, int color,
                                float scaleVal, boolean useShadow) {
        TextRenderer tr = TextRenderer.get();
        tr.begin(scaleVal, false, true);
        try {
            double w = tr.getWidth(text, false);
            double h = tr.getHeight(false);
            double x = cx - w / 2.0;
            double y = cy - h / 2.0;
            Color c = new Color(color);
            if (tr instanceof CustomTextRenderer ctr) {
                ctr.render(event.getGuiGraphics(), text, x, y, c, useShadow);
            } else {
                tr.render(text, x, y, c, useShadow);
            }
        } finally {
            tr.end();
        }
    }

    // ── ColorConfigurable ─────────────────────────────────────────

    @Override
    public List<NamedColor> getColors() {
        return Arrays.asList(
                new NamedColor("Normal", normalColor),
                new NamedColor("Big Damage", bigColor),
                new NamedColor("Kill", killColor)
        );
    }

    @Override
    public void openColorEditor() {
        WindowFactory factory = AxiomMod.getWindowFactory();
        if (factory == null) return;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        UiComponent content = new ColorCustomizerView(this);
        factory.openCustomWindow("damagenumbers_colors", "Damage Numbers Colors", sw, sh, content);
    }

    // ── Sisäiset ──────────────────────────────────────────────────

    private enum DamageKind { NORMAL, BIG, KILL }

    private static final class DamageEntry {
        final String text;
        final DamageKind kind;
        final Vec3 pos;
        final long spawnTime;

        DamageEntry(String text, DamageKind kind, Vec3 pos, long spawnTime) {
            this.text = text;
            this.kind = kind;
            this.pos = pos;
            this.spawnTime = spawnTime;
        }
    }
}