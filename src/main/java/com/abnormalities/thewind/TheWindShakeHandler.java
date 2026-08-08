package com.abnormalities.thewind;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "abnormalities", value = Dist.CLIENT)
public class TheWindShakeHandler {
    private static final Map<UUID, float[]> SHAKES = new HashMap<>();

    public static void triggerShake(Player player, float intensity, int duration) {
        SHAKES.put(player.getUUID(), new float[]{intensity, duration, intensity});
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
        java.util.Random rng = new java.util.Random();
        event.setPitch(event.getPitch() + (rng.nextFloat() - 0.5f) * 2.0f * factor);
        event.setYaw(event.getYaw() + (rng.nextFloat() - 0.5f) * 2.0f * factor);
        event.setRoll(event.getRoll() + (rng.nextFloat() - 0.5f) * 1.0f * factor);

        shake[1] = remaining - 1;
        shake[0] = intensity * 0.95f;
    }
}
