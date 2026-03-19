package silversword.axiom.client.config;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import silversword.axiom.client.hud.BaseHudElement;
import silversword.axiom.client.hud.HudComponentSettings;
import silversword.axiom.client.hud.HudElement;
import silversword.axiom.client.hud.HudManager;
import silversword.axiom.client.modules.NamedColor;
import silversword.axiom.client.setting.Setting;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class HudConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("axiom_client_hud.json");

    public static void save(HudManager hud) {
        try {
            Path path = FILE;
            if (path.getParent() != null) Files.createDirectories(path.getParent());

            JsonObject root = new JsonObject();
            JsonObject elementsObj = new JsonObject();

            for (HudElement e : hud.elements()) {
                JsonObject o = new JsonObject();
                o.addProperty("x", e.x());
                o.addProperty("y", e.y());
                o.addProperty("enabled", e.enabled());

                HudComponentSettings s = ((BaseHudElement) e).getSettings();
                JsonObject settingsObj = new JsonObject();

                // Tallenna tavalliset asetukset (Setting)
                for (Setting setting : s.getSettings()) {
                    settingsObj.add(setting.getName(), GSON.toJsonTree(setting.getJsonValue()));
                }

                // Tallenna väriasetukset (NamedColor)
                for (NamedColor nc : s.getNamedColors()) {
                    settingsObj.add(nc.getName(), GSON.toJsonTree(nc.getColor().getSetting().getJsonValue()));
                }

                o.add("settings", settingsObj);
                elementsObj.add(e.id(), o);
            }

            root.add("elements", elementsObj);
            Files.writeString(path, GSON.toJson(root));
        } catch (Exception e) {
            System.err.println("[Axiom] HUD-saving error: " + e);
            e.printStackTrace();
        }
    }

    public static void load(HudManager hud) {
        try {
            if (!Files.exists(FILE)) return;
            String json = Files.readString(FILE);
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            if (root == null) return;

            JsonObject elementsObj = root.getAsJsonObject("elements");
            if (elementsObj == null) return;

            for (Map.Entry<String, JsonElement> entry : elementsObj.entrySet()) {
                String id = entry.getKey();
                JsonElement val = entry.getValue();
                if (!val.isJsonObject()) continue;

                JsonObject o = val.getAsJsonObject();
                int x = o.has("x") ? o.get("x").getAsInt() : 0;
                int y = o.has("y") ? o.get("y").getAsInt() : 0;
                boolean enabled = !o.has("enabled") || o.get("enabled").getAsBoolean();

                for (HudElement e : hud.elements()) {
                    if (e.id().equals(id)) {
                        e.setPos(x, y);
                        if (!e.isModuleControlled()) e.setEnabled(enabled);

                        if (e instanceof BaseHudElement) {
                            HudComponentSettings s = ((BaseHudElement) e).getSettings();

                            if (o.has("settings")) {
                                JsonObject settingsObj = o.getAsJsonObject("settings");
                                if (settingsObj != null) {
                                    // Aseta tavalliset asetukset
                                    for (Setting setting : s.getSettings()) {
                                        if (settingsObj.has(setting.getName())) {
                                            JsonElement valueElem = settingsObj.get(setting.getName());
                                            Object obj = jsonElementToObject(valueElem);
                                            setting.setJsonValue(obj);
                                        }
                                    }
                                    // Aseta väriasetukset
                                    for (NamedColor nc : s.getNamedColors()) {
                                        if (settingsObj.has(nc.getName())) {
                                            JsonElement valueElem = settingsObj.get(nc.getName());
                                            Object obj = jsonElementToObject(valueElem);
                                            nc.getColor().getSetting().setJsonValue(obj);
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[Axiom] Failed to load HUD config: " + e);
            e.printStackTrace();
        }
    }

    private static Object jsonElementToObject(JsonElement elem) {
        if (elem.isJsonPrimitive()) {
            JsonPrimitive prim = elem.getAsJsonPrimitive();
            if (prim.isBoolean()) return prim.getAsBoolean();
            if (prim.isNumber()) return prim.getAsNumber();
            if (prim.isString()) return prim.getAsString();
        } else if (elem.isJsonArray()) {
            JsonArray arr = elem.getAsJsonArray();
            Object[] array = new Object[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                array[i] = jsonElementToObject(arr.get(i));
            }
            return array;
        }
        return null;
    }
}