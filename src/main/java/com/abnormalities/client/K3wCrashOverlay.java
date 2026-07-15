package com.abnormalities.client;

import com.abnormalities.entity.K3wEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class K3wCrashOverlay {
    private static final ResourceLocation HUD = new ResourceLocation("abnormalities", "textures/gui/k3whud001.png");
    private static long crashStartTime = 0;
    private static boolean showingCrash = false;
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
        boolean crashing = false;
        var entities = mc.level.getEntitiesOfClass(K3wEntity.class, mc.player.getBoundingBox().inflate(128.0D));
        for (K3wEntity k3w : entities) {
            if (k3w.isCrashing()) {
                crashing = true;
                break;
            }
        }
        if (!crashing) {
            showingCrash = false;
            crashStartTime = 0;
            return;
        }
        long now = System.currentTimeMillis();
        if (!showingCrash) {
            showingCrash = true;
            crashStartTime = now;
        }
        long elapsed = now - crashStartTime;
        float alpha = Math.min(1.0F, elapsed / 200.0F);
        GuiGraphics gg = event.getGuiGraphics();
        int sw = gg.guiWidth();
        int sh = gg.guiHeight();
        gg.pose().pushPose();
        gg.pose().setIdentity();
        gg.setColor(1.0F, 1.0F, 1.0F, alpha);
        gg.blit(HUD, 0, 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
        gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        gg.pose().popPose();
    }
}
