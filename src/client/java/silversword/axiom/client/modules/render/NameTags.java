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
import silversword.axiom.client.modules.moduleutils.TargetGroup;
import silversword.axiom.client.render.font.TextRenderer;
import silversword.axiom.client.render.rendersystem.Renderer2D;
import silversword.axiom.client.render.rendersystem.utils.color.Color;
import silversword.axiom.client.render.rendersystem.utils.color.SettingColor;
import silversword.axiom.client.render.rendersystem.utils.render.NametagUtils;
import silversword.axiom.client.render.rendersystem.utils.render.RenderUtils;
import silversword.axiom.client.setting.*;
import silversword.axiom.client.utils.render.DrawTexture;
import silversword.axiom.client.utils.render.TextUtils;


import java.util.*;

public final class NameTags extends AxiomMod implements ColorConfigurable, KeybindConfigurable {
    private final Minecraft mc = Minecraft.getInstance();

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
    private final Vector3d pos = new Vector3d();

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

        drawPlayers = new SettingBoolean("Draw Players", true);
        drawHostile = new SettingBoolean("Draw Hostile", true);
        drawPassive = new SettingBoolean("Draw Passive", true);
        drawNeutral = new SettingBoolean("Draw Neutral", true);
        drawWater = new SettingBoolean("Draw Water", true);
        drawBoss = new SettingBoolean("Draw Boss", true);
        drawItems = new SettingBoolean("Draw Items", true);
        drawItemFrames = new SettingBoolean("Draw Item Frames", true);
        drawTNT = new SettingBoolean("Draw TNT", true);

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
    @AxiomEvent
    private void onRender3D(Render3DEvent event) {
        NametagUtils.onRender(RenderUtils.view);
    }

    @AxiomEvent
    private void onRender2D(Render2DEvent event) {
        if (event.drawContext == null) return;
        if (!isEnabled() || entityList.isEmpty()) return;

        boolean shadow = false;
        int count = getRenderCount();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();

        for (int i = count - 1; i >= 0; i--) {
            Entity entity = entityList.get(i);

            double x = Mth.lerp(event.tickDelta, entity.xOld, entity.getX());
            double y = Mth.lerp(event.tickDelta, entity.yOld, entity.getY());
            double z = Mth.lerp(event.tickDelta, entity.zOld, entity.getZ());
            pos.set(x, y + getHeight(entity) + nameOffset.getValue(), z);

            if (!NametagUtils.worldToScreen(pos, scale.getValue())) continue;

            double dist = Math.sqrt(entity.distanceToSqr(cameraPos));
            double distanceScale = Mth.clamp(1.0 - dist * 0.005, 0.8, 3.0);
            double finalScale = scale.getValue() * distanceScale;
            NametagUtils.scale = finalScale;

            NametagUtils.begin(pos, event.drawContext);

            if (entity instanceof Player) {
                renderPlayerNametag(event, (Player) entity, shadow);
            } else if (entity instanceof ItemEntity) {
                renderItemNametag(event, ((ItemEntity) entity).getItem(), shadow);
            } else if (entity instanceof ItemFrame) {
                renderItemNametag(event, ((ItemFrame) entity).getItem(), shadow);
            } else if (entity instanceof PrimedTnt) {
                renderTntNametag(event, ((PrimedTnt) entity).getFuse(), shadow);
            } else if (entity instanceof MinecartTNT && ((MinecartTNT) entity).isPrimed()) {
                renderTntNametag(event, ((MinecartTNT) entity).getFuse(), shadow);
            } else if (entity instanceof LivingEntity) {
                renderLivingNametag(event, (LivingEntity) entity, shadow);
            } else {
                renderGenericNametag(event, entity, shadow);
            }

            NametagUtils.end(event.drawContext);
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
    private void renderPlayerNametag(Render2DEvent event, Player player, boolean shadow) {
        TextRenderer text = TextRenderer.get();

        // ---- Line 0: Health (heart + number) ----
        String healthText = "";
        double healthLineWidth = 0;
        double healthLineHeight = 0;
        double heartSize = 10; // slightly larger
        if (showHealth.get()) {
            float health = player.getHealth();
            healthText = String.valueOf(Math.round(health));
            text.begin(1.0, false, true);
            double healthNumWidth = text.getWidth(healthText, shadow);
            double healthNumHeight = text.getHeight(shadow);
            text.end();
            healthLineWidth = heartSize + 2 + healthNumWidth;
            healthLineHeight = Math.max(heartSize, healthNumHeight);
        }

        // ---- Line 1: Name with extras ----
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
            int ping = getPing(player);
            nameBuilder.append(" [").append(ping).append("ms]");
        }
        if (showDistance.get()) {
            double dist = Math.round(distanceToCamera(player) * 10.0) / 10.0;
            nameBuilder.append(" ").append(dist).append("m");
        }
        String nameLine = nameBuilder.toString();
        double nameLineWidth = nameLine.length() * TextUtils.CHAR_UNIT;
        double nameLineHeight = TextUtils.FONT_HEIGHT;

        // ---- Line 2: Armor icons with durability bars ----
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
        if (armorCount > 0) {
            double iconSize = 16;
            double barHeight = 2;
            double gapBetween = 2;
            armorLineWidth = armorCount * iconSize + (armorCount - 1) * gapBetween;
            armorLineHeight = iconSize + barHeight + 2;
        }

        // ---- Determine maximum width among lines ----
        double maxWidth = Math.max(healthLineWidth, Math.max(nameLineWidth, armorLineWidth));

        // ---- Background dimensions ----
        double padding = 2.0;
        double gapBetweenLines = 2.0;
        double totalHeight = padding;
        if (showHealth.get()) totalHeight += healthLineHeight + gapBetweenLines;
        totalHeight += nameLineHeight + gapBetweenLines;
        if (armorCount > 0) totalHeight += armorLineHeight;
        totalHeight += padding;

        double bgWidth = maxWidth + padding * 2;
        double bgHeight = totalHeight;

        // Draw background
        drawBackground(-bgWidth / 2, -bgHeight / 2, bgWidth, bgHeight);

        // ---- Draw lines ----
        double yCursor = -bgHeight / 2 + padding;

        // Health line (centered)
        if (showHealth.get()) {
            double healthX = -healthLineWidth / 2;
            // Heart icon – centered vertically in the line
            double heartY = yCursor + (healthLineHeight - heartSize) / 2;
            DrawTexture.add(HEART_TEXTURE, healthX, heartY, heartSize, heartSize, new Color(255, 255, 255, 255));
            // Render immediately so the matrix transformation applies
            DrawTexture.renderAll();

            // Health number – also centered vertically
            double numX = healthX + heartSize + 2;
            double numY = yCursor + (healthLineHeight - TextUtils.FONT_HEIGHT) / 2;
            text.begin(1.0, false, true);
            text.render(healthText, numX, numY, getHealthColor(player), shadow);
            text.end();

            yCursor += healthLineHeight + gapBetweenLines;
        }

        // Name line (centered)
        double nameX = -nameLineWidth / 2;
        double nameY = yCursor + (nameLineHeight - TextUtils.FONT_HEIGHT) / 2;
        text.begin(1.0, false, true);
        text.render(nameLine, nameX, nameY, textColor.getCurrentColor(), shadow);
        text.end();
        yCursor += nameLineHeight + gapBetweenLines;

        // Armor line (centered)
        if (armorCount > 0) {
            double iconSize = 16;
            double barHeight = 2;
            double gapBetween = 2;
            double startX = -armorLineWidth / 2;
            double armorY = yCursor;

            for (int i = 0; i < armorStacks.size(); i++) {
                ItemStack stack = armorStacks.get(i);
                double iconX = startX + i * (iconSize + gapBetween);

                // Draw item icon
                event.drawContext.renderItem(stack, (int) iconX, (int) armorY);

                // Draw durability bar below icon
                if (stack.isDamageableItem()) {
                    int maxDurability = stack.getMaxDamage();
                    int currentDurability = maxDurability - stack.getDamageValue();
                    float percent = (float) currentDurability / maxDurability;

                    Color barColor;
                    if (percent >= 0.7f) {
                        barColor = new Color(25, 252, 25); // green
                    } else if (percent >= 0.5f) {
                        barColor = new Color(255, 255, 25); // yellow
                    } else if (percent >= 0.2f) {
                        barColor = new Color(255, 105, 25); // orange
                    } else {
                        barColor = new Color(255, 25, 25); // red
                    }

                    double barX = iconX;
                    double barY = armorY + iconSize + 1;
                    double barWidth = iconSize * percent;

                    Renderer2D.COLOR.begin();
                    // Background (dark)
                    Renderer2D.COLOR.quad(barX, barY, iconSize, barHeight, new Color(0, 0, 0, 100));
                    // Fill
                    Renderer2D.COLOR.quad(barX, barY, barWidth, barHeight, barColor);
                    Renderer2D.COLOR.end();
                    Renderer2D.COLOR.render();
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

    // Other entity types (unchanged)
    private void renderItemNametag(Render2DEvent event, ItemStack stack, boolean shadow) {
        if (stack.isEmpty()) return;

        TextRenderer text = TextRenderer.get();
        String name = stack.getHoverName().getString();
        String count = " x" + stack.getCount();

        double nameWidth = name.length() * TextUtils.CHAR_UNIT;
        double countWidth = count.length() * TextUtils.CHAR_UNIT;
        double textHeight = TextUtils.FONT_HEIGHT;

        double width = nameWidth + countWidth;
        double padding = 2.0;
        double bgWidth = width + padding * 2;
        double bgHeight = textHeight + padding * 2;

        drawBackground(-bgWidth / 2, -bgHeight / 2, bgWidth, bgHeight);

        text.begin(1.0, false, true);
        double hX = -width / 2;
        double hY = -textHeight / 2;
        hX = text.render(name, hX, hY, textColor.getCurrentColor(), shadow);
        text.render(count, hX, hY, new Color(232, 185, 35), shadow);
        text.end();
    }

    private void renderLivingNametag(Render2DEvent event, LivingEntity entity, boolean shadow) {
        TextRenderer text = TextRenderer.get();
        String name = entity.getType().getDescription().getString();
        float health = entity.getHealth();
        String healthText = " " + Math.round(health);

        double nameWidth = name.length() * TextUtils.CHAR_UNIT;
        double healthWidth = healthText.length() * TextUtils.CHAR_UNIT;
        double textHeight = TextUtils.FONT_HEIGHT;

        double width = nameWidth + healthWidth;
        double padding = 2.0;
        double bgWidth = width + padding * 2;
        double bgHeight = textHeight + padding * 2;

        drawBackground(-bgWidth / 2, -bgHeight / 2, bgWidth, bgHeight);

        double healthPercentage = health / entity.getMaxHealth();
        Color healthColor;
        if (healthPercentage <= 0.333) healthColor = new Color(255, 25, 25);
        else if (healthPercentage <= 0.666) healthColor = new Color(255, 105, 25);
        else healthColor = new Color(25, 252, 25);

        text.begin(1.0, false, true);
        double hX = -width / 2;
        double hY = -textHeight / 2;
        hX = text.render(name, hX, hY, textColor.getCurrentColor(), shadow);
        text.render(healthText, hX, hY, healthColor, shadow);
        text.end();
    }

    private void renderGenericNametag(Render2DEvent event, Entity entity, boolean shadow) {
        TextRenderer text = TextRenderer.get();
        String name = entity.getType().getDescription().getString();

        double textWidth = name.length() * TextUtils.CHAR_UNIT;
        double textHeight = TextUtils.FONT_HEIGHT;
        double padding = 2.0;
        double bgWidth = textWidth + padding * 2;
        double bgHeight = textHeight + padding * 2;

        drawBackground(-bgWidth / 2, -bgHeight / 2, bgWidth, bgHeight);

        text.begin(1.0, false, true);
        text.render(name, -textWidth / 2, -textHeight / 2, textColor.getCurrentColor(), shadow);
        text.end();
    }

    private void renderTntNametag(Render2DEvent event, int fuseTicks, boolean shadow) {
        String timeText = ticksToTime(fuseTicks);
        TextRenderer text = TextRenderer.get();

        double textWidth = timeText.length() * TextUtils.CHAR_UNIT;
        double textHeight = TextUtils.FONT_HEIGHT;
        double padding = 2.0;
        double bgWidth = textWidth + padding * 2;
        double bgHeight = textHeight + padding * 2;

        drawBackground(-bgWidth / 2, -bgHeight / 2, bgWidth, bgHeight);

        text.begin(1.0, false, true);
        text.render(timeText, -textWidth / 2, -textHeight / 2, new Color(232, 185, 35), shadow);
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

    // --------------------------- Background & Utilities --------------------
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

    // Placeholder methods – replace with your actual implementations
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