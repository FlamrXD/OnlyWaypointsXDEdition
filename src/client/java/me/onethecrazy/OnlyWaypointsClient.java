package me.onethecrazy;

import me.onethecrazy.commands.Commands;
import me.onethecrazy.screen.WaypointMenuScreen;
import me.onethecrazy.util.FileUtil;
import me.onethecrazy.waypoints.WaypointManager;
import me.onethecrazy.waypoints.objects.Waypoint;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;


import java.io.IOException;


public class OnlyWaypointsClient implements ClientModInitializer {
	@Nullable
	private static OnlyWaypointClientOptions options;

	public static OnlyWaypointClientOptions options(){
		// Get options
		try{
			if(options == null)
				options = FileUtil.getSave();
		} catch (IOException e) {
            OnlyWaypoints.LOGGER.error("Error while getting save: {0}", e);
        }

		return options;
    }

	@Override
	public void onInitializeClient() {
		registerCommands();
		registerKeybinds();
		registerJoinEventHook();
		registerRenderHook();

		try {
			FileUtil.createDefaultPath();
		} catch (IOException e) {
			OnlyWaypoints.LOGGER.error("Ran into error while creating default path: {0}", e);
		}
    }

	private void registerRenderHook(){
		// If we draw in one event (e.g.) AFTER_TRANSLUCENT we risk: - Clouds rendering on top of Beams
		//															 - Labels not rendering from very far away (likely due to clip plane)
		// so we just draw the labels and beams separately
		WorldRenderEvents.AFTER_TRANSLUCENT.register(WaypointManager::renderBeams);
		HudElementRegistry.addLast(Identifier.of("onlywaypoints", "waypoint_label_layer"), WaypointManager::renderLabels);
	}

	private void registerJoinEventHook(){
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> WaypointManager.LoadWaypoints(client.world));
	}

	private void registerKeybinds(){
		var OPEN_MENU_HOTKEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.onlywaypoints.open_menu",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_M,
				Text.translatable("category.onlywaypoints").getString()
		));

		var TOGGLE_VISIBILITY_HOTKEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.onlywaypoints.toggle_visibility",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_T,
				Text.translatable("category.onlywaypoints").getString()
		));

		var ADD_WAYPOINT_HOTKEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.onlywaypoints.add_waypoint",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_N,
				Text.translatable("category.onlywaypoints").getString()
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if(OPEN_MENU_HOTKEY.wasPressed()){
				WaypointMenuScreen.open();
			}

			if(TOGGLE_VISIBILITY_HOTKEY.wasPressed()){
				WaypointManager.toggleGlobalVisibility();
			}

			if(ADD_WAYPOINT_HOTKEY.wasPressed()){
				WaypointManager.addWaypoint(new Waypoint(MinecraftClient.getInstance()));
			}
		});
	}

	private void registerCommands(){
		Commands.initializeCommands();

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(Commands.WAYPOINT_COMMAND);
		});
	}
}