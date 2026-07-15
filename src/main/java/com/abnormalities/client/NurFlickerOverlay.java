package com.abnormalities.client;

import com.abnormalities.entity.NurEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class NurFlickerOverlay {
    private static final ResourceLocation HUD_001 = new ResourceLocation("abnormalities", "textures/gui/nurhud001.png");
    private static final ResourceLocation HUD_002 = new ResourceLocation("abnormalities", "textures/gui/nurhud002.png");
    private static boolean showingFlicker = false;
    private static int flickerCooldown = 0;
    private static int flickerDuration = 0;
    private static long lastFlickerTime = 0;
    private static boolean renderedThisFrame = false;
    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            renderedThisFrame = false;
        }
    }
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (renderedThisFrame) return;
        renderedThisFrame = true;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) return;
        if (mc.options.hideGui) return;
        boolean chasing = false;
        var entities = mc.level.getEntitiesOfClass(NurEntity.class, mc.player.getBoundingBox().inflate(128.0D));
        for (NurEntity nur : entities) {
            if (nur.isChasing()) {
                chasing = true;
                break;
            }
        }
        if (!chasing) return;
        GuiGraphics gg = event.getGuiGraphics();
        int sw = gg.guiWidth();
        int sh = gg.guiHeight();
        long now = System.currentTimeMillis();
        ResourceLocation tex;
        if (showingFlicker) {
            if (now - lastFlickerTime >= flickerDuration) {
                showingFlicker = false;
                flickerCooldown = 40 + (int)(Math.random() * 120);
            }
            tex = HUD_002;
        } else {
            if (flickerCooldown > 0) {
                flickerCooldown--;
            } else if (now - lastFlickerTime > 800) {
                if (Math.random() < 0.03) {
                    showingFlicker = true;
                    flickerDuration = 30 + (int)(Math.random() * 50);
                    lastFlickerTime = now;
                }
            }
            tex = HUD_001;
        }
        gg.pose().pushPose();
        gg.pose().setIdentity();
        gg.blit(tex, 0, 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
        gg.pose().popPose();
    }
}