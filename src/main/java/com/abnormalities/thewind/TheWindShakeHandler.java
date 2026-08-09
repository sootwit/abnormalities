package com.abnormalities.thewind;

import com.abnormalities.AbnormalitiesMod;
import com.abnormalities.network.TheWindShakePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "abnormalities", value = Dist.CLIENT)
public class TheWindShakeHandler {
    private static final Map<UUID, float[]> SHAKES = new HashMap<>();
    private static final java.util.Random RNG = new java.util.Random();

    public static void triggerShake(Player player, float intensity, int duration) {
        SHAKES.put(player.getUUID(), new float[]{intensity, duration, intensity});
    }

    public static void sendShake(ServerPlayer player, float intensity, int duration) {
        AbnormalitiesMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new TheWindShakePacket(intensity, duration));
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        UUID id = mc.player.getUUID();
        float[] shake = SHAKES.get(id);
        if (shake == null) return;

        float intensity = shake[0];
        int remaining = (int) shake[1];
        float original = shake[2];

        if (remaining <= 0) {
            SHAKES.remove(id);
            return;
        }

        float factor = intensity / Math.max(original, 0.01f);
        event.setPitch(event.getPitch() + (RNG.nextFloat() - 0.5f) * 6.0f * factor);
        event.setYaw(event.getYaw() + (RNG.nextFloat() - 0.5f) * 6.0f * factor);
        event.setRoll(event.getRoll() + (RNG.nextFloat() - 0.5f) * 3.0f * factor);

        shake[1] = remaining - 1;
        shake[0] = intensity * 0.98f;
    }
}
