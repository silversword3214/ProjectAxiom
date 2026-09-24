package silversword.axiom.client.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameType;
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
import silversword.axiom.client.modules.moduleutils.TargetGroup;
import silversword.axiom.client.render.font.CustomTextRenderer;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.render.rendersystem.utils.render.NametagUtils;
import silversword.axiom.client.setting.*;

import java.util.*;

public final class NameTags extends AxiomMod implements ColorConfigurable, KeybindConfigurable {
    private final Minecraft mc = Minecraft.getInstance();

    private static Render2DEvent lastProcessedEvent = null;

    /**
     * Pystysuuntainen siirto, joka kompensoi sen, että CustomTextRenderer.getHeight()
     * palauttaa koko rivilaatikon korkeuden (sis. descender-tilan), kun taas
     * Font.render() sijoittaa glyyfit rivilaatikon yläreunaan.
     * Ilman tätä teksti näyttää olevan liian korkealla laatikossa.
     */
    private static final double TEXT_VSHIFT_FACTOR = 3.5;

    // --- Settings -------------------------------------------------
    private final SettingNumber scale;
    private final SettingSlider renderDistance;
    private final SettingBoolean ignoreSelf;
    private final SettingBoolean ignoreFriends;
    private final SettingBoolean culling;
    private final SettingNumber maxCullRange;
    private final SettingNumber maxCullCount;
    private final SettingNumber nameOffset;

    // Colors
    final SettingColor textColor;
    final SettingColor background;
    final SettingColor outline;

    // Filters
    private final SettingBoolean drawPlayers;
    private final SettingBoolean drawHostile;
    private final SettingBoolean drawPassive;
    private final SettingBoolean drawNeutral;
    private final SettingBoolean drawWater;
    private final SettingBoolean drawBoss;
    private final SettingBoolean drawItems;
    private final SettingBoolean drawItemFrames;
    private final SettingBoolean drawTNT;

    // Background mode
    private final SettingMode bgMode;

    // Player-specific toggles
    private final SettingBoolean showPing;
    private final SettingBoolean showDistance;
    private final SettingBoolean showGamemode;
    private final SettingBoolean showHealth;
    private final SettingBoolean showArmor;

    public final SettingKeybind toggleKey = new SettingKeybind("Toggle Key", 0);

    // --------------------------------------------------------------
    private final List<Entity> entityList = new ArrayList<>();

    private static final Set<TargetGroup> HANDLED_GROUPS = EnumSet.of(
            TargetGroup.PLAYER, TargetGroup.HOSTILE, TargetGroup.PASSIVE,
            TargetGroup.NEUTRAL, TargetGroup.WATER, TargetGroup.BOSS
    );

    private static final Identifier HEART_TEXTURE = Identifier.fromNamespaceAndPath(
            "projectaxiom", "textures/icons/heart.png");

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public NameTags() {
        super("NameTags", "Renders custom nametags above entities", ModuleCategory.RENDER);

        scale = new SettingNumber("Scale", 0.1, 3.0, 0.1, 1.5);
        renderDistance = new SettingSlider("Render Distance",
                new double[]{16, 32, 64, 96, 128, 256, 512}, 96);
        ignoreSelf = new SettingBoolean("Ignore Self", true);
        ignoreFriends = new SettingBoolean("Ignore Friends", false);
        culling = new SettingBoolean("Culling", false);
        maxCullRange = new SettingNumber("Culling Range", 0, 200, 20, 1);
        maxCullCount = new SettingNumber("Culling Count", 1, 100, 50, 1);
        nameOffset = new SettingNumber("Name Offset", -1.0, 5.0, 0.1, 0.5);

        textColor = new SettingColor("Text Color", new Color(255, 255, 255, 255));
        background = new SettingColor("Background", new Color(0, 0, 0, 75));
        outline = new SettingColor("Outline", new Color(255, 255, 255, 255));

        drawPlayers = new SettingBoolean("Players", true);
        drawHostile = new SettingBoolean("Hostile Entities", true);
        drawPassive = new SettingBoolean("Passive Entities", true);
        drawNeutral = new SettingBoolean("Neutral Entities", true);
        drawWater = new SettingBoolean("Water Entities", true);
        drawBoss = new SettingBoolean("Boss Entities", true);
        drawItems = new SettingBoolean("Items", true);
        drawItemFrames = new SettingBoolean("Item Frames", true);
        drawTNT = new SettingBoolean("TNT timer", true);

        bgMode = new SettingMode("Background",
                new String[]{"None", "Filled", "Outline", "Rounded"}, "Filled");

        showPing = new SettingBoolean("Show Ping", true);
        showDistance = new SettingBoolean("Show Distance", false);
        showGamemode = new SettingBoolean("Show Gamemode", false);
        showHealth = new SettingBoolean("Show Health", true);
        showArmor = new SettingBoolean("Show Armor Icons", true);

        addHiddenSetting(textColor.getSetting());
        addHiddenSetting(background.getSetting());
        addHiddenSetting(outline.getSetting());
        addHiddenSetting(toggleKey);

        addSetting(nameOffset);
        addSetting(scale);
        addSetting(renderDistance);
        addSetting(ignoreSelf);
        addSetting(ignoreFriends);
        addSetting(culling);
        addSetting(maxCullRange);
        addSetting(maxCullCount);
        addSetting(bgMode);
        addSetting(drawPlayers);
        addSetting(drawHostile);
        addSetting(drawPassive);
        addSetting(drawNeutral);
        addSetting(drawWater);
        addSetting(drawBoss);
        addSetting(drawItems);
        addSetting(drawItemFrames);
        addSetting(drawTNT);
        addSetting(showPing);
        addSetting(showDistance);
        addSetting(showGamemode);
        addSetting(showHealth);
        addSetting(showArmor);
    }

    @Override
    public SettingKeybind getKeybind() {
        return toggleKey;
    }

    // --------------------------- Tick & Filter ----------------------
    @Override
    protected void onTick() {
        if (!isEnabled() || mc.level == null || mc.player == null) {
            entityList.clear();
            return;
        }

        entityList.clear();

        double maxDistSq = renderDistance.getValue() * renderDistance.getValue();
        Vec3 cameraPos = mc.gameRenderer.mainCamera().position();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player && ignoreSelf.get()) continue;
            if (!entity.isAlive()) continue;

            if (!shouldDrawEntity(entity)) continue;

            double distSq = entity.distanceToSqr(cameraPos);
            if (distSq > maxDistSq) continue;

            if (!culling.get() || distSq <= maxCullRange.getValue() * maxCullRange.getValue()) {
                entityList.add(entity);
            }
        }

        entityList.sort(Comparator.comparingDouble(e -> -e.distanceToSqr(cameraPos)));
    }

    private boolean shouldDrawEntity(Entity entity) {
        EntityType<?> type = entity.getType();

        if (entity instanceof LivingEntity) {
            TargetGroup group = TargetGroup.getGroup(entity);
            return switch (group) {
                case PLAYER -> drawPlayers.get();
                case HOSTILE -> drawHostile.get();
                case PASSIVE -> drawPassive.get();
                case NEUTRAL -> drawNeutral.get();
                case WATER -> drawWater.get();
                case BOSS -> drawBoss.get();
                default -> false;
            };
        }

        if (type == EntityTypes.ITEM) return drawItems.get();
        if (type == EntityTypes.ITEM_FRAME || type == EntityTypes.GLOW_ITEM_FRAME)
            return drawItemFrames.get();
        if (type == EntityTypes.TNT || type == EntityTypes.TNT_MINECART)
            return drawTNT.get();

        return false;
    }

    // --------------------------- 2D Renderöinti ---------------------
    @Subscribe
    private void onRender2D(Render2DEvent event) {
        if (event == lastProcessedEvent) return;
        lastProcessedEvent = event;

        if (event.getGuiGraphics() == null) return;
        if (!isEnabled() || entityList.isEmpty()) return;

        int count = getRenderCount();
        Vec3 cameraPos = mc.gameRenderer.mainCamera().position();

        for (int i = count - 1; i >= 0; i--) {
            Entity entity = entityList.get(i);

            double x = Mth.lerp(event.tickDelta, entity.xOld, entity.getX());
            double y = Mth.lerp(event.tickDelta, entity.yOld, entity.getY());
            double z = Mth.lerp(event.tickDelta, entity.zOld, entity.getZ());

            Vec3 worldPos = new Vec3(x, y + getHeight(entity) + nameOffset.getValue(), z);

            Vec3 screenPos = NametagUtils.worldToScreen(
                    worldPos,
                    event.getScreenWidth(),
                    event.getScreenHeight());
            if (screenPos == null) continue;

            double dist = Math.sqrt(entity.distanceToSqr(cameraPos));
            double distanceScale = Mth.clamp(1.0 - dist * 0.005, 0.8, 3.0);
            double finalScale = scale.getValue() * distanceScale;

            if (entity instanceof Player p) {
                renderPlayerNametag(event, p, screenPos.x, screenPos.y, finalScale);
            } else if (entity instanceof ItemEntity ie) {
                renderItemNametag(event, ie.getItem(), screenPos.x, screenPos.y, finalScale);
            } else if (entity instanceof ItemFrame f) {
                renderItemNametag(event, f.getItem(), screenPos.x, screenPos.y, finalScale);
            } else if (entity instanceof PrimedTnt tnt) {
                renderTntNametag(event, tnt.getFuse(), screenPos.x, screenPos.y, finalScale);
            } else if (entity instanceof MinecartTNT tnt && tnt.isPrimed()) {
                renderTntNametag(event, tnt.getFuse(), screenPos.x, screenPos.y, finalScale);
            } else if (entity instanceof LivingEntity le) {
                renderLivingNametag(event, le, screenPos.x, screenPos.y, finalScale);
            } else {
                renderGenericNametag(event, entity, screenPos.x, screenPos.y, finalScale);
            }
        }
    }

    // --------------------------- Apurit -----------------------------

    private int getRenderCount() {
        if (!culling.get()) return entityList.size();
        return (int) Math.min(maxCullCount.getValue(), entityList.size());
    }

    private double getHeight(Entity entity) {
        double height = entity.getEyeHeight(entity.getPose());
        EntityType<?> type = entity.getType();
        if (type == EntityTypes.ITEM
                || type == EntityTypes.ITEM_FRAME
                || type == EntityTypes.GLOW_ITEM_FRAME) {
            height += 0.2;
        } else {
            height += 0.5;
        }
        return height;
    }

    /** Mittaa tekstin leveys samalla renderöijällä ja skaalalla kuin piirto. */
    private double measureWidth(String text, double scaleVal) {
        if (text == null || text.isEmpty()) return 0;
        TextRenderer tr = TextRenderer.get();
        tr.begin(scaleVal, false, true);
        try {
            return tr.getWidth(text, false);
        } finally {
            tr.end();
        }
    }

    /** Mittaa rivilaatikon korkeus samalla renderöijällä ja skaalalla kuin piirto. */
    private double measureFontHeight(double scaleVal) {
        TextRenderer tr = TextRenderer.get();
        tr.begin(scaleVal, false, true);
        try {
            return tr.getHeight(false);
        } finally {
            tr.end();
        }
    }

    /**
     * Pystysuuntainen siirto, joka kompensoi sen, että Font.render() sijoittaa
     * glyyfit rivilaatikon YLÄREUNAAN, kun taas getHeight() palauttaa koko
     * laatikon korkeuden (sis. descender-tila alaosassa).
     */
    private double getTextVShift(double scaleVal) {
        return TEXT_VSHIFT_FACTOR * scaleVal;
    }

    /** Yhteinen tekstinpiirto. Lisää pystysuuntaisen siirron, jotta teksti
     *  asettuu visuaalisesti laatikon keskelle. */
    private void drawText(Render2DEvent event, String text,
                          double x, double y, Color color,
                          boolean shadow, double scaleVal) {
        var g = event.getGuiGraphics();
        TextRenderer tr = TextRenderer.get();
        tr.begin(scaleVal, false, true);
        try {
            double adjustedY = y + getTextVShift(scaleVal);
            if (tr instanceof CustomTextRenderer ctr) {
                ctr.render(g, text, x, adjustedY, color, shadow);
            } else {
                tr.render(text, x, adjustedY, color, shadow);
            }
        } finally {
            tr.end();
        }
    }

    // --------------------------- Player Nametag ---------------------

    private void renderPlayerNametag(Render2DEvent event, Player player,
                                     double screenX, double screenY, double finalScale) {
        var g = event.getGuiGraphics();
        var r = event.getRenderer();

        double fontHeight = measureFontHeight(finalScale);
        double vShift = getTextVShift(finalScale);

        // ---- Rivi 0: Terveys ----
        String healthText = "";
        double healthLineWidth = 0;
        double healthLineHeight = 0;
        double heartSize = 10 * finalScale;
        if (showHealth.get()) {
            healthText = String.valueOf(Math.round(player.getHealth()));
            double hw = measureWidth(healthText, finalScale);
            double hh = fontHeight;
            healthLineWidth = heartSize + (2 * finalScale) + hw;
            healthLineHeight = Math.max(heartSize, hh);
        }

        // ---- Rivi 1: Nimi + gamemode + ping + dist ----
        StringBuilder nameBuilder = new StringBuilder();
        if (showGamemode.get()) {
            GameType gm = getGameMode(player);
            String gmText = (gm == null) ? "BOT" : switch (gm) {
                case SPECTATOR -> "Sp";
                case SURVIVAL -> "S";
                case CREATIVE -> "C";
                case ADVENTURE -> "A";
                default -> "?";
            };
            nameBuilder.append("[").append(gmText).append("] ");
        }
        nameBuilder.append(player.getName().getString());
        if (showPing.get()) {
            nameBuilder.append(" [").append(getPing(player)).append("ms]");
        }
        if (showDistance.get()) {
            double dist = Math.round(distanceToCamera(player) * 10.0) / 10.0;
            nameBuilder.append(" ").append(dist).append("m");
        }
        String nameLine = nameBuilder.toString();
        double nameLineWidth = measureWidth(nameLine, finalScale);
        double nameLineHeight = fontHeight;

        // ---- Rivi 2: Armor-ikonit ----
        List<ItemStack> armorStacks = new ArrayList<>();
        if (showArmor.get()) {
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                ItemStack stack = player.getItemBySlot(slot);
                if (!stack.isEmpty()) armorStacks.add(stack);
            }
        }
        int armorCount = armorStacks.size();
        double armorLineHeight = 0;
        double armorLineWidth = 0;
        double iconSize = 16 * finalScale;
        double armorGap = 2 * finalScale;

        if (armorCount > 0) {
            armorLineWidth = armorCount * iconSize + (armorCount - 1) * armorGap;
            armorLineHeight = iconSize + (2 * finalScale);
        }

        // ---- Kokonaismitat ----
        double maxWidth = Math.max(healthLineWidth,
                Math.max(nameLineWidth, armorLineWidth));
        double padding = 4.0 * finalScale;
        double gap = 2.0 * finalScale;

        double totalHeight = padding;
        if (showHealth.get()) totalHeight += healthLineHeight + gap;
        totalHeight += nameLineHeight;
        if (armorCount > 0) totalHeight += gap + armorLineHeight;
        totalHeight += padding;

        double bgWidth = maxWidth + padding * 2;
        double bgHeight = totalHeight;
        double bgX = screenX - bgWidth / 2;
        double bgY = screenY - bgHeight / 2;

        drawBackground(event, bgX, bgY, bgWidth, bgHeight, finalScale);

        double yCursor = bgY + padding;

        // Terveysrivi
        if (showHealth.get()) {
            double healthX = bgX + padding;
            double hTextY = yCursor + (healthLineHeight - nameLineHeight) / 2;

            // Ikoni siirretään samalla pystysiirrolla kuin teksti, jotta ne
            // pysyvät linjassa.
            r.drawTexture(HEART_TEXTURE,
                    (float) healthX,
                    (float) (yCursor + (healthLineHeight - heartSize) / 2 + vShift),
                    (float) heartSize, (float) heartSize,
                    0xFFFFFFFF);

            drawText(event, healthText,
                    healthX + heartSize + 2 * finalScale,
                    hTextY,
                    getHealthColor(player),
                    false, finalScale);

            yCursor += healthLineHeight + gap;
        }

        // Nimirivi
        drawText(event, nameLine, bgX + padding, yCursor,
                textColor.getCurrentColor(), false, finalScale);
        yCursor += nameLineHeight + gap;

        // Armorrivi
        if (armorCount > 0) {
            double startX = bgX + padding;
            double iconY = yCursor + vShift;
            for (int i = 0; i < armorStacks.size(); i++) {
                ItemStack stack = armorStacks.get(i);
                double iconX = startX + i * (iconSize + armorGap);

                g.item(stack, (int) iconX, (int) iconY);

                if (stack.isDamageableItem()) {
                    float percent = (float) (stack.getMaxDamage() - stack.getDamageValue())
                            / stack.getMaxDamage();
                    int barColor = (percent >= 0.7f) ? 0xFF19FC19
                            : (percent >= 0.5f) ? 0xFFFFFF19
                            : (percent >= 0.2f) ? 0xFFFF6919
                            : 0xFFFF1919;

                    double barY = iconY + iconSize + 1 * finalScale;
                    r.drawRect((float) iconX, (float) barY,
                            (float) iconSize, (float) (2 * finalScale),
                            0x64000000);
                    r.drawRect((float) iconX, (float) barY,
                            (float) (iconSize * percent), (float) (2 * finalScale),
                            barColor);
                }
            }
        }
    }

    private Color getHealthColor(Player player) {
        float percent = player.getHealth() / player.getMaxHealth();
        if (percent <= 0.333f) return new Color(255, 25, 25);
        if (percent <= 0.666f) return new Color(255, 105, 25);
        return new Color(25, 252, 25);
    }

    // --------------------------- Item Nametag -----------------------

    private void renderItemNametag(Render2DEvent event, ItemStack stack,
                                   double screenX, double screenY, double finalScale) {
        if (stack.isEmpty()) return;

        String name = stack.getHoverName().getString();
        String count = " x" + stack.getCount();

        double nameWidth = measureWidth(name, finalScale);
        double countWidth = measureWidth(count, finalScale);
        double textHeight = measureFontHeight(finalScale);

        double padding = 4.0 * finalScale;
        double bgWidth = nameWidth + countWidth + padding * 2;
        double bgHeight = textHeight + padding * 2;
        double bgX = screenX - bgWidth / 2;
        double bgY = screenY - bgHeight / 2;

        drawBackground(event, bgX, bgY, bgWidth, bgHeight, finalScale);

        drawText(event, name, bgX + padding, bgY + padding,
                textColor.getCurrentColor(), false, finalScale);
        drawText(event, count, bgX + padding + nameWidth, bgY + padding,
                new Color(0xFFE8B923), false, finalScale);
    }

    // --------------------------- Living Nametag ---------------------

    private void renderLivingNametag(Render2DEvent event, LivingEntity entity,
                                     double screenX, double screenY, double finalScale) {
        String name = entity.getType().getDescription().getString();
        String healthText = " " + Math.round(entity.getHealth());

        double nameWidth = measureWidth(name, finalScale);
        double healthWidth = measureWidth(healthText, finalScale);
        double textHeight = measureFontHeight(finalScale);

        double padding = 4.0 * finalScale;
        double bgWidth = nameWidth + healthWidth + padding * 2;
        double bgHeight = textHeight + padding * 2;
        double bgX = screenX - bgWidth / 2;
        double bgY = screenY - bgHeight / 2;

        drawBackground(event, bgX, bgY, bgWidth, bgHeight, finalScale);

        float hp = entity.getHealth() / entity.getMaxHealth();
        int hColor = (hp <= 0.333f) ? 0xFFFF1919
                : (hp <= 0.666f) ? 0xFFFF6919
                : 0xFF19FC19;

        drawText(event, name, bgX + padding, bgY + padding,
                textColor.getCurrentColor(), false, finalScale);
        drawText(event, healthText, bgX + padding + nameWidth, bgY + padding,
                new Color(hColor), false, finalScale);
    }

    // --------------------------- Generic Nametag --------------------

    private void renderGenericNametag(Render2DEvent event, Entity entity,
                                      double screenX, double screenY, double finalScale) {
        String name = entity.getType().getDescription().getString();

        double textWidth = measureWidth(name, finalScale);
        double textHeight = measureFontHeight(finalScale);
        double padding = 4.0 * finalScale;

        double bgWidth = textWidth + padding * 2;
        double bgHeight = textHeight + padding * 2;
        double bgX = screenX - bgWidth / 2;
        double bgY = screenY - bgHeight / 2;

        drawBackground(event, bgX, bgY, bgWidth, bgHeight, finalScale);

        drawText(event, name, bgX + padding, bgY + padding,
                textColor.getCurrentColor(), false, finalScale);
    }

    // --------------------------- TNT Nametag ------------------------

    private void renderTntNametag(Render2DEvent event, int fuseTicks,
                                  double screenX, double screenY, double finalScale) {
        String timeText = ticksToTime(fuseTicks);

        double textWidth = measureWidth(timeText, finalScale);
        double textHeight = measureFontHeight(finalScale);
        double padding = 4.0 * finalScale;

        double bgWidth = textWidth + padding * 2;
        double bgHeight = textHeight + padding * 2;
        double bgX = screenX - bgWidth / 2;
        double bgY = screenY - bgHeight / 2;

        drawBackground(event, bgX, bgY, bgWidth, bgHeight, finalScale);

        drawText(event, timeText, bgX + padding, bgY + padding,
                new Color(0xFFE8B923), false, finalScale);
    }

    private String ticksToTime(int ticks) {
        if (ticks > 20 * 3600) {
            return (ticks / 20 / 3600) + " h";
        } else if (ticks > 20 * 60) {
            return (ticks / 20 / 60) + " m";
        } else {
            int s = ticks / 20;
            int ms = (ticks % 20) / 2;
            return s + "." + ms + " s";
        }
    }

    // --------------------------- Background -------------------------

    private void drawBackground(Render2DEvent event, double x, double y,
                                double width, double height, double finalScale) {
        String mode = bgMode.getMode();
        if (mode.equals("None")) return;

        var r = event.getRenderer();
        int bgArgb = background.getCurrentColor().getARGB();
        int outlineArgb = outline.getCurrentColor().getARGB();
        double radius = 3.0 * finalScale;
        double thickness = Math.max(1.0, finalScale);

        if (mode.equals("Filled")) {
            r.drawRect((float) x, (float) y, (float) width, (float) height, bgArgb);
        } else if (mode.equals("Outline")) {
            r.drawRectOutline((float) x, (float) y, (float) width, (float) height,
                    (float) thickness, outlineArgb);
        } else if (mode.equals("Rounded")) {
            r.drawRoundedRect(x, y, width, height, radius, bgArgb);
        }

        if (!mode.equals("Outline") && outline.getCurrentColor().getAlpha() > 0) {
            r.drawRoundedRectOutline(x, y, width, height, radius,
                    outlineArgb, thickness);
        }
    }

    // --------------------------- Placeholderit ----------------------
    private GameType getGameMode(Player player) { return null; }
    private int getPing(Player player) { return 0; }
    private double distanceToCamera(Entity entity) {
        return mc.gameRenderer.mainCamera().position().distanceTo(entity.position());
    }

    // --------------------------- Color Config -----------------------

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
        factory.openCustomWindow("nametags_color", "NameTags Color Customizer",
                sw, sh, content);
    }
}