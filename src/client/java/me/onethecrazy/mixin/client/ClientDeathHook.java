package me.onethecrazy.mixin.client;

import me.onethecrazy.waypoints.WaypointManager;
import me.onethecrazy.waypoints.objects.Coordinates;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientDeathHook extends LivingEntity{


    protected ClientDeathHook(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method="playSound", at=@At("HEAD"))
    private void onPlaySound(SoundEvent sound, float volume, float pitch, CallbackInfo ci) {
        var client = MinecraftClient.getInstance();
        var player = client.player;

        // This is a wierd way of checking this, but Minecraft handles Death like this, so...
        if(sound == this.getDeathSound()){
            var coords = new Coordinates(player.getBlockX(), player.getBlockY(), player.getBlockZ());
            var dimension = client.world.getRegistryKey();

            WaypointManager.updateLastDeathWaypoint(coords, dimension);
        }
    }
}

