package silversword.axiom.client.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.axiomrenderer.RenderAPI;
import silversword.axiom.client.render.rendersystem.axiomrenderer.core.RenderCore;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.render.rendersystem.utils.render.NametagUtils;
import silversword.axiom.client.setting.*;
import silversword.axiom.client.utils.render.TextUtils;

import java.util.*;

public final class NameTags extends AxiomMod implements ColorConfigurable, KeybindConfigurable {
    private final Minecraft mc = Minecraft.getInstance();
    private final RenderCore core = RenderAPI.getInstance().getCore();

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

    // Player‑specific toggles
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

    // Heart icon
    private static final Identifier HEART_TEXTURE = Identifier.fromNamespaceAndPath("projectaxiom", "textures/icons/heart.png");

    // Armor slots in order
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public NameTags() {
        super("NameTags", "Renders custom nametags above entities", ModuleCategory.RENDER);

        scale = new SettingNumber("Scale", 0.1, 3.0, 0.1, 1.5);
        renderDistance = new SettingSlider("Render Distance", new double[]{16, 32, 64, 96, 128, 256, 512}, 96);
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

        bgMode = new SettingMode("Background", new String[]{"None", "Filled", "Outline", "Rounded"}, "Filled");

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

    // --------------------------- Tick & Filter ------------------------------
    @Override
    protected void onTick() {
        if (!isEnabled() || mc.level == null) {
            entityList.clear();
            return;
        }

        entityList.clear();

        double maxDistSq = renderDistance.getValue() * renderDistance.getValue();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();

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

        if (type == EntityType.ITEM) return drawItems.get();
        if (type == EntityType.ITEM_FRAME || type == EntityType.GLOW_ITEM_FRAME) return drawItemFrames.get();
        if (type == EntityType.TNT || type == EntityType.TNT_MINECART) return drawTNT.get();

        return false;
    }

    // --------------------------- 3D -> 2D conversion -----------------------
    @Subscribe
    private void onRender2D(Render2DEvent event) {

        if (event.getGuiGraphics() == null) return;
        if (!isEnabled() || entityList.isEmpty()) return;

        int count = getRenderCount();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();

        for (int i = count - 1; i >= 0; i--) {
            Entity entity = entityList.get(i);

            double x = Mth.lerp(event.tickDelta, entity.xOld, entity.getX());
            double y = Mth.lerp(event.tickDelta, entity.yOld, entity.getY());
            double z = Mth.lerp(event.tickDelta, entity.zOld, entity.getZ());

            Vec3 worldPos = new Vec3(x, y + getHeight(entity) + nameOffset.getValue(), z);

            // Convert to screen coordinates using NametagUtils
            Vec3 screenPos = NametagUtils.worldToScreen(worldPos);
            if (screenPos == null) continue; // behind camera

            double dist = Math.sqrt(entity.distanceToSqr(cameraPos));
            double distanceScale = Mth.clamp(1.0 - dist * 0.005, 0.8, 3.0);
            double finalScale = scale.getValue() * distanceScale;

            // Draw nametag at screen position with scaling
            if (entity instanceof Player) {
                renderPlayerNametag(event, (Player) entity, screenPos.x, screenPos.y, finalScale);
            } else if (entity instanceof ItemEntity) {
                renderItemNametag(event, ((ItemEntity) entity).getItem(), screenPos.x, screenPos.y, finalScale);
            } else if (entity instanceof ItemFrame) {
                renderItemNametag(event, ((ItemFrame) entity).getItem(), screenPos.x, screenPos.y, finalScale);
            } else if (entity instanceof PrimedTnt) {
                renderTntNametag(event, ((PrimedTnt) entity).getFuse(), screenPos.x, screenPos.y, finalScale);
            } else if (entity instanceof MinecartTNT && ((MinecartTNT) entity).isPrimed()) {
                renderTntNametag(event, ((MinecartTNT) entity).getFuse(), screenPos.x, screenPos.y, finalScale);
            } else if (entity instanceof LivingEntity) {
                renderLivingNametag(event, (LivingEntity) entity, screenPos.x, screenPos.y, finalScale);
            } else {
                renderGenericNametag(event, entity, screenPos.x, screenPos.y, finalScale);
            }
        }
    }

    private int getRenderCount() {
        if (!culling.get()) return entityList.size();
        return (int) Math.min(maxCullCount.getValue(), entityList.size());
    }

    private double getHeight(Entity entity) {
        double height = entity.getEyeHeight(entity.getPose());
        EntityType<?> type = entity.getType();
        if (type == EntityType.ITEM || type == EntityType.ITEM_FRAME || type == EntityType.GLOW_ITEM_FRAME) {
            height += 0.2;
        } else {
            height += 0.5;
        }
        return height;
    }

    // --------------------------- Player nametag with health (top) ----------
    private void renderPlayerNametag(Render2DEvent event, Player player, double screenX, double screenY, double finalScale) {
        TextRenderer text = TextRenderer.get();
        int fontHeight = TextUtils.getHeight();

        // ---- Rivi 0: Terveys (sydän + numero) ----
        String healthText = "";
        double healthLineWidth = 0;
        double healthLineHeight = 0;
        double heartSize = 10 * finalScale;
        if (showHealth.get()) {
            float health = player.getHealth();
            healthText = String.valueOf(Math.round(health));

            // Käytetään TextUtilsia numeron leveyteen
            double healthNumWidth = TextUtils.getWidth(healthText) * finalScale;
            double healthNumHeight = fontHeight * finalScale;

            healthLineWidth = heartSize + (2 * finalScale) + healthNumWidth;
            healthLineHeight = Math.max(heartSize, healthNumHeight);
        }

        // ---- Rivi 1: Nimi ja lisätiedot ----
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
        double nameLineWidth = TextUtils.getWidth(nameLine) * finalScale;
        double nameLineHeight = fontHeight * finalScale;

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
            armorLineHeight = iconSize + (2 * finalScale); // Ikoni + pieni tila durability-palkille
        }

        // ---- Lasketaan kokonaismitat ----
        double maxWidth = Math.max(healthLineWidth, Math.max(nameLineWidth, armorLineWidth));
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

        // Piirretään tausta
        drawBackground(bgX, bgY, bgWidth, bgHeight, finalScale);

        // ---- Piirretään sisällöt ----
        double yCursor = bgY + padding;

        // 0. Terveysrivi
        if (showHealth.get()) {
            double healthX = bgX + padding;
            double hTextY = yCursor + (healthLineHeight - nameLineHeight) / 2;

            core.addTexture(HEART_TEXTURE, (float) healthX, (float) (yCursor + (healthLineHeight - heartSize) / 2), (float) heartSize, (float) heartSize, 0xFFFFFFFF);

            text.begin(finalScale, false, true);
            text.render(healthText, (float)(healthX + heartSize + 2 * finalScale), (float)hTextY, getHealthColor(player), false);
            text.end();

            yCursor += healthLineHeight + gap;
        }

        // 1. Nimirivi
        text.begin(finalScale, false, true);
        text.render(nameLine, bgX + padding, yCursor, textColor.getCurrentColor(), false);
        text.end();
        yCursor += nameLineHeight + gap;

        // 2. Armorrivi
        if (armorCount > 0) {
            double startX = bgX + padding;
            for (int i = 0; i < armorStacks.size(); i++) {
                ItemStack stack = armorStacks.get(i);
                double iconX = startX + i * (iconSize + armorGap);

                // Item-ikoni (Vanilla GuiGraphics vaatii int-koordinaatit)
                event.getGuiGraphics().renderItem(stack, (int) iconX, (int) yCursor);

                // Durability-palkki
                if (stack.isDamageableItem()) {
                    float percent = (float) (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage();
                    int barColor = (percent >= 0.7f) ? 0xFF19FC19 : (percent >= 0.5f) ? 0xFFFFFF19 : (percent >= 0.2f) ? 0xFFFF6919 : 0xFFFF1919;

                    double barY = yCursor + iconSize + 1 * finalScale;
                    core.addRect2D((float) iconX, (float) barY, (float) iconSize, (float) (2 * finalScale), 0x64000000); // Tausta
                    core.addRect2D((float) iconX, (float) barY, (float) (iconSize * percent), (float) (2 * finalScale), barColor);
                }
            }
        }
    }

    private Color getHealthColor(Player player) {
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float percent = health / maxHealth;
        if (percent <= 0.333) return new Color(255, 25, 25);
        else if (percent <= 0.666) return new Color(255, 105, 25);
        else return new Color(25, 252, 25);
    }

    private void renderItemNametag(Render2DEvent event, ItemStack stack, double screenX, double screenY, double finalScale) {
        if (stack.isEmpty()) return;

        String name = stack.getHoverName().getString();
        String count = " x" + stack.getCount();

        double nameWidth = TextUtils.getWidth(name) * finalScale;
        double countWidth = TextUtils.getWidth(count) * finalScale;
        double textHeight = TextUtils.getHeight() * finalScale;

        double padding = 4.0 * finalScale;
        double bgWidth = (nameWidth + countWidth) + padding * 2;
        double bgHeight = textHeight + padding * 2;
        double bgX = screenX - bgWidth / 2;
        double bgY = screenY - bgHeight / 2;

        drawBackground(bgX, bgY, bgWidth, bgHeight, finalScale);

        TextRenderer text = TextRenderer.get();
        text.begin(finalScale, false, true);
        text.render(name, bgX + padding, bgY + padding, textColor.getCurrentColor(), false);
        text.render(count, (float)(bgX + padding + nameWidth), (float)(bgY + padding), new Color(0xFFE8B923), false);
        text.end();
    }

    private void renderLivingNametag(Render2DEvent event, LivingEntity entity, double screenX, double screenY, double finalScale) {
        String name = entity.getType().getDescription().getString();
        String healthText = " " + Math.round(entity.getHealth());

        double nameWidth = TextUtils.getWidth(name) * finalScale;
        double healthWidth = TextUtils.getWidth(healthText) * finalScale;
        double textHeight = TextUtils.getHeight() * finalScale;

        double padding = 4.0 * finalScale;
        double bgWidth = (nameWidth + healthWidth) + padding * 2;
        double bgHeight = textHeight + padding * 2;
        double bgX = screenX - bgWidth / 2;
        double bgY = screenY - bgHeight / 2;

        drawBackground(bgX, bgY, bgWidth, bgHeight, finalScale);

        double healthPercentage = entity.getHealth() / entity.getMaxHealth();
        int hColor = (healthPercentage <= 0.333) ? 0xFFFF1919 : (healthPercentage <= 0.666) ? 0xFFFF6919 : 0xFF19FC19;

        TextRenderer text = TextRenderer.get();
        text.begin(finalScale, false, true);
        text.render(name, bgX + padding, bgY + padding, textColor.getCurrentColor(), false);
        text.render(healthText, (float)(bgX + padding + nameWidth), (float)(bgY + padding), new Color(hColor), false);
        text.end();
    }

    private void renderGenericNametag(Render2DEvent event, Entity entity, double screenX, double screenY, double finalScale) {
        String name = entity.getType().getDescription().getString();

        double textWidth = TextUtils.getWidth(name) * finalScale;
        double textHeight = TextUtils.getHeight() * finalScale;
        double padding = 4.0 * finalScale;

        double bgWidth = textWidth + padding * 2;
        double bgHeight = textHeight + padding * 2;
        double bgX = screenX - bgWidth / 2;
        double bgY = screenY - bgHeight / 2;

        drawBackground(bgX, bgY, bgWidth, bgHeight, finalScale);

        TextRenderer text = TextRenderer.get();
        text.begin(finalScale, false, true);
        text.render(name, bgX + padding, bgY + padding, textColor.getCurrentColor(), false);
        text.end();
    }

    private void renderTntNametag(Render2DEvent event, int fuseTicks, double screenX, double screenY, double finalScale) {
        String timeText = ticksToTime(fuseTicks);

        double textWidth = TextUtils.getWidth(timeText) * finalScale;
        double textHeight = TextUtils.getHeight() * finalScale;
        double padding = 4.0 * finalScale;

        double bgWidth = textWidth + padding * 2;
        double bgHeight = textHeight + padding * 2;
        double bgX = screenX - bgWidth / 2;
        double bgY = screenY - bgHeight / 2;

        drawBackground(bgX, bgY, bgWidth, bgHeight, finalScale);

        TextRenderer text = TextRenderer.get();
        text.begin(finalScale, false, true);
        text.render(timeText, (float)(bgX + padding), (float)(bgY + padding), new Color(0xFFE8B923), false);
        text.end();
    }

    private String ticksToTime(int ticks) {
        if (ticks > 20 * 3600) {
            int h = ticks / 20 / 3600;
            return h + " h";
        } else if (ticks > 20 * 60) {
            int m = ticks / 20 / 60;
            return m + " m";
        } else {
            int s = ticks / 20;
            int ms = (ticks % 20) / 2;
            return s + "." + ms + " s";
        }
    }

    // --------------------------- Background --------------------
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
            // Draw outline as lines (4 lines)
            core.addRectOutline2D((float) x, (float) y, (float) width, (float) height, (float) thickness, outlineArgb);
        } else if (mode.equals("Rounded")) {
            core.addRoundedRect((float) x, (float) y, (float) width, (float) height, (float) radius, bgArgb);
        }

        if (!mode.equals("Outline") && outline.getCurrentColor().getAlpha() > 0) {
            core.addRoundedRectOutline((float) x, (float) y, (float) width, (float) height, (float) radius, (float) thickness, outlineArgb);
        }
    }

    // Placeholder methods
    private GameType getGameMode(Player player) { return null; }
    private int getPing(Player player) { return 0; }
    private double distanceToCamera(Entity entity) {
        return mc.gameRenderer.getMainCamera().position().distanceTo(entity.position());
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
        factory.openCustomWindow("nametags_color", "NameTags Color Customizer", sw, sh, content);
    }
}