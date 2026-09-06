package com.abnormalities.client;

import com.abnormalities.entity.NurEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = com.abnormalities.AbnormalitiesMod.MODID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class NurFlickerOverlay {
    private static final ResourceLocation HUD_001 = new ResourceLocation("abnormalities", "textures/gui/nurhud001.png");
    private static final ResourceLocation HUD_002 = new ResourceLocation("abnormalities", "textures/gui/nurhud002.png");
    private static final ResourceLocation DUMMY_HUD = new ResourceLocation("abnormalities", "textures/gui/k3whud001.png");
    private static final int OVERLAY_PRIORITY = 20;
    private static boolean showingFlicker = false;
    private static boolean showingDummy = false;
    private static long cooldownEnd = 0;
    private static long flickerDuration = 0;
    private static long lastFlickerTime = 0;
    private static long nextFlickerTime = 0;
    private static boolean registeredOverlay = false;

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) return;
        boolean chasing = false;
        boolean dummy = false;
        var entities = mc.level.getEntitiesOfClass(NurEntity.class, mc.player.getBoundingBox().inflate(128.0D));
        for (NurEntity nur : entities) {
            if (nur.isChasing()) { chasing = true; break; }
            if (nur.isDummy()) dummy = true;
        }
        if (!chasing && !dummy) {
            showingFlicker = false;
            showingDummy = false;
            cooldownEnd = 0;
            nextFlickerTime = 0;
            if (registeredOverlay) {
                OverlayManager.unregister(OVERLAY_PRIORITY);
                registeredOverlay = false;
            }
            return;
        }
        if (!registeredOverlay) {
            OverlayManager.register(OVERLAY_PRIORITY, NurFlickerOverlay::renderOverlay);
            registeredOverlay = true;
        }
        long now = System.currentTimeMillis();
        if (showingFlicker) {
            if (now - lastFlickerTime >= flickerDuration) {
                showingFlicker = false;
                cooldownEnd = now + 2000 + (long)(Math.random() * 1000);
            }
        } else {
            if (now >= cooldownEnd && now >= nextFlickerTime) {
                showingFlicker = true;
                flickerDuration = 50 + (long)(Math.random() * 150);
                lastFlickerTime = now;
                nextFlickerTime = now + 3000 + (long)(Math.random() * 4000);
            }
        }
    }

    private static void renderOverlay(int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) return;
        boolean chasing = false;
        boolean dummy = false;
        var entities = mc.level.getEntitiesOfClass(NurEntity.class, mc.player.getBoundingBox().inflate(128.0D));
        for (NurEntity nur : entities) {
            if (nur.isChasing()) { chasing = true; break; }
            if (nur.isDummy()) dummy = true;
        }
        if (!chasing && !dummy) return;
        GuiGraphics gg = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
        ResourceLocation tex;
        if (dummy && !chasing) {
            tex = DUMMY_HUD;
        } else if (showingFlicker) {
            tex = HUD_002;
        } else {
            tex = HUD_001;
        }
        gg.pose().pushPose();
        gg.pose().setIdentity();
        gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        gg.blit(tex, 0, 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
        gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        gg.pose().popPose();
    }
}
