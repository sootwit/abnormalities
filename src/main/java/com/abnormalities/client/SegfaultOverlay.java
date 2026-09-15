package com.abnormalities.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = com.abnormalities.AbnormalitiesMod.MODID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class SegfaultOverlay {
    private static final ResourceLocation SEGFAULT_STOP_1 = new ResourceLocation("abnormalities", "textures/gui/segfault_stop1.png");
    private static final ResourceLocation SEGFAULT_STOP_2 = new ResourceLocation("abnormalities", "textures/gui/segfault_stop2.png");
    private static final ResourceLocation SEGFAULT_CONTINUE_1 = new ResourceLocation("abnormalities", "textures/gui/segfault_continue1.png");
    private static final ResourceLocation SEGFAULT_CONTINUE_2 = new ResourceLocation("abnormalities", "textures/gui/segfault_continue2.png");
    private static final ResourceLocation SEGFAULT_HIT_1 = new ResourceLocation("abnormalities", "textures/gui/segfault1.png");
    private static final ResourceLocation SEGFAULT_HIT_2 = new ResourceLocation("abnormalities", "textures/gui/segfault2.png");
    private static final ResourceLocation OVERLAY_STOP = new ResourceLocation("abnormalities", "textures/gui/segfault_overlaystop.png");
    private static final ResourceLocation OVERLAY_CONTINUE = new ResourceLocation("abnormalities", "textures/gui/segfault_overlaycontinue.png");
    private static final ResourceLocation TEXT_STOP = new ResourceLocation("abnormalities", "textures/gui/segfault_textstop.png");
    private static final ResourceLocation TEXT_CONTINUE = new ResourceLocation("abnormalities", "textures/gui/segfault_textcontinue.png");
    private static final int OVERLAY_PRIORITY = 40;
    private static boolean registeredOverlay = false;
    private static int animTick = 0;

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
            animTick = 0;
            return;
        }
        long elapsed = System.currentTimeMillis() - lastPacketTime;
        if (elapsed > 15000) {
            currentState = -1;
            animTick = 0;
            if (registeredOverlay) {
                OverlayManager.unregister(OVERLAY_PRIORITY);
                registeredOverlay = false;
            }
            return;
        }
        animTick++;
        if (!registeredOverlay) {
            OverlayManager.register(OVERLAY_PRIORITY, SegfaultOverlay::renderOverlay);
            registeredOverlay = true;
        }
    }

    private static ResourceLocation getAnimFrame(ResourceLocation f1, ResourceLocation f2) {
        return (animTick / 5) % 2 == 0 ? f1 : f2;
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
                gg.blit(getAnimFrame(SEGFAULT_HIT_1, SEGFAULT_HIT_2), fx, fy, 0, 0.0F, 0.0F, faceSize, faceSize, faceSize, faceSize);
                if (elapsed < 200) {
                    float a = 1.0F - elapsed / 200.0F;
                    gg.setColor(1.0F, 1.0F, 1.0F, a);
                    gg.blit(TEXT_STOP, 0, 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
                    gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                }
            } else {
                gg.blit(OVERLAY_CONTINUE, 0, 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
                gg.blit(getAnimFrame(SEGFAULT_HIT_1, SEGFAULT_HIT_2), fx, fy, 0, 0.0F, 0.0F, faceSize, faceSize, faceSize, faceSize);
                if (elapsed < 200) {
                    float a = 1.0F - elapsed / 200.0F;
                    gg.setColor(1.0F, 1.0F, 1.0F, a);
                    gg.blit(TEXT_CONTINUE, 0, 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
                    gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                }
            }
        } else if (currentState == 2) {
            gg.fill(0, 0, sw, sh, 0x88000000);
            gg.blit(getAnimFrame(SEGFAULT_HIT_1, SEGFAULT_HIT_2), fx, fy, 0, 0.0F, 0.0F, faceSize, faceSize, faceSize, faceSize);
        } else {
            if (currentState == 0) {
                gg.blit(OVERLAY_STOP, 0, 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
                gg.blit(getAnimFrame(SEGFAULT_STOP_1, SEGFAULT_STOP_2), fx, fy, 0, 0.0F, 0.0F, faceSize, faceSize, faceSize, faceSize);
                if (elapsed < 200) {
                    float a = 1.0F - elapsed / 200.0F;
                    gg.setColor(1.0F, 1.0F, 1.0F, a);
                    gg.blit(TEXT_STOP, 0, 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
                    gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                }
            } else if (currentState == 1) {
                gg.blit(OVERLAY_CONTINUE, 0, 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
                gg.blit(getAnimFrame(SEGFAULT_CONTINUE_1, SEGFAULT_CONTINUE_2), fx, fy, 0, 0.0F, 0.0F, faceSize, faceSize, faceSize, faceSize);
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
