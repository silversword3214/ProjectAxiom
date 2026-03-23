package silversword.axiom.client.modules.waypoints;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import silversword.axiom.client.managers.WaypointManager;

public class WaypointCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("waypoint")
                    .then(Commands.argument("name", StringArgumentType.word())
                            .executes(context -> {
                                String name = StringArgumentType.getString(context, "name");
                                Minecraft client = Minecraft.getInstance();
                                if (client.player != null) {
                                    Waypoint wp = new Waypoint(name,
                                            client.player.getX(), client.player.getY(), client.player.getZ(),
                                            0xFF00FF00);
                                    WaypointManager.getInstance().add(wp);
                                    client.player.displayClientMessage(Component.literal("Waypoint '" + name + "' set."), false);
                                }
                                return 1;
                            })));
        });
    }
}