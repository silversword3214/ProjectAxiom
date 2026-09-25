package silversword.axiom.client.hud.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import silversword.axiom.client.config.ClickGuiConfigManager;
import silversword.axiom.client.hud.BaseHudElement;
import silversword.axiom.client.hud.core.HudContext;
import silversword.axiom.client.render.rendersystem.utils.color.rainbow.RainbowPalette;

/**
 * HealthIndicator — näyttää targetin healthin crosshairin oikealla puolella.
 *
 * Layout:
 *      [♥ HP-teksti]           ← ylhäällä
 *   [Ikoni] [████████████]     ← alhaalla, ikoni vasemmalla, bar oikealla
 *
 * Ominaisuudet:
 *   - Animoitu health bar (menetetty HP näkyy punaisena phantomina joka liukuu alas)
 *   - Toimii pelaajille ja mobeille (spawn egg ikoni)
 *   - Kaikki pyöristettyjä
 */
public final class HealthIndicatorHudComponent extends BaseHudElement {

    // ── Asetukset (moduuli päivittää) ────────────────────────────
    private float scale = 1.0f;
    private double maxRange = 48.0;
    private boolean playersOnly = false;
    private boolean showHeartCounter = true;
    private boolean showNumericHp = true;
    private boolean showEntityIcon = true;
    private int backgroundColor = 0xCC1A1A1A;
    private int borderColor = 0xFF6A00FF;
    private int textColor = 0xFFFFFFFF;

    // Rainbow-tekstuuri — generoidaan kerran per frame
    private net.minecraft.client.renderer.texture.DynamicTexture gradientTexture;
    private net.minecraft.resources.Identifier gradientTexId;
    private com.mojang.blaze3d.platform.NativeImage gradientImage;
    private int gradientW = 0;
    private int gradientH = 0;
    private static final int SUPERSAMPLE = 4;

    // ── Animaatio ────────────────────────────────────────────────
    private static final long ANIM_DURATION_MS = 450;
    private static final long LINGER_MS = 1500;
    private static final long FADE_MS = 300;

    private int lockedId = -1;
    private long lastSeenMs = 0L;
    private float lastMaxHp = 20f;
    private float currentHp = 20f;
    private float prevHp = 20f;
    private float displayedHp = 20f;
    private long lastHpChangeMs = 0L;

    private int lastW = 100, lastH = 40;

    private boolean rainbowHealthBar = false;
    private float rainbowSpeed = 1.0f;

    public void setRainbowHealthBar(boolean v) { rainbowHealthBar = v; }
    public void setRainbowSpeed(float v) { rainbowSpeed = Math.max(0.1f, Math.min(5f, v)); }

    public HealthIndicatorHudComponent() {
        super("HealthIndicator", 10, 10);
        this.enabled = false;
    }

    // ── Setterit ─────────────────────────────────────────────────
    public void setScale(float v) { scale = clamp(v, 0.5f, 3.0f); }
    public void setMaxRange(double v) { maxRange = Math.max(0, v); }
    public void setPlayersOnly(boolean v) { playersOnly = v; }
    public void setShowHeartCounter(boolean v) { showHeartCounter = v; }
    public void setShowNumericHp(boolean v) { showNumericHp = v; }
    public void setShowEntityIcon(boolean v) { showEntityIcon = v; }
    public void setBackgroundColor(int c) { backgroundColor = c; }
    public void setBorderColor(int c) { borderColor = c; }
    public void setTextColor(int c) { textColor = c; }

    @Override public boolean isModuleControlled() { return true; }
    @Override public int width(Minecraft mc) { return Math.max(1, lastW); }
    @Override public int height(Minecraft mc) { return Math.max(1, lastH); }

    // ── Render ───────────────────────────────────────────────────

    @Override
    public void render(HudContext ctx, DeltaTracker tickCounter) {
        if (!enabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        long now = System.currentTimeMillis();

        // 1) Etsi target
        LivingEntity target = findTarget(mc);
        if (target != null && target.isAlive()) {
            boolean eligible = (!playersOnly || target instanceof Player)
                    && (maxRange <= 0 || mc.player.distanceTo(target) <= maxRange);

            if (eligible) {
                if (lockedId != target.getId()) {
                    // Uusi target — resetoi animaatio
                    lockedId = target.getId();
                    lastMaxHp = Math.max(1, target.getMaxHealth());
                    currentHp = target.getHealth();
                    prevHp = currentHp;
                    displayedHp = currentHp;
                    lastHpChangeMs = now;
                } else {
                    // Päivitä HP jos muuttui
                    float newHp = target.getHealth();
                    if (Math.abs(newHp - currentHp) > 0.01f) {
                        prevHp = currentHp;
                        currentHp = newHp;
                        lastHpChangeMs = now;
                    }
                    lastMaxHp = Math.max(1, target.getMaxHealth());
                }
                lastSeenMs = now;
            }
        }

        // 2) Tarkista pitäisikö vielä piirtää
        if (lockedId < 0) return;
        long elapsed = now - lastSeenMs;
        if (elapsed > LINGER_MS) { lockedId = -1; return; }

        Entity e = mc.level.getEntity(lockedId);
        if (!(e instanceof LivingEntity living) || !living.isAlive()) {
            lockedId = -1;
            return;
        }

        // 3) Päivitä animaatio
        long sinceChange = now - lastHpChangeMs;
        float t = Math.min(1f, sinceChange / (float) ANIM_DURATION_MS);
        float eased = 1f - (1f - t) * (1f - t); // ease out quad
        displayedHp = prevHp + (currentHp - prevHp) * eased;

        // 4) Alpha — fade out lopussa
        float alpha = 1.0f;
        if (elapsed > LINGER_MS - FADE_MS) {
            alpha = clamp01((LINGER_MS - elapsed) / (float) FADE_MS);
        }

        drawHud(ctx, mc, living, alpha);
    }

    @Override
    public void renderEdit(HudContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            lastMaxHp = Math.max(1, mc.player.getMaxHealth());
            currentHp = mc.player.getHealth();
            displayedHp = currentHp;
            prevHp = currentHp;
            drawHud(ctx, mc, mc.player, 1.0f);
        }
    }

    // ── Piirto ───────────────────────────────────────────────────

    private void drawHud(HudContext ctx, Minecraft mc, LivingEntity target, float alpha) {
        int fontH = ctx.fontHeight();
        int padding = (int) (4 * scale);
        int radius = (int) (4 * scale);
        int gap = (int) (5 * scale);
        int barH = (int) (8 * scale);
        int barW = (int) (96 * scale);              // ← leveämpi bar
        int heartSize = (int) (fontH * scale);
        int iconTarget = (int) (26 * scale);        // ← iconin minimikorkeus
        int raise = (int) (2 * scale);              // ← nosta sydän + ikoni ylemmäs

        String hpText = String.format("%.1f", currentHp);
        String maxText = String.format("/%.0f", lastMaxHp);
        int hpW = (int) (ctx.textWidth(hpText) * scale);
        int maxW = (int) (ctx.textWidth(maxText) * scale);

        // ── Ylärivi: sydän + HP-teksti ──────────────────────────
        int topRowW = 0;
        int topRowH = 0;
        if (showHeartCounter) {
            topRowW += heartSize + (int) (3 * scale);
            topRowH = Math.max(topRowH, heartSize);
        }
        if (showNumericHp) {
            topRowW += hpW + maxW;
            topRowH = Math.max(topRowH, (int) (fontH * scale));
        }

        // Oikean sarakkeen leveys = max(ylärivi, bar)
        int rightColW = Math.max(topRowW, barW);

        // Sisällön korkeus: ikoni määrittää, mutta ei pienempi kuin ylärivi + bar
        int contentH = Math.max(iconTarget, topRowH + (int) (3 * scale) + barH);
        int iconSize = contentH;                     // ← ikoni = koko korkeus

        int contentW = (showEntityIcon ? iconSize + gap : 0) + rightColW;

        int totalW = contentW + padding * 2;
        int totalH = contentH + padding * 2;

        lastW = totalW;
        lastH = totalH;

        int x0 = x;
        int y0 = y;

        int bg = applyAlpha(backgroundColor, alpha);
        int border = applyAlpha(borderColor, alpha);
        int txt = applyAlpha(textColor, alpha);
        int dimTxt = applyAlpha(0xFFAAAAAA, alpha);

        // Tausta
        ctx.fillRounded(x0, y0, totalW, totalH, radius, bg);
        ctx.drawRoundedOutline(x0, y0, totalW, totalH, radius, border, Math.max(1.0, scale));

        int cursorX = x0 + padding;

        // ── Ikoni vasemmalla, täysi korkeus, nostettu ───────────
        if (showEntityIcon) {
            drawEntityIcon(ctx, target, cursorX, y0 + padding - raise + 1, iconSize, alpha);
            cursorX += iconSize + gap;
        }

        // ── Oikea sarake ────────────────────────────────────────
        int rightX = cursorX;
        int rightY = y0 + padding;

        // Ylärivi (nostettu)
        int topTextY = rightY + (topRowH - (int) (fontH * scale)) / 2 - raise + 3;

        if (showHeartCounter) {
            int heartY = rightY + (topRowH - heartSize) / 2 - raise;
            drawHeart(ctx, rightX, heartY, heartSize, applyAlpha(0xFFFFFFFF, alpha));
            rightX += heartSize + (int) (3 * scale);
        }
        if (showNumericHp) {
            ctx.drawScaledText(hpText, rightX, topTextY, txt, true, scale);
            ctx.drawScaledText(maxText, rightX + hpW, topTextY, dimTxt, true, scale);
        }

        // Alarivi: health bar (rounded)
        int barY = rightY + topRowH + (int) (3 * scale);
        drawHealthBar(ctx, cursorX, barY, rightColW, barH, alpha, Math.max(2, barH / 2));
    }

    private void drawHealthBar(HudContext ctx, int x, int y, int w, int h, float alpha, int radius) {
        float pct = clamp01(currentHp / lastMaxHp);
        float displayPct = clamp01(displayedHp / lastMaxHp);

        // Tausta
        ctx.fillRounded(x, y, w, h, radius, applyAlpha(0xFF1A1A1A, alpha));

        // Phantom — punainen, liukuu alas
        if (displayPct > pct + 0.005f) {
            int phantomW = Math.max(1, (int) (w * displayPct));
            ctx.fillRounded(x, y, phantomW, h, radius,
                    applyAlpha(0xFFFF3355, alpha));
        }

        // Täyttö
        if (pct > 0.005f) {
            int fillW = Math.max(1, (int) (w * pct));

            if (rainbowHealthBar) {
                // ── Rainbow: piirretään segmentteinä paletista ──
                drawRainbowFill(ctx, x, y, fillW, h, alpha);
            } else {
                // ── Tavallinen: HP-tason mukaan ──
                ctx.fillRounded(x, y, fillW, h, radius,
                        applyAlpha(hpColor(pct), alpha));
            }
        }

        // Reunus
        ctx.drawRoundedOutline(x, y, w, h, radius,
                applyAlpha(0x40000000, alpha), 1.0);
    }

    /**
     * Piirtää rainbow-täytön paletin avulla.
     * Segmentit ottavat värinsä paletista x-position mukaan.
     * Aika liikuttaa koko gradienttia → animoitu wave.
     */
    private void drawRainbowFill(HudContext ctx, int x, int y, int w, int h, float alpha) {
        if (w <= 0 || h <= 0) return;

        RainbowPalette palette;
        try {
            palette = ClickGuiConfigManager.getRainbowPalette();
        } catch (Throwable t) {
            palette = null;
        }
        if (palette == null) return;

        if (!ensureGradientTexture(w, h)) return;

        int texW = w * SUPERSAMPLE;
        int texH = h * SUPERSAMPLE;

        long now = System.currentTimeMillis();
        float period = 5000f / Math.max(0.1f, rainbowSpeed);
        float timePos = (now % (long) period) / period;

        // Täytetään 4x resoluutiolla
        for (int px = 0; px < texW; px++) {
            // Väri — sama xFrac koko skaalalla (0..1 välillä koko barin leveydellä)
            // Väri — toistetaan paletti N kertaa barin leveydellä
            float xFrac = px / (float) texW;
            float density = 3.0f;                                // ← säädä tästä
            float t = (timePos + xFrac * density) % 2.0f;
            if (t < 0) t += 1.0f;
            int argb = palette.getColorInterpolatedLoop(t);

            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;

            for (int py = 0; py < texH; py++) {
                // Maski — 4x tarkkuudella, mutta parametrit suhteessa w/h:hon
                double alphaMask = pillMaskHi(px, py, texW, texH, w, h);

                int finalA = (int) Math.round(255.0 * alphaMask);
                int abgr = (finalA << 24) | (b << 16) | (g << 8) | r;
                gradientImage.setPixelABGR(px, py, abgr);
            }
        }

        gradientTexture.upload();

        // Piirrä skaalattuna alas → bilinear smoothaa reunat
        int texColor = applyAlpha(0xFFFFFFFF, alpha);
        ctx.renderer.drawTexture(gradientTexId, (float) x, (float) y, (float) w, (float) h, texColor);
    }

    /** Palauttaa alpha-arvon 0..1 pill-muodon mukaan. */
    /**
     * Laskee pill-maskin arvon pikselille. Toimii sekä 1x että korkeammalla
     * resoluutiolla. Käyttää SDF-etäisyyttä jotta saadaan sama anti-aliasing
     * kuin RenderCoren omissa rounded recteissä.
     */
    private static double pillMaskHi(int texPx, int texPy, int texW, int texH, int logicalW, int logicalH) {
        // Muunnetaan takaisin loogisiin koordinaatteihin (0..logicalW, 0..logicalH)
        double cx = (texPx + 0.5) * logicalW / texW;
        double cy = (texPy + 0.5) * logicalH / texH;

        double r = logicalH / 2.0;
        if (logicalW < r * 2) r = logicalW / 2.0;

        // SDF pillille: rounded rect jossa radius = h/2
        // SDF-arvo = etäisyys pintaan (positiivinen ulkona, negatiivinen sisällä)
        double halfW = logicalW / 2.0;
        double halfH = logicalH / 2.0;
        double centerX = halfW;
        double centerY = halfH;

        double dx = Math.abs(cx - centerX) - (halfW - r);
        double dy = Math.abs(cy - centerY) - (halfH - r);

        double outX = Math.max(dx, 0);
        double outY = Math.max(dy, 0);
        double outsideDist = Math.sqrt(outX * outX + outY * outY);
        double insideDist = Math.min(Math.max(dx, dy), 0);

        double sdf = outsideDist + insideDist - r;

        // Anti-aliasing: 1px leveä pehmeä reuna
        // (korkealla resoluutiolla tämä on ~0.25 loogista pikseliä, mutta
        // kun bilinear skaalaa alas, se muuttuu sileäksi)
        double aaWidth = 1.0 / SUPERSAMPLE;  // yksi texeli = 1/4 loogista pikseliä

        if (sdf <= -aaWidth) return 1.0;
        if (sdf >= aaWidth)  return 0.0;

        // Pehmeä siirtymä
        return (aaWidth - sdf) / (2.0 * aaWidth);
    }

    /** Varmistaa että tekstuuri on oikean kokoinen. */
    private boolean ensureGradientTexture(int w, int h) {
        int texW = w * SUPERSAMPLE;
        int texH = h * SUPERSAMPLE;

        if (gradientTexture != null && gradientW == texW && gradientH == texH) return true;

        // Vapauta vanha
        if (gradientTexture != null) {
            try {
                net.minecraft.client.Minecraft.getInstance()
                        .getTextureManager().release(gradientTexId);
            } catch (Throwable ignored) {}
            gradientTexture = null;
        }

        try {
            gradientW = texW;
            gradientH = texH;

            gradientImage = new com.mojang.blaze3d.platform.NativeImage(
                    com.mojang.blaze3d.platform.NativeImage.Format.RGBA, texW, texH, false);

            gradientTexId = net.minecraft.resources.Identifier.fromNamespaceAndPath(
                    "projectaxiom", "healthindicator_rainbow_"
                            + Integer.toHexString(System.identityHashCode(this)));

            gradientTexture = new net.minecraft.client.renderer.texture.DynamicTexture(
                    () -> "healthindicator_rainbow", gradientImage);

            // LINEAARINEN suodatus — pakollinen AA:lle downsamplauksessa
            gradientTexture.sampler = com.mojang.blaze3d.systems.RenderSystem
                    .getSamplerCache()
                    .getClampToEdge(com.mojang.renderpearl.api.textures.FilterMode.LINEAR);

            net.minecraft.client.Minecraft.getInstance()
                    .getTextureManager().register(gradientTexId, gradientTexture);
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    private static final Identifier HEART_TEXTURE = Identifier.fromNamespaceAndPath(
            "projectaxiom", "textures/icons/heart.png");

    private void drawHeart(HudContext ctx, int x, int y, int size, int color) {
        ctx.renderer.drawTexture(HEART_TEXTURE, x, y, size, size, color);
    }

    private void drawEntityIcon(HudContext ctx, LivingEntity entity, int x, int y, int size, float alpha) {
        ItemStack egg = getSpawnEgg(entity);
        if (!egg.isEmpty()) {
            ctx.item(egg, x, y, size);
            return;
        }

        // Fallback: värillinen ympyrä + alkukirjain
        int color = colorForEntity(entity);
        ctx.fillRounded(x, y, size, size, size / 4, applyAlpha(color, alpha));

        String name = entity.getName().getString();
        if (!name.isEmpty()) {
            String initial = name.substring(0, 1).toUpperCase();
            int tw = (int) (ctx.textWidth(initial) * 0.8f);
            int tx = x + (size - tw) / 2;
            int ty = y + (size - ctx.fontHeight()) / 2;
            ctx.drawScaledText(initial, tx, ty, applyAlpha(0xFFFFFFFF, alpha), true, 0.8f);
        }
    }

    // ── Apumetodit ───────────────────────────────────────────────

    private static LivingEntity findTarget(Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (hit instanceof EntityHitResult ehr
                && ehr.getEntity() instanceof LivingEntity le) {
            return le;
        }
        return null;
    }

    /** Etsii spawn eggin nimeämiskonvention perusteella: <mob>_spawn_egg */
    private static ItemStack getSpawnEgg(LivingEntity entity) {
        try {
            Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            if (typeId == null) return ItemStack.EMPTY;

            Identifier eggId = Identifier.fromNamespaceAndPath(
                    typeId.getNamespace(), typeId.getPath() + "_spawn_egg");

            var item = BuiltInRegistries.ITEM.getValue(eggId);
            if (item == null) return ItemStack.EMPTY;
            return new ItemStack(item);
        } catch (Throwable t) {
            return ItemStack.EMPTY;
        }
    }

    private static int colorForEntity(LivingEntity entity) {
        int h = entity.getType().getDescriptionId().hashCode();
        float hue = (h & 0xFFFF) / 65536.0f;
        int rgb = java.awt.Color.HSBtoRGB(hue, 0.6f, 0.7f);
        return 0xFF000000 | (rgb & 0x00FFFFFF);
    }

    private static int hpColor(float pct) {
        if (pct >= 0.66f) return 0xFF44DD44;
        if (pct >= 0.33f) return 0xFFFFCC00;
        return 0xFFFF3344;
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static int applyAlpha(int argb, float mul) {
        int a = (argb >>> 24) & 0xFF;
        int na = clamp((int) (a * mul), 0, 255);
        return (na << 24) | (argb & 0x00FFFFFF);
    }

    public void dispose() {
        if (gradientTexture != null && gradientTexId != null) {
            try {
                net.minecraft.client.Minecraft.getInstance()
                        .getTextureManager().release(gradientTexId);
            } catch (Throwable ignored) {}
            gradientTexture = null;
            gradientImage = null;
            gradientTexId = null;
        }
    }
}