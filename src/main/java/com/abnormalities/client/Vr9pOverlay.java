package com.abnormalities.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = com.abnormalities.AbnormalitiesMod.MODID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class Vr9pOverlay {
    private static final ResourceLocation VR9P = new ResourceLocation("abnormalities", "textures/gui/vr9p.png");
    private static final ResourceLocation OVERLAY_STOP = new ResourceLocation("abnormalities", "textures/gui/vr9p_overlaystop.png");
    private static final ResourceLocation OVERLAY_CONTINUE = new ResourceLocation("abnormalities", "textures/gui/vr9p_overlaycontinue.png");
    private static final ResourceLocation TEXT_STOP = new ResourceLocation("abnormalities", "textures/gui/vr9p_textstop.png");
    private static final ResourceLocation TEXT_CONTINUE = new ResourceLocation("abnormalities", "textures/gui/vr9p_textcontinue.png");

    public static int currentState = -1;
    public static long stateStartTime = 0;
    public static int stateDuration = 0;

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.SUBTITLES.type()) return;
        if (currentState < 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        long elapsed = System.currentTimeMillis() - stateStartTime;
        if (stateDuration > 0 && elapsed > stateDuration * 50L) {
            currentState = -1;
            return;
        }
        GuiGraphics gg = event.getGuiGraphics();
        int sw = gg.guiWidth();
        int sh = gg.guiHeight();
        gg.pose().pushPose();
        gg.pose().setIdentity();
        gg.blit(VR9P, 0, 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
        if (currentState == 0) {
            gg.blit(OVERLAY_STOP, 0, 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
            gg.blit(TEXT_STOP, 0, 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
        } else if (currentState == 1) {
            gg.blit(OVERLAY_CONTINUE, 0, 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
            gg.blit(TEXT_CONTINUE, 0, 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
        }
        gg.pose().popPose();
    }
}
