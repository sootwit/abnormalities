package com.abnormalities.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = com.abnormalities.AbnormalitiesMod.MODID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class NoIdeaClient {
    private static final ResourceLocation IMAGE = new ResourceLocation("abnormalities", "textures/gui/youhavenoidea.png");
    private static final int OVERLAY_PRIORITY = 70;
    private static final long IMAGE_SHOW_MS = 3800L;
    private static boolean active = false;
    private static long startMs = 0;
    private static long durationMs = 10000L;
    private static boolean registeredOverlay = false;

    public static void start(int durationTicks) {
        Minecraft mc = Minecraft.getInstance();
        active = true;
        startMs = System.currentTimeMillis();
        durationMs = durationTicks * 50L;
        if (!registeredOverlay) {
            OverlayManager.register(OVERLAY_PRIORITY, NoIdeaClient::renderOverlay);
            registeredOverlay = true;
        }
        if (mc != null && mc.screen != null) mc.setScreen(null);
    }

    public static void end() {
        active = false;
        if (registeredOverlay) {
            OverlayManager.unregister(OVERLAY_PRIORITY);
            registeredOverlay = false;
        }
    }

    public static boolean isActive() {
        return active;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        if (mc.player == null || mc.level == null) {
            if (active) end();
            return;
        }
        if (!active) return;
        long elapsed = System.currentTimeMillis() - startMs;
        if (elapsed > durationMs + 1000L) {
            end();
            return;
        }
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
        if (mc.screen != null) mc.setScreen(null);
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
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (active) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        end();
    }

    private static void renderOverlay(int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        GuiGraphics gg = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
        long elapsed = System.currentTimeMillis() - startMs;
        if (elapsed < IMAGE_SHOW_MS) {
            gg.fill(0, 0, sw, sh, 0xFF000000);
            return;
        }
        gg.pose().pushPose();
        gg.pose().setIdentity();
        gg.fill(0, 0, sw, sh, 0xFF000000);
        float alpha = Math.min(1.0F, (elapsed - IMAGE_SHOW_MS) / 100.0F);
        gg.setColor(1.0F, 1.0F, 1.0F, alpha);
        gg.blit(IMAGE, 0, 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
        gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        gg.pose().popPose();
    }
}
