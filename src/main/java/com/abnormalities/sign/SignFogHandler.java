package com.abnormalities.sign;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SignFogHandler {
    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.level.dimension() != SignDimension.LEVEL_KEY) return;
        event.setNearPlaneDistance(2.0F);
        event.setFarPlaneDistance(48.0F);
        event.setFogShape(com.mojang.blaze3d.shaders.FogShape.SPHERE);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.level.dimension() != SignDimension.LEVEL_KEY) return;
        event.setRed(0.03F);
        event.setGreen(0.03F);
        event.setBlue(0.06F);
    }
}
