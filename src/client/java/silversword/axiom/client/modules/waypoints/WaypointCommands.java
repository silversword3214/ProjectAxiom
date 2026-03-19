package silversword.axiom.client.modules.waypoints;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import silversword.axiom.client.managers.WaypointManager;

public class WaypointCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("waypoint")
                    .then(CommandManager.argument("name", StringArgumentType.word())
                            .executes(context -> {
                                String name = StringArgumentType.getString(context, "name");
                                MinecraftClient client = MinecraftClient.getInstance();
                                if (client.player != null) {
                                    Waypoint wp = new Waypoint(name,
                                            client.player.getX(), client.player.getY(), client.player.getZ(),
                                            0xFF00FF00);
                                    WaypointManager.getInstance().add(wp);
                                    client.player.sendMessage(Text.literal("Waypoint '" + name + "' set."), false);
                                }
                                return 1;
                            })));
        });
    }
}