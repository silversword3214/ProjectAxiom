package silversword.axiom.client.managers;

import com.google.gson.*;
import silversword.axiom.client.modules.waypoints.Waypoint;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class WaypointManager {
    private static WaypointManager instance;
    private final List<Waypoint> waypoints = new CopyOnWriteArrayList<>();
    private final Path configPath = Paths.get("config/projectaxiom/waypoints.json");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private WaypointManager() {
        load();
    }

    public static WaypointManager getInstance() {
        if (instance == null) instance = new WaypointManager();
        return instance;
    }

    public List<Waypoint> getAll() {
        return Collections.unmodifiableList(waypoints);
    }

    public void add(Waypoint wp) {
        waypoints.add(wp);
        save();
    }

    public void remove(UUID id) {
        waypoints.removeIf(w -> w.id.equals(id));
        save();
    }


    public void update(Waypoint updated) {
        for (int i = 0; i < waypoints.size(); i++) {
            if (waypoints.get(i).id.equals(updated.id)) {
                waypoints.set(i, updated);
                save();
                break;
            }
        }
    }

    public void save() {
        try {
            Files.createDirectories(configPath.getParent());
            JsonArray arr = new JsonArray();
            for (Waypoint wp : waypoints) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", wp.id.toString());
                obj.addProperty("name", wp.name);
                obj.addProperty("x", wp.x);
                obj.addProperty("y", wp.y);
                obj.addProperty("z", wp.z);
                obj.addProperty("color", wp.color);
                obj.addProperty("enabled", wp.enabled);
                obj.addProperty("bgColor", wp.bgColor);
                obj.addProperty("outlineColor", wp.outlineColor);
                obj.addProperty("showBg", wp.showBg);
                obj.addProperty("showOutline", wp.showOutline);
                obj.addProperty("shape", wp.shape);
                obj.addProperty("scale", wp.scale);
                arr.add(obj);
            }
            String json = gson.toJson(arr);
            Files.writeString(configPath, json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load() {
        if (!Files.exists(configPath)) return;
        try {
            String json = Files.readString(configPath);
            JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
            waypoints.clear();
            for (JsonElement e : arr) {
                JsonObject obj = e.getAsJsonObject();
                UUID id = UUID.fromString(obj.get("id").getAsString());
                String name = obj.get("name").getAsString();
                double x = obj.get("x").getAsDouble();
                double y = obj.get("y").getAsDouble();
                double z = obj.get("z").getAsDouble();
                int color = obj.get("color").getAsInt();
                boolean enabled = obj.get("enabled") != null ? obj.get("enabled").getAsBoolean() : true;

                // New fields with defaults
                int bgColor = obj.has("bgColor") ? obj.get("bgColor").getAsInt() : 0x64000000;
                int outlineColor = obj.has("outlineColor") ? obj.get("outlineColor").getAsInt() : 0xFFFFFFFF;
                boolean showBg = obj.has("showBg") ? obj.get("showBg").getAsBoolean() : true;
                boolean showOutline = obj.has("showOutline") ? obj.get("showOutline").getAsBoolean() : true;
                String shape = obj.has("shape") ? obj.get("shape").getAsString() : "Rounded";
                double scale = obj.has("scale") ? obj.get("scale").getAsDouble() : 1.0;

                waypoints.add(new Waypoint(id, name, x, y, z, color, enabled,
                        bgColor, outlineColor, showBg, showOutline, shape, scale));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}