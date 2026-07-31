package com.abnormalities.client;

import com.abnormalities.config.AbnormalitiesConfig;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ScreenshotEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Random;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = com.abnormalities.AbnormalitiesMod.MODID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class LensClientHandler {
    private static final Random RNG = new Random();
    private static long lastScreenshotTime = 0;

    @SubscribeEvent
    public static void onScreenshot(ScreenshotEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.player == null) return;
        if (!AbnormalitiesConfig.L3NS_ENABLED.get()) return;
        long now = System.currentTimeMillis();
        if (now - lastScreenshotTime < AbnormalitiesConfig.L3NS_COOLDOWN.get() * 1000L) return;
        long tod = mc.level.getDayTime() % 24000L;
        if (tod < 13000L && mc.level.getMaxLocalRawBrightness(mc.player.blockPosition()) > 7) return;
        if (RNG.nextInt(AbnormalitiesConfig.L3NS_CHANCE.get()) != 0) return;
        lastScreenshotTime = now;
        drawFigure(event.getImage());
    }

    private static void drawFigure(NativeImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int bodyH = (int) (h * 0.22);
        int bodyW = Math.max(2, (int) (w * 0.012));
        int headW = Math.max(2, (int) (w * 0.02));
        int bx = (int) (w * (0.68 + RNG.nextDouble() * 0.12));
        int by = (int) (h * (0.62 + RNG.nextDouble() * 0.15));
        int dark = 0xE0101010;
        img.fillRect(bx - bodyW / 2, by - bodyH, bodyW, bodyH, dark);
        img.fillRect(bx - headW / 2, by - bodyH - headW, headW, headW, dark);
        int eyeColor = 0xE0FFFFFF;
        img.fillRect(bx - headW / 4 - 1, by - bodyH - headW + headW / 3, 1, 1, eyeColor);
        img.fillRect(bx + headW / 4, by - bodyH - headW + headW / 3, 1, 1, eyeColor);
    }
}
