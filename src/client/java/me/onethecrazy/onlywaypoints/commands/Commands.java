package me.onethecrazy.onlywaypoints.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.onethecrazy.onlywaypoints.screen.WaypointMenuScreen;
import me.onethecrazy.onlywaypoints.waypoints.WaypointManager;
import me.onethecrazy.onlywaypoints.waypoints.objects.Waypoint;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.util.Optional;


public class Commands {
    public static LiteralArgumentBuilder<FabricClientCommandSource> WAYPOINT_COMMAND;

    public static void initializeCommands(){
        WAYPOINT_COMMAND = ClientCommandManager.literal("onlywaypoints").executes(context -> waypointsCommandHandler(context, WaypointCommandType.BASE))
                .then(ClientCommandManager.literal("add")
                        .then(ClientCommandManager.argument("name", StringArgumentType.string()).executes(context -> waypointsCommandHandler(context, WaypointCommandType.ADD)))
                            .executes(context -> waypointsCommandHandler(context, WaypointCommandType.ADD)))
                .then(ClientCommandManager.literal("remove")
                        .then(ClientCommandManager.argument("uuid", StringArgumentType.string()).executes(context -> waypointsCommandHandler(context, WaypointCommandType.REMOVE)))
                            .executes(context -> waypointsCommandHandler(context, WaypointCommandType.REMOVE)))
                .then(ClientCommandManager.literal("toggle").executes(context -> waypointsCommandHandler(context, WaypointCommandType.TOGGLE)));
    }

    private static int waypointsCommandHandler(CommandContext<FabricClientCommandSource> ctx, WaypointCommandType cmdType){
        switch(cmdType){
            case WaypointCommandType.BASE -> {
                ctx.getSource().getClient().send(WaypointMenuScreen::open);
                return 1;
            }
            case ADD -> {
                String name = StringArgumentType.getString(ctx, "name");
                WaypointManager.addWaypoint(new Waypoint(ctx.getSource().getClient(), name));

                ctx.getSource().sendFeedback(Text.of("Added Waypoint..."));

                return 1;
            }
            case REMOVE -> {
                String name = StringArgumentType.getString(ctx, "uuid");
                Optional<Waypoint> wp = WaypointManager.getWaypointByUUID(name);

                if(wp.isEmpty()){
                    ctx.getSource().sendError(Text.of("No Waypoint with that UUID exists!"));
                    return 1;
                }

                WaypointManager.removeWaypoint(wp.get());

                ctx.getSource().sendFeedback(Text.of("Removed Waypoint..."));

                return 1;
            }
            case TOGGLE -> {
                WaypointManager.toggleGlobalVisibility();
                return 1;
            }
        }

        return 0;
    }

    private enum WaypointCommandType {
        BASE,
        ADD,
        REMOVE,
        TOGGLE
    }
}
