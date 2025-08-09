package me.onethecrazy.onlywaypoints.waypoints;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import me.onethecrazy.OnlyWaypoints;
import me.onethecrazy.onlywaypoints.OnlyWaypointsClient;
import me.onethecrazy.mixin.ServerAccessor;
import me.onethecrazy.onlywaypoints.util.BeamRenderer;
import me.onethecrazy.onlywaypoints.util.FileUtil;
import me.onethecrazy.onlywaypoints.waypoints.objects.Coordinates;
import me.onethecrazy.onlywaypoints.waypoints.objects.Waypoint;
import me.onethecrazy.onlywaypoints.waypoints.objects.WaypointType;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class WaypointManager {
    public static List<Waypoint> waypoints;
    public static Path currentSavePath;
    // To prevent Beacon Beam not spinning when /tick freeze is used
    private static double unfrozenTime = 0;
    // Toggle rendering of all waypoints
    public static boolean shouldRenderGlobally;

    public static void LoadWaypoints(ClientWorld world){
        shouldRenderGlobally = true;

        var client = MinecraftClient.getInstance();

        String tag = "";

        // Integrated Server
        if(client.getServer() != null){
            tag = ((ServerAccessor)client.getServer()).getSession().getDirectoryName();
        }
        // 3rd party server
        else if (client.getNetworkHandler() != null){
            var adr = client.getNetworkHandler().getConnection().getAddress();
            if(adr instanceof InetSocketAddress)
                tag = ((InetSocketAddress) adr).getHostString();
            else
                tag = adr.toString();
        }

        OnlyWaypoints.LOGGER.info("Loading waypoints for: " + tag);

        currentSavePath = FileUtil.getDefaultPath().resolve(tag).resolve("waypoints.json");

        try{
            String json = FileUtil.readJSONArrayFileContents(currentSavePath);

            Gson gson = new Gson();

            Type listType = new TypeToken<List<Waypoint>>(){}.getType();
            List<Waypoint> intermediate = gson.fromJson(json, listType);

            waypoints = intermediate;
        }
        catch(Exception e){
            OnlyWaypoints.LOGGER.error("Ran into error while loading waypoints: {0}", e);
            waypoints = List.of();
        }
    }

    public static void addWaypoint(Waypoint wp){
        waypoints.add(wp);

        saveWaypoints();
    }

    public static void removeWaypoint(Waypoint wp){
        waypoints.remove(wp);

        saveWaypoints();
    }

    public static void saveWaypoints(){
        Gson gson = new Gson();
        String json = gson.toJson(waypoints);

        try{
            FileUtil.writeFile(currentSavePath, json);
        }
        catch(Exception e){
            OnlyWaypoints.LOGGER.error("Ran into error while saving waypoints: " + e);
        }
    }

    public static void renderBeams(WorldRenderContext ctx){
        MinecraftClient client = MinecraftClient.getInstance();
        // Sanity Check, should never be null
        if (client.world == null) return;

        var playerDimension = client.world.getRegistryKey();

        float delta = ctx.tickCounter().getDynamicDeltaTicks();
        unfrozenTime += delta;

        VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();

        for (Waypoint wp : waypoints) {
            if(!wp.shouldRender || !Objects.equals(playerDimension.getValue().getPath(), wp.dimension.getValue().getPath()) || !Objects.equals(playerDimension.getValue().getNamespace(), wp.dimension.getValue().getNamespace()) || !shouldRenderGlobally)
                continue;

            renderBeam(wp, ctx, delta, consumers);
        }
    }

    public static void renderLabels(DrawContext context, RenderTickCounter tick){
        MinecraftClient client = MinecraftClient.getInstance();
        // Sanity Check, should never be null
        if (client.world == null) return;

        var playerDimension = client.world.getRegistryKey();


        for (Waypoint wp : waypoints) {
            if(!wp.shouldRender || !Objects.equals(playerDimension.getValue().getPath(), wp.dimension.getValue().getPath()) || !Objects.equals(playerDimension.getValue().getNamespace(), wp.dimension.getValue().getNamespace()) || !shouldRenderGlobally)
                continue;

            renderLabel(wp, context, tick);
        }
    }

    private static void renderLabel(Waypoint wp, DrawContext ctx, RenderTickCounter tick){
        MinecraftClient client = MinecraftClient.getInstance();
        Camera camera = client.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();
        Window window = client.getWindow();

        // Construct projection Matrix
        float fovRad = (float)Math.toRadians(client.options.getFov().getValue());
        float aspect = (float)window.getScaledWidth() / window.getScaledHeight();
        float near = 0.05F;
        float far = client.options.getViewDistance().getValue() * 16.0F;
        Matrix4f projMatrix = new Matrix4f()
                .identity()
                .perspective(fovRad, aspect, near, far);

        // Construct view matrix
        float pitch = camera.getPitch();
        float yaw   = camera.getYaw();
        Matrix4f viewMatrix = new Matrix4f()
                .identity()
                .rotateX((float)Math.toRadians(pitch))
                .rotateY((float)Math.toRadians(yaw))
                .translate(
                        (float)-camPos.x,
                        (float)-camPos.y,
                        (float)-camPos.z
                )
                .translate(0f, 0f, 0);


        Vec3d worldPos = new Vec3d(
                wp.coordinates.x + 0.5,
                wp.coordinates.y + 1,
                wp.coordinates.z + 0.5);

        Vector4f clip = new Vector4f(
                (float)worldPos.x,
                (float)worldPos.y - 2f * ((float)worldPos.y - (float)camPos.y),
                (float)worldPos.z,
                1f
        );

        viewMatrix.transform(clip);
        projMatrix.transform(clip);

        // Not in camera space
        if(clip.w() >= 0f) return;



        // Evil projection fuckery
        float ndcX = clip.x() / clip.w();
        float ndcY = clip.y() / clip.w();
        int sw = window.getScaledWidth();
        int sh = window.getScaledHeight();
        int sx = (int)((ndcX * 0.5f + 0.5f) * sw);
        int sy = (int)((1f - (ndcY * 0.5f + 0.5f)) * sh);

        // Draw
        TextRenderer tr = client.textRenderer;
        Text txt = Text.of(wp.name + " [" + (int)worldPos.distanceTo(camPos) + "m]");
        int color = 0xFFFFFFFF;

        float textWidth = tr.getWidth(txt);
        float textHeight = tr.fontHeight;
        float xOffset = textWidth / 2f;
        int margin = 2;

        if(worldPos.distanceTo(camPos) > OnlyWaypointsClient.options().dontRenderAfterDistance)
            return;

        ctx.fill(sx - margin - (int)xOffset, sy - margin, sx + (int)xOffset + margin, sy + (int)textHeight + margin, ColorHelper.withAlpha(0.5f, 0x000000));

        // Draw Text
        ctx.drawText(
                tr,
                txt,
                sx - (int)xOffset,
                sy,
                color,
                true
        );
    }

    private static void renderBeam(Waypoint wp, WorldRenderContext ctx, float delta, VertexConsumerProvider consumers){
        Camera camera = ctx.camera();
        Vec3d cam = camera.getPos();
        MatrixStack ms = ctx.matrixStack();

        // Get Translations
        double bx = wp.coordinates.x - cam.x;
        double by = wp.coordinates.y - cam.y;
        double bz = wp.coordinates.z - cam.z;

        ms.push();


        // Get Alpha
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        double distance = Math.sqrt(Math.pow(player.getX() - wp.coordinates.x, 2) + Math.pow(player.getZ() - wp.coordinates.z, 2));
        float alpha = Math.max((float)(1 - Math.pow(0.5, Math.max(0, distance - 5))), 0.1f);

        if(distance > OnlyWaypointsClient.options().dontRenderAfterDistance)
            return;

        ms.translate(bx, by, bz);

        BeamRenderer.renderBeamWithOpacity(
                ms,
                consumers,
                BeaconBlockEntityRenderer.BEAM_TEXTURE,
                delta,
                1.0F,
                (long)unfrozenTime,
                0,
                BeaconBlockEntityRenderer.MAX_BEAM_HEIGHT,
                0x4B0082,
                alpha,
                0.15F,
                0.175F
        );

        ms.pop();
    }

    public static void toggleGlobalVisibility(){
        shouldRenderGlobally = !shouldRenderGlobally;
    }

    public static void updateLastDeathWaypoint(Coordinates coords, RegistryKey<World> dimension){
        Stream<Waypoint> deathWaypoints = waypoints.stream().filter(w -> w.type == WaypointType.DEATH);

        // Pattern for matching the death number in wp name
        Pattern p = Pattern.compile("^.*\\((\\d+)\\)$");

        String lastDeathString = Text.translatable("gui.onlywaypoints.last_death").getString();

        deathWaypoints.forEach(wp -> {
            Matcher m = p.matcher(wp.name);

            if(m.matches()){
                String wpName = wp.name;
                String deathIndex = m.group(1);
                int deathIndexInt = Integer.parseInt(deathIndex);

                wp.name = wpName.replace(deathIndex, String.valueOf(deathIndexInt + 1));
            }
            // No number found -> Last Death
            else{
                wp.name = lastDeathString + " (1)";
            }
        });

        // Add Last Death (we had none before)
        addWaypoint(new Waypoint(dimension, coords, lastDeathString, WaypointType.DEATH));
    }

    public static Optional<Waypoint> getWaypointByUUID(String uuid){
        return waypoints.stream().filter(w -> Objects.equals(w.uuid, uuid)).findFirst();
    }

    public static String getUniqueName(String prefix){
        int highestNameIndex = -1;
        Pattern p = Pattern.compile("^.*\\((\\d+)\\)$");

        for(Waypoint wp : waypoints) {
            if(wp.name.contains(prefix)){
                if(highestNameIndex == -1)
                    highestNameIndex++;

                Matcher m = p.matcher(wp.name);

                if(!m.matches())
                    continue;

                String index = m.group(1);
                int indexInt = Integer.parseInt(index);

                if(indexInt > highestNameIndex) highestNameIndex = indexInt;
            }
        }

        String numberString = highestNameIndex == -1 ? "" : (" (" + (highestNameIndex + 1) + ")");

        return prefix + numberString;
    }

    public static String getNewPlaceholderName(){
        // Construct new name
        return getUniqueName(Text.translatable("gui.onlywaypoints.new_waypoint_placeholder").getString());
    }
}
