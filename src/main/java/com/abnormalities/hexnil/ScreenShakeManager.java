package com.abnormalities.hexnil;

import com.abnormalities.AbnormalitiesMod;
import com.abnormalities.network.HexNilShakePacket;
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
public class ScreenShakeManager {
    private static final Map<UUID, ShakeState> ACTIVE = new HashMap<>();

    private record ShakeState(int totalDuration, int elapsed, float intensity) {}

    public static void triggerShake(Player player, float intensity, int duration) {
        ACTIVE.put(player.getUUID(), new ShakeState(duration, 0, intensity));
    }

    public static void sendShake(ServerPlayer player, float intensity, int duration) {
        AbnormalitiesMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new HexNilShakePacket(intensity, duration));
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        UUID id = mc.player.getUUID();
        ShakeState state = ACTIVE.get(id);
        if (state == null) return;

        int elapsed = state.elapsed() + 1;
        if (elapsed >= state.totalDuration()) {
            ACTIVE.remove(id);
            return;
        }
        ACTIVE.put(id, new ShakeState(state.totalDuration(), elapsed, state.intensity()));

        float progress = (float) elapsed / (float) state.totalDuration();
        float amplitude = envelope(progress) * state.intensity();

        double time = System.currentTimeMillis() / 1000.0;

        float pitch = (float)(Math.sin(time * 11.0) * 0.7 + Math.sin(time * 17.3) * 0.3) * amplitude;
        float yaw = (float)(Math.sin(time * 13.0) * 0.5 + Math.sin(time * 19.1) * 0.5) * amplitude;
        float roll = (float)(Math.sin(time * 7.0) * 0.6 + Math.sin(time * 23.0) * 0.4) * amplitude * 0.5f;

        event.setPitch(event.getPitch() + pitch);
        event.setYaw(event.getYaw() + yaw);
        event.setRoll(event.getRoll() + roll);
    }

    private static float envelope(float progress) {
        float fadeIn = smoothstep(0.0f, 0.08f, progress);
        float fadeOut = 1.0f - smoothstep(0.65f, 1.0f, progress);
        return fadeIn * fadeOut;
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = Math.max(0.0f, Math.min(1.0f, (x - edge0) / (edge1 - edge0)));
        return t * t * (3.0f - 2.0f * t);
    }
}
