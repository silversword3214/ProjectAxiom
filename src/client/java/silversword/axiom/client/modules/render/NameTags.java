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
import org.joml.Matrix4f;
import org.joml.Vector4f;
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
import silversword.axiom.client.setting.*;
import silversword.axiom.client.utils.render.DrawTexture;
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
    @Subscribe
    private void onRender2D(Render2DEvent event) {
        if (event.getGuiGraphics() == null) return;
        if (!isEnabled() || entityList.isEmpty()) return;

        // Get current projection and view matrices from the camera
        Matrix4f proj = mc.gameRenderer.getProjectionMatrix(mc.gameRenderer.getFov(mc.gameRenderer.getMainCamera(), event.tickDelta, true));
        Matrix4f view = new Matrix4f().rotate(mc.gameRenderer.getMainCamera().rotation().conjugate())
                .translate(-(float) mc.gameRenderer.getMainCamera().position().x,
                        -(float) mc.gameRenderer.getMainCamera().position().y,
                        -(float) mc.gameRenderer.getMainCamera().position().z);

        int count = getRenderCount();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();

        for (int i = count - 1; i >= 0; i--) {
            Entity entity = entityList.get(i);

            double x = Mth.lerp(event.tickDelta, entity.xOld, entity.getX());
            double y = Mth.lerp(event.tickDelta, entity.yOld, entity.getY());
            double z = Mth.lerp(event.tickDelta, entity.zOld, entity.getZ());

            Vec3 worldPos = new Vec3(x, y + getHeight(entity) + nameOffset.getValue(), z);

            // Convert to screen coordinates
            Vec3 screenPos = worldToScreen(proj, view, worldPos);
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

    private Vec3 worldToScreen(Matrix4f proj, Matrix4f view, Vec3 worldPos) {
        Vector4f clip = new Vector4f((float) worldPos.x, (float) worldPos.y, (float) worldPos.z, 1.0f);
        clip.mul(view).mul(proj);
        if (clip.w <= 0.0f) return null; // behind camera

        float invW = 1.0f / clip.w;
        float ndcX = clip.x * invW;
        float ndcY = clip.y * invW;

        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();
        float screenX = (ndcX * 0.5f + 0.5f) * width;
        float screenY = (1.0f - (ndcY * 0.5f + 0.5f)) * height;

        return new Vec3(screenX, screenY, clip.z * invW);
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

        // ---- Line 0: Health (heart + number) ----
        String healthText = "";
        double healthLineWidth = 0;
        double healthLineHeight = 0;
        double heartSize = 10 * finalScale;
        if (showHealth.get()) {
            float health = player.getHealth();
            healthText = String.valueOf(Math.round(health));
            text.begin(1.0, false, true);
            double healthNumWidth = text.getWidth(healthText, false);
            double healthNumHeight = text.getHeight(false);
            text.end();
            healthLineWidth = heartSize + 2 * finalScale + healthNumWidth;
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
        double nameLineWidth = nameLine.length() * TextUtils.CHAR_UNIT * finalScale;
        double nameLineHeight = TextUtils.FONT_HEIGHT * finalScale;

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
            double iconSize = 16 * finalScale;
            double barHeight = 2 * finalScale;
            double gapBetween = 2 * finalScale;
            armorLineWidth = armorCount * iconSize + (armorCount - 1) * gapBetween;
            armorLineHeight = iconSize + barHeight + 2 * finalScale;
        }

        // ---- Determine maximum width among lines ----
        double maxWidth = Math.max(healthLineWidth, Math.max(nameLineWidth, armorLineWidth));

        // ---- Background dimensions ----
        double padding = 2.0 * finalScale;
        double gapBetweenLines = 2.0 * finalScale;
        double totalHeight = padding;
        if (showHealth.get()) totalHeight += healthLineHeight + gapBetweenLines;
        totalHeight += nameLineHeight + gapBetweenLines;
        if (armorCount > 0) totalHeight += armorLineHeight;
        totalHeight += padding;

        double bgWidth = maxWidth + padding * 2;
        double bgHeight = totalHeight;
        double bgX = screenX - bgWidth / 2;
        double bgY = screenY - bgHeight / 2;

        // Draw background
        drawBackground(bgX, bgY, bgWidth, bgHeight, finalScale);

        // ---- Draw lines ----
        double yCursor = bgY + padding;

        // Health line
        if (showHealth.get()) {
            double healthX = bgX + padding;
            double healthY = yCursor + (healthLineHeight - heartSize) / 2;
            // Heart icon
            core.addTexture(HEART_TEXTURE, (float) healthX, (float) healthY, (float) heartSize, (float) heartSize, 0xFFFFFFFF);
            // Health number
            double numX = healthX + heartSize + 2 * finalScale;
            double numY = yCursor + (healthLineHeight - TextUtils.FONT_HEIGHT * finalScale) / 2;
            text.begin(1.0, false, true);
            text.render(healthText, numX, numY, getHealthColor(player), false);
            text.end();
            yCursor += healthLineHeight + gapBetweenLines;
        }

        // Name line
        double nameX = bgX + padding;
        double nameY = yCursor + (nameLineHeight - TextUtils.FONT_HEIGHT * finalScale) / 2;
        text.begin(1.0, false, true);
        text.render(nameLine, nameX, nameY, textColor.getCurrentColor(), false);
        text.end();
        yCursor += nameLineHeight + gapBetweenLines;

        // Armor line
        if (armorCount > 0) {
            double iconSize = 16 * finalScale;
            double barHeight = 2 * finalScale;
            double gapBetween = 2 * finalScale;
            double startX = bgX + padding;
            double armorY = yCursor;

            for (int i = 0; i < armorStacks.size(); i++) {
                ItemStack stack = armorStacks.get(i);
                double iconX = startX + i * (iconSize + gapBetween);

                // Draw item icon using vanilla GuiGraphics
                event.getGuiGraphics().renderItem(stack, (int) iconX, (int) armorY);

                // Draw durability bar below icon
                if (stack.isDamageableItem()) {
                    int maxDurability = stack.getMaxDamage();
                    int currentDurability = maxDurability - stack.getDamageValue();
                    float percent = (float) currentDurability / maxDurability;

                    int barColor;
                    if (percent >= 0.7f) {
                        barColor = 0xFF19FC19; // green
                    } else if (percent >= 0.5f) {
                        barColor = 0xFFFFFF19; // yellow
                    } else if (percent >= 0.2f) {
                        barColor = 0xFFFF6919; // orange
                    } else {
                        barColor = 0xFFFF1919; // red
                    }

                    double barX = iconX;
                    double barY = armorY + iconSize + 1 * finalScale;
                    double barWidth = iconSize * percent;

                    core.addRect2D((float) barX, (float) barY, (float) barWidth, (float) barHeight, 0x64000000); // background
                    core.addRect2D((float) barX, (float) barY, (float) barWidth, (float) barHeight, barColor);
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

    // Other entity types
    private void renderItemNametag(Render2DEvent event, ItemStack stack, double screenX, double screenY, double finalScale) {
        if (stack.isEmpty()) return;

        TextRenderer text = TextRenderer.get();
        String name = stack.getHoverName().getString();
        String count = " x" + stack.getCount();

        double nameWidth = name.length() * TextUtils.CHAR_UNIT * finalScale;
        double countWidth = count.length() * TextUtils.CHAR_UNIT * finalScale;
        double textHeight = TextUtils.FONT_HEIGHT * finalScale;

        double width = nameWidth + countWidth;
        double padding = 2.0 * finalScale;
        double bgWidth = width + padding * 2;
        double bgHeight = textHeight + padding * 2;
        double bgX = screenX - bgWidth / 2;
        double bgY = screenY - bgHeight / 2;

        drawBackground(bgX, bgY, bgWidth, bgHeight, finalScale);

        double textX = bgX + padding;
        double textY = bgY + padding;
        text.begin(1.0, false, true);
        text.render(name, textX, textY, textColor.getCurrentColor(), false);
        text.render(count, textX + nameWidth, textY, new Color(232, 185, 35), false);
        text.end();
    }

    private void renderLivingNametag(Render2DEvent event, LivingEntity entity, double screenX, double screenY, double finalScale) {
        TextRenderer text = TextRenderer.get();
        String name = entity.getType().getDescription().getString();
        float health = entity.getHealth();
        String healthText = " " + Math.round(health);

        double nameWidth = name.length() * TextUtils.CHAR_UNIT * finalScale;
        double healthWidth = healthText.length() * TextUtils.CHAR_UNIT * finalScale;
        double textHeight = TextUtils.FONT_HEIGHT * finalScale;

        double width = nameWidth + healthWidth;
        double padding = 2.0 * finalScale;
        double bgWidth = width + padding * 2;
        double bgHeight = textHeight + padding * 2;
        double bgX = screenX - bgWidth / 2;
        double bgY = screenY - bgHeight / 2;

        drawBackground(bgX, bgY, bgWidth, bgHeight, finalScale);

        double healthPercentage = health / entity.getMaxHealth();
        Color healthColor;
        if (healthPercentage <= 0.333) healthColor = new Color(255, 25, 25);
        else if (healthPercentage <= 0.666) healthColor = new Color(255, 105, 25);
        else healthColor = new Color(25, 252, 25);

        double textX = bgX + padding;
        double textY = bgY + padding;
        text.begin(1.0, false, true);
        text.render(name, textX, textY, textColor.getCurrentColor(), false);
        text.render(healthText, textX + nameWidth, textY, healthColor, false);
        text.end();
    }

    private void renderGenericNametag(Render2DEvent event, Entity entity, double screenX, double screenY, double finalScale) {
        TextRenderer text = TextRenderer.get();
        String name = entity.getType().getDescription().getString();

        double textWidth = name.length() * TextUtils.CHAR_UNIT * finalScale;
        double textHeight = TextUtils.FONT_HEIGHT * finalScale;
        double padding = 2.0 * finalScale;
        double bgWidth = textWidth + padding * 2;
        double bgHeight = textHeight + padding * 2;
        double bgX = screenX - bgWidth / 2;
        double bgY = screenY - bgHeight / 2;

        drawBackground(bgX, bgY, bgWidth, bgHeight, finalScale);

        double textX = bgX + padding;
        double textY = bgY + padding;
        text.begin(1.0, false, true);
        text.render(name, textX, textY, textColor.getCurrentColor(), false);
        text.end();
    }

    private void renderTntNametag(Render2DEvent event, int fuseTicks, double screenX, double screenY, double finalScale) {
        String timeText = ticksToTime(fuseTicks);
        TextRenderer text = TextRenderer.get();

        double textWidth = timeText.length() * TextUtils.CHAR_UNIT * finalScale;
        double textHeight = TextUtils.FONT_HEIGHT * finalScale;
        double padding = 2.0 * finalScale;
        double bgWidth = textWidth + padding * 2;
        double bgHeight = textHeight + padding * 2;
        double bgX = screenX - bgWidth / 2;
        double bgY = screenY - bgHeight / 2;

        drawBackground(bgX, bgY, bgWidth, bgHeight, finalScale);

        double textX = bgX + padding;
        double textY = bgY + padding;
        text.begin(1.0, false, true);
        text.render(timeText, textX, textY, new Color(232, 185, 35), false);
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