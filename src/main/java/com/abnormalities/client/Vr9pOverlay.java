package com.abnormalities.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = com.abnormalities.AbnormalitiesMod.MODID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class Vr9pOverlay {
    private static final ResourceLocation VR9P_STOP = new ResourceLocation("abnormalities", "textures/gui/vr9pstop.png");
    private static final ResourceLocation VR9P_CONTINUE = new ResourceLocation("abnormalities", "textures/gui/vr9pcontinue.png");
    private static final ResourceLocation VR9P_HIT = new ResourceLocation("abnormalities", "textures/gui/vr9phit.png");
    private static final ResourceLocation OVERLAY_STOP = new ResourceLocation("abnormalities", "textures/gui/vr9p_overlaystop.png");
    private static final ResourceLocation OVERLAY_CONTINUE = new ResourceLocation("abnormalities", "textures/gui/vr9p_overlaycontinue.png");
    private static final ResourceLocation TEXT_STOP = new ResourceLocation("abnormalities", "textures/gui/vr9p_textstop.png");
    private static final ResourceLocation TEXT_CONTINUE = new ResourceLocation("abnormalities", "textures/gui/vr9p_textcontinue.png");
    private static final int OVERLAY_PRIORITY = 40;
    private static boolean registeredOverlay = false;

    public static int currentState = -1;
    public static long lastPacketTime = 0;

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) {
            if (registeredOverlay) {
                OverlayManager.unregister(OVERLAY_PRIORITY);
                registeredOverlay = false;
            }
            return;
        }
        if (currentState < 0) {
            if (registeredOverlay) {
                OverlayManager.unregister(OVERLAY_PRIORITY);
                registeredOverlay = false;
            }
            return;
        }
        long elapsed = System.currentTimeMillis() - lastPacketTime;
        if (elapsed > 15000) {
            currentState = -1;
            if (registeredOverlay) {
                OverlayManager.unregister(OVERLAY_PRIORITY);
                registeredOverlay = false;
            }
            return;
        }
        if (!registeredOverlay) {
            OverlayManager.register(OVERLAY_PRIORITY, Vr9pOverlay::renderOverlay);
            registeredOverlay = true;
        }
    }

    private static void renderOverlay(int sw, int sh) {
        if (currentState < 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        long elapsed = System.currentTimeMillis() - lastPacketTime;
        if (elapsed > 15000) { currentState = -1; return; }
        GuiGraphics gg = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
        gg.pose().pushPose();
        gg.pose().setIdentity();
        int faceSize = Math.min(sw, sh) / 3;
        int fx = (sw - faceSize) / 2;
        int fy = (sh - faceSize) / 2 - faceSize / 4;
        if (currentState == 3 || currentState == 4) {
            if (currentState == 3) {
                gg.blit(OVERLAY_STOP, 0, 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
                gg.blit(VR9P_HIT, fx, fy, 0, 0.0F, 0.0F, faceSize, faceSize, faceSize, faceSize);
                if (elapsed < 200) {
                    float a = 1.0F - elapsed / 200.0F;
                    gg.setColor(1.0F, 1.0F, 1.0F, a);
                    gg.blit(TEXT_STOP, 0, 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
                    gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                }
            } else {
                gg.blit(OVERLAY_CONTINUE, 0, 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
                gg.blit(VR9P_HIT, fx, fy, 0, 0.0F, 0.0F, faceSize, faceSize, faceSize, faceSize);
                if (elapsed < 200) {
                    float a = 1.0F - elapsed / 200.0F;
                    gg.setColor(1.0F, 1.0F, 1.0F, a);
                    gg.blit(TEXT_CONTINUE, 0, 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
                    gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                }
            }
        } else if (currentState == 2) {
            gg.fill(0, 0, sw, sh, 0x88000000);
            gg.blit(VR9P_HIT, fx, fy, 0, 0.0F, 0.0F, faceSize, faceSize, faceSize, faceSize);
        } else {
            if (currentState == 0) {
                gg.blit(OVERLAY_STOP, 0, 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
                gg.blit(VR9P_STOP, fx, fy, 0, 0.0F, 0.0F, faceSize, faceSize, faceSize, faceSize);
                if (elapsed < 200) {
                    float a = 1.0F - elapsed / 200.0F;
                    gg.setColor(1.0F, 1.0F, 1.0F, a);
                    gg.blit(TEXT_STOP, 0, 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
                    gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                }
            } else if (currentState == 1) {
                gg.blit(OVERLAY_CONTINUE, 0, 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
                gg.blit(VR9P_CONTINUE, fx, fy, 0, 0.0F, 0.0F, faceSize, faceSize, faceSize, faceSize);
                if (elapsed < 200) {
                    float a = 1.0F - elapsed / 200.0F;
                    gg.setColor(1.0F, 1.0F, 1.0F, a);
                    gg.blit(TEXT_CONTINUE, 0, 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
                    gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                }
            }
        }
        gg.pose().popPose();
    }
}
