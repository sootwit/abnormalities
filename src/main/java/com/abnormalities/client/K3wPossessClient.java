package com.abnormalities.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = com.abnormalities.AbnormalitiesMod.MODID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class K3wPossessClient {
    private static boolean possessed = false;
    private static boolean blackout = false;
    private static long blackoutEnd = 0;
    private static int k3wEntityId = -1;

    public static void handle(int phase, int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (phase == 0) {
            possessed = true;
            blackout = true;
            blackoutEnd = System.currentTimeMillis() + 5000L;
            k3wEntityId = entityId;
        } else if (phase == 1) {
            blackout = false;
            if (mc.options != null) mc.options.hideGui = true;
        } else if (phase == -1) {
            possessed = false;
            blackout = false;
            k3wEntityId = -1;
            if (mc.options != null) mc.options.hideGui = false;
        }
    }

    public static boolean isPossessed() {
        return possessed;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!possessed) return;
        if (blackout && System.currentTimeMillis() >= blackoutEnd) {
            blackout = false;
        }
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
        if (possessed) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (possessed) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (!possessed) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) mc.getSoundManager().resume();
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiEvent.Post event) {
        if (!possessed) return;
        var gui = event.getGuiGraphics();
        var mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        if (blackout) {
            gui.fill(0, 0, w, h, 0xFF000000);
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!possessed || k3wEntityId < 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity k3w = mc.level.getEntity(k3wEntityId);
        if (k3w == null) return;
        event.setYaw(k3w.getYHeadRot());
        event.setPitch(k3w.getXRot());
    }
}