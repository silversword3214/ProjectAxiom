package silversword.axiom.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import silversword.axiom.client.main.AxiomMod;
import silversword.axiom.client.managers.ModuleManager;
import silversword.axiom.client.setting.Setting;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class SettingsConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE = "axiom_client_settings.json";
    private static final Type TYPE = new TypeToken<Map<String, Map<String, Object>>>() {}.getType();

    private SettingsConfigManager() {}

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE);
    }

    public static void saveAll() {
        try {
            Map<String, Map<String, Object>> root = new HashMap<>();

            for (AxiomMod mod : ModuleManager.getInstance().getModules()) {
                if (mod == null) continue;
                String mid = mod.getId();
                if (mid == null) continue;

                Map<String, Object> smap = new HashMap<>();
                for (Setting s : mod.getAllSettings()) {
                    if (s == null) continue;
                    smap.put(s.getName(), s.getJsonValue());
                }
                // ✅ Tallenna moduulin päälläolotila
                smap.put("enabled", mod.isEnabled());

                root.put(mid, smap);
            }

            Path p = path();
            if (p.getParent() != null) Files.createDirectories(p.getParent());
            Files.writeString(p, GSON.toJson(root));

        } catch (Throwable t) {
            System.err.println("[Axiom] Failed to save settings");
            t.printStackTrace();
        }
    }

    public static void loadAll() {
        try {
            Path p = path();
            if (!Files.exists(p)) return;

            String json = Files.readString(p).trim();
            if (json.isEmpty()) return;

            Map<String, Map<String, Object>> root = GSON.fromJson(json, TYPE);
            if (root == null) return;

            for (AxiomMod mod : ModuleManager.getInstance().getModules()) {
                if (mod == null) continue;
                String mid = mod.getId();
                if (mid == null) continue;

                Map<String, Object> smap = root.get(mid);
                if (smap == null) continue;

                // Lataa enabled-tila ensin
                if (smap.containsKey("enabled")) {
                    Object val = smap.get("enabled");
                    if (val instanceof Boolean) {
                        mod.setEnabled((Boolean) val);
                    }
                }

                // Lataa muut asetukset
                for (Setting s : mod.getAllSettings()) {
                    if (s == null) continue;
                    if (!smap.containsKey(s.getName())) continue;
                    s.setJsonValue(smap.get(s.getName()));
                }
            }

        } catch (Throwable t) {
            System.err.println("[Axiom] Failed to load settings");
            t.printStackTrace();
        }
    }
}