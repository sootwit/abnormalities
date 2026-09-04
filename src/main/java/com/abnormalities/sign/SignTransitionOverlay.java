package com.abnormalities.sign;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SignTransitionOverlay {
    private static long showUntil = 0;

    public static void show() {
        showUntil = System.currentTimeMillis() + 600L;
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiEvent.Post event) {
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
        var gui = event.getGuiGraphics();
        var mc = net.minecraft.client.Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        int color = ((int)(alpha * 255) << 24);
        gui.fill(0, 0, w, h, color);
    }
}
