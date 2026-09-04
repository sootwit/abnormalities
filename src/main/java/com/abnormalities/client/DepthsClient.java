package com.abnormalities.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = com.abnormalities.AbnormalitiesMod.MODID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class DepthsClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|DepthsClient");
    private static boolean active = false;

    public static void handle(boolean a) {
        active = a;
        LOGGER.info("[DepthsClient] depths {}", a ? "started" : "ended");
    }

    public static boolean isActive() {
        return active;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        Player player = mc.player;
        if (!player.isInWater()) return;
        var opts = mc.options;
        boolean sprinting = opts.keySprint.isDown();
        boolean moving = opts.keyUp.isDown() || opts.keyDown.isDown() || opts.keyLeft.isDown() || opts.keyRight.isDown();
        if (sprinting && moving) {
            opts.keySprint.setDown(false);
            player.setSprinting(false);
        }
        if (opts.keyJump.isDown()) {
            opts.keyJump.setDown(false);
        }
    }

    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton.Pre event) {
        if (active) event.setCanceled(true);
    }
}