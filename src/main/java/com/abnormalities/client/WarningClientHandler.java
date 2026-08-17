package com.abnormalities.client;

import com.abnormalities.AbnormalitiesMod;
import com.abnormalities.network.WarningAckPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AbnormalitiesMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class WarningClientHandler {
    private static boolean pendingShow = false;

    public static void queueShow() {
        pendingShow = true;
    }

    @SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (!pendingShow) return;
        pendingShow = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        mc.setScreen(new WarningScreen());
    }
}
