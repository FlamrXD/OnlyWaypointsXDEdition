package me.onethecrazy.waypoints.objects;

import me.onethecrazy.waypoints.WaypointManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Waypoint {
    public RegistryKey<World> dimension;
    public Coordinates coordinates;
    public String name;
    public WaypointType type;
    public boolean shouldRender;
    public String uuid;

    public Waypoint(RegistryKey<World> dimension, Coordinates coordinates, String name){
        this.dimension = dimension;
        this.coordinates = coordinates;
        this.name = name;
        this.type = WaypointType.USER;
        this.shouldRender = true;
    }

    public Waypoint(RegistryKey<World> dimension, Coordinates coordinates, String name, WaypointType type){
        this.dimension = dimension;
        this.coordinates = coordinates;
        this.name = name;
        this.type = type;
        this.shouldRender = true;
    }

    public Waypoint(MinecraftClient client){
        var coords = new Coordinates(client.player.getBlockX(), client.player.getBlockY(), client.player.getBlockZ());
        var dim = client.world.getRegistryKey();

        this.dimension = dim;
        this.coordinates = coords;
        this.name = WaypointManager.getNewPlaceholderName();

        this.type = WaypointType.USER;
        this.shouldRender = true;
        this.uuid = genUUID();
    }

    public Waypoint(MinecraftClient client, String name){
        var coords = new Coordinates(client.player.getBlockX(), client.player.getBlockY(), client.player.getBlockZ());
        var dim = client.world.getRegistryKey();

        this.dimension = dim;
        this.coordinates = coords;
        this.name = name;
        this.type = WaypointType.USER;
        this.shouldRender = true;
        this.uuid = genUUID();
    }

    private static String genUUID(){
        String shortHex = UUID
                .randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8);

        return shortHex;
    }
}
