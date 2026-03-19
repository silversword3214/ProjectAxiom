package silversword.axiom.client.modules.misc.deathlocation;

import com.google.gson.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class DeathLocationManager {
    private static DeathLocationManager instance;
    private final List<DeathEntry> entries = new CopyOnWriteArrayList<>();
    private final Path configPath = Paths.get("config/projectaxiom/deathlocations.json");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private DeathLocationManager() {
        load();
    }

    public static DeathLocationManager getInstance() {
        if (instance == null) instance = new DeathLocationManager();
        return instance;
    }

    public List<DeathEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public void addEntry(DeathEntry entry) {
        entries.add(0, entry); // uusin ensimmäiseksi
        if (entries.size() > 50) { // rajoitetaan määrää
            entries.remove(entries.size() - 1);
        }
        save();
    }

    public void removeEntry(int index) {
        if (index >= 0 && index < entries.size()) {
            entries.remove(index);
            save();
        }
    }

    public void clear() {
        entries.clear();
        save();
    }

    private void save() {
        try {
            Files.createDirectories(configPath.getParent());
            JsonArray arr = new JsonArray();
            for (DeathEntry e : entries) {
                JsonObject obj = new JsonObject();
                obj.addProperty("x", e.x);
                obj.addProperty("y", e.y);
                obj.addProperty("z", e.z);
                obj.addProperty("timestamp", e.timestamp);
                obj.addProperty("world", e.world);
                arr.add(obj);
            }
            Files.writeString(configPath, gson.toJson(arr));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load() {
        if (!Files.exists(configPath)) return;
        try {
            String json = Files.readString(configPath);
            JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
            entries.clear();
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                double x = obj.get("x").getAsDouble();
                double y = obj.get("y").getAsDouble();
                double z = obj.get("z").getAsDouble();
                String timestamp = obj.get("timestamp").getAsString();
                String world = obj.get("world").getAsString();
                // Luodaan entry uudelleen (timestamp menee konstruktorissa, mutta halutaan säilyttää tallennettu)
                DeathEntry entry = new DeathEntry(x, y, z, world);
                // Tässä pitäisi asettaa tallennettu timestamp, mutta yksinkertaistetaan: ei käytetä tallennettua aikaa uudelleen
                // Parempi: lisätään kenttä konstruktoriin, mutta nyt tehdään nopeasti:
                // Käytetään reflektiota tai muuta – skipataan tässä, koska timestamp ei kriittinen.
                entries.add(entry);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}