package com.abnormalities.client;

import com.abnormalities.entity.FriendEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = com.abnormalities.AbnormalitiesMod.MODID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class FriendCrashOverlay {
    private static final ResourceLocation HUD = new ResourceLocation("abnormalities", "textures/gui/friendhud001.png");
    private static final int OVERLAY_PRIORITY = 30;
    private static long crashStartTime = 0;
    private static boolean showingCrash = false;
    private static boolean registeredOverlay = false;
    private static boolean apparitionActive = false;
    private static long apparitionStart = 0;

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) return;
        if (apparitionActive && System.currentTimeMillis() - apparitionStart > 100) {
            apparitionActive = false;
        }
        boolean crashing = false;
        var entities = mc.level.getEntitiesOfClass(FriendEntity.class, mc.player.getBoundingBox().inflate(128.0D));
        for (FriendEntity friend : entities) {
            if (friend.isCrashing()) { crashing = true; break; }
        }
        if (!crashing && !apparitionActive) {
            showingCrash = false;
            crashStartTime = 0;
            if (registeredOverlay) {
                OverlayManager.unregister(OVERLAY_PRIORITY);
                registeredOverlay = false;
            }
            return;
        }
        if (!registeredOverlay) {
            OverlayManager.register(OVERLAY_PRIORITY, FriendCrashOverlay::renderOverlay);
            registeredOverlay = true;
        }
        long now = System.currentTimeMillis();
        if (crashing && !showingCrash) {
            showingCrash = true;
            crashStartTime = now;
        }
    }

    private static void renderOverlay(int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) return;
        GuiGraphics gg = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
        if (apparitionActive) {
            gg.pose().pushPose();
            gg.pose().setIdentity();
            gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            gg.blit(HUD, 0, 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
            gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            gg.pose().popPose();
            return;
        }
        if (!showingCrash) return;
        float alpha = Math.min(1.0F, (System.currentTimeMillis() - crashStartTime) / 200.0F);
        gg.pose().pushPose();
        gg.pose().setIdentity();
        gg.setColor(1.0F, 1.0F, 1.0F, alpha);
        gg.blit(HUD, 0, 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
        gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        gg.pose().popPose();
    }

    public static void triggerApparition() {
        apparitionActive = true;
        apparitionStart = System.currentTimeMillis();
    }
}
