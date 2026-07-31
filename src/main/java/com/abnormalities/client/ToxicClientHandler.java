package com.abnormalities.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = com.abnormalities.AbnormalitiesMod.MODID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class ToxicClientHandler {
    private static boolean active = false;

    public static void setActive(boolean a) {
        active = a;
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
        var opts = mc.options;
        clearKey(opts.keyUp);
        clearKey(opts.keyDown);
        clearKey(opts.keyLeft);
        clearKey(opts.keyRight);
        clearKey(opts.keyJump);
        clearKey(opts.keyShift);
        clearKey(opts.keySprint);
        clearKey(opts.keyAttack);
        clearKey(opts.keyUse);
        clearKey(opts.keyDrop);
        clearKey(opts.keyInventory);
        clearKey(opts.keyChat);
        mc.player.setShiftKeyDown(false);
        mc.player.setSprinting(false);
    }

    private static void clearKey(KeyMapping key) {
        key.setDown(false);
    }

    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton.Pre event) {
        if (active) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (active) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (!active) return;
        if (event.getNewScreen() instanceof DeathScreen) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) mc.getSoundManager().resume();
        event.setCanceled(true);
    }
}
