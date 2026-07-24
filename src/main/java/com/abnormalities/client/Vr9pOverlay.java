package com.abnormalities.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = com.abnormalities.AbnormalitiesMod.MODID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class Vr9pOverlay {
    private static final ResourceLocation VR9P_STOP = new ResourceLocation("abnormalities", "textures/gui/vr9pstop.png");
    private static final ResourceLocation VR9P_CONTINUE = new ResourceLocation("abnormalities", "textures/gui/vr9pcontinue.png");
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
        if (currentState == 0) {
            gg.blit(VR9P_STOP, 0, 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
            gg.blit(OVERLAY_STOP, 0, 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
            if (elapsed < 200) {
                gg.setColor(1.0F, 1.0F, 1.0F, 1.0F - elapsed / 200.0F);
                gg.blit(TEXT_STOP, 0, 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
                gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        } else if (currentState == 1) {
            gg.blit(VR9P_CONTINUE, 0, 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
            gg.blit(OVERLAY_CONTINUE, 0, 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
            if (elapsed < 200) {
                gg.setColor(1.0F, 1.0F, 1.0F, 1.0F - elapsed / 200.0F);
                gg.blit(TEXT_CONTINUE, 0, 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
                gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
        gg.pose().popPose();
    }
}
