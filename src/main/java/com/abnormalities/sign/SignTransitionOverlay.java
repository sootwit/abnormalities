package com.abnormalities.sign;

import com.abnormalities.client.OverlayManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SignTransitionOverlay {
    private static final int OVERLAY_PRIORITY = 50;
    private static long showUntil = 0;
    private static boolean registeredOverlay = false;

    public static void show() {
        showUntil = System.currentTimeMillis() + 600L;
        if (!registeredOverlay) {
            OverlayManager.register(OVERLAY_PRIORITY, SignTransitionOverlay::renderOverlay);
            registeredOverlay = true;
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (showUntil == 0 && registeredOverlay) {
            OverlayManager.unregister(OVERLAY_PRIORITY);
            registeredOverlay = false;
        }
    }

    private static void renderOverlay(int w, int h) {
        if (showUntil == 0) return;
        long now = System.currentTimeMillis();
        if (now >= showUntil) {
            showUntil = 0;
            return;
        }
        float elapsed = (float)(now - (showUntil - 600L));
        float alpha = Math.min(1.0F, 1.0F - (elapsed / 600.0F));
        if (alpha <= 0.0F) {
            showUntil = 0;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        GuiGraphics gui = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
        int color = ((int)(alpha * 255) << 24);
        gui.fill(0, 0, w, h, color);
    }
}
