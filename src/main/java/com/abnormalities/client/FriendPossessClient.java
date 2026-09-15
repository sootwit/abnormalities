package com.abnormalities.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = com.abnormalities.AbnormalitiesMod.MODID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class FriendPossessClient {
    private static final int OVERLAY_PRIORITY = 60;
    private static boolean possessed = false;
    private static boolean blackout = false;
    private static long blackoutEnd = 0;
    private static int friendEntityId = -1;
    private static boolean registeredOverlay = false;

    public static void handle(int phase, int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (phase == 0) {
            possessed = true;
            blackout = true;
            blackoutEnd = System.currentTimeMillis() + 5000L;
            friendEntityId = entityId;
            if (!registeredOverlay) {
                OverlayManager.register(OVERLAY_PRIORITY, FriendPossessClient::renderOverlay);
                registeredOverlay = true;
            }
        } else if (phase == 1) {
            blackout = false;
            if (mc.options != null) mc.options.hideGui = true;
        } else if (phase == -1) {
            possessed = false;
            blackout = false;
            friendEntityId = -1;
            if (mc.options != null) mc.options.hideGui = false;
            if (registeredOverlay) {
                OverlayManager.unregister(OVERLAY_PRIORITY);
                registeredOverlay = false;
            }
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
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        possessed = false;
        blackout = false;
        friendEntityId = -1;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.options != null) mc.options.hideGui = false;
        if (registeredOverlay) {
            OverlayManager.unregister(OVERLAY_PRIORITY);
            registeredOverlay = false;
        }
    }

    private static void renderOverlay(int sw, int sh) {
        if (!possessed) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        if (blackout) {
            GuiGraphics gg = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
            gg.fill(0, 0, sw, sh, 0xFF000000);
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!possessed || friendEntityId < 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity friend = mc.level.getEntity(friendEntityId);
        if (friend == null) return;
        event.setYaw(friend.getYHeadRot());
        event.setPitch(friend.getXRot());
    }
}
