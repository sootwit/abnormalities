package com.abnormalities.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = com.abnormalities.AbnormalitiesMod.MODID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class DistantFlashbackClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|DistantClient");
    private static final Deque<ResourceLocation> SCREENSHOT_BUFFER = new ArrayDeque<>();
    private static final int MAX_BUFFER = 8;
    private static final int CAPTURE_INTERVAL = 600;
    private static final Random RNG = new Random();
    private static final int OVERLAY_PRIORITY = 10;
    private static int captureTick = 0;
    private static int activeStage = 0;
    private static long stageStart = 0;
    private static ResourceLocation currentScreenshot = null;
    private static boolean overlayVisible = false;
    private static float fadeAlpha = 0.0f;
    private static int fadeDirection = 0;
    private static boolean loadedFromDisk = false;
    private static final java.util.Set<ResourceLocation> usedScreenshots = new java.util.HashSet<>();
    private static boolean registeredOverlay = false;

    public static void handleStage(int stage) {
        LOGGER.info("[DistantClient] received stage {}", stage);
        activeStage = stage;
        stageStart = System.currentTimeMillis();
        if (stage == 0) {
            fadeDirection = -1;
            return;
        }
        if (stage == 1) {
            usedScreenshots.clear();
            currentScreenshot = pickUnusedScreenshot();
            if (currentScreenshot != null) usedScreenshots.add(currentScreenshot);
            overlayVisible = true;
            fadeAlpha = 0.0f;
            fadeDirection = 1;
            registerOverlayIfNeeded();
            LOGGER.info("[DistantClient] stage 1 fade-in, screenshot={}", currentScreenshot);
        } else if (stage >= 2 && stage <= 6) {
            if (stage == 6) {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null && mc.player != null) {
                    mc.player.playSound(net.minecraft.sounds.SoundEvents.AMBIENT_CAVE.get(), 2.0f, 0.3f);
                }
            }
            fadeDirection = -2;
            LOGGER.info("[DistantClient] stage {} pitch-black transition", stage);
        } else {
            overlayVisible = false;
            currentScreenshot = null;
            unregisterOverlayIfNeeded();
        }
    }

    private static void registerOverlayIfNeeded() {
        if (!registeredOverlay) {
            OverlayManager.register(OVERLAY_PRIORITY, DistantFlashbackClient::renderOverlay);
            registeredOverlay = true;
        }
    }

    private static void unregisterOverlayIfNeeded() {
        if (registeredOverlay) {
            OverlayManager.unregister(OVERLAY_PRIORITY);
            registeredOverlay = false;
        }
    }

    private static void renderOverlay(int sw, int sh) {
        if (!overlayVisible) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        GuiGraphics gg = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
        gg.pose().pushPose();
        gg.pose().setIdentity();
        if (fadeDirection == -2) {
            gg.setColor(1.0F, 1.0F, 1.0F, 1.0f - fadeAlpha);
            if (currentScreenshot != null) {
                gg.blit(currentScreenshot, 0, 0, sw, sh, 0.0F, 0.0F, sw, sh, sw, sh);
            }
            gg.setColor(1.0F, 1.0F, 1.0F, fadeAlpha);
            gg.fill(0, 0, sw, sh, 0xFF000000);
        } else if (fadeDirection == 1) {
            gg.fill(0, 0, sw, sh, 0xFF000000);
            gg.setColor(1.0F, 1.0F, 1.0F, fadeAlpha);
            if (currentScreenshot != null) {
                gg.blit(currentScreenshot, 0, 0, sw, sh, 0.0F, 0.0F, sw, sh, sw, sh);
            }
        } else if (fadeDirection == -1) {
            gg.setColor(1.0F, 1.0F, 1.0F, fadeAlpha);
            if (currentScreenshot != null) {
                gg.blit(currentScreenshot, 0, 0, sw, sh, 0.0F, 0.0F, sw, sh, sw, sh);
            }
            gg.setColor(1.0F, 1.0F, 1.0F, 1.0f - fadeAlpha);
            gg.fill(0, 0, sw, sh, 0xFF000000);
        } else if (overlayVisible) {
            if (currentScreenshot != null) {
                gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                gg.blit(currentScreenshot, 0, 0, sw, sh, 0.0F, 0.0F, sw, sh, sw, sh);
            } else {
                gg.fill(0, 0, sw, sh, 0xFF000000);
            }
        }
        gg.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        gg.pose().popPose();
    }

    private static ResourceLocation pickRandomScreenshot() {
        if (SCREENSHOT_BUFFER.isEmpty()) return null;
        List<ResourceLocation> list = new ArrayList<>(SCREENSHOT_BUFFER);
        return list.get(RNG.nextInt(list.size()));
    }

    private static ResourceLocation pickUnusedScreenshot() {
        if (SCREENSHOT_BUFFER.isEmpty()) return null;
        List<ResourceLocation> unused = new ArrayList<>();
        for (ResourceLocation tex : SCREENSHOT_BUFFER) {
            if (!usedScreenshots.contains(tex)) {
                unused.add(tex);
            }
        }
        if (unused.isEmpty()) {
            usedScreenshots.clear();
            return pickRandomScreenshot();
        }
        return unused.get(RNG.nextInt(unused.size()));
    }

    public static void cancelFlashback() {
        activeStage = 0;
        overlayVisible = false;
        currentScreenshot = null;
        fadeAlpha = 0.0f;
        fadeDirection = 0;
        unregisterOverlayIfNeeded();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) return;

        if (!loadedFromDisk) {
            loadedFromDisk = true;
            loadScreenshotsFromDisk(mc);
        }

        captureTick++;
        if (captureTick == 1 || (captureTick % CAPTURE_INTERVAL == 0 && !overlayVisible && activeStage == 0)) {
            captureScreenshot(mc);
        }

        if (fadeDirection == 1) {
            fadeAlpha += 0.067f;
            if (fadeAlpha >= 1.0f) {
                fadeAlpha = 1.0f;
                fadeDirection = 0;
            }
        } else if (fadeDirection == -2) {
            fadeAlpha += 0.067f;
            if (fadeAlpha >= 1.0f) {
                fadeAlpha = 1.0f;
                fadeDirection = 0;
                ResourceLocation old = currentScreenshot;
                currentScreenshot = pickUnusedScreenshot();
                if (currentScreenshot != null) usedScreenshots.add(currentScreenshot);
                fadeAlpha = 1.0f;
                fadeDirection = 1;
                LOGGER.info("[DistantClient] pitch-black reached, new screenshot={}", currentScreenshot);
            }
        } else if (fadeDirection == -1) {
            fadeAlpha -= 0.067f;
            if (fadeAlpha <= 0.0f) {
                fadeAlpha = 0.0f;
                fadeDirection = 0;
                overlayVisible = false;
                currentScreenshot = null;
                unregisterOverlayIfNeeded();
            }
        }

        if (activeStage > 0 && overlayVisible) {
            long elapsed = System.currentTimeMillis() - stageStart;
            if (elapsed > 30000) {
                cancelFlashback();
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        cancelFlashback();
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            while (!SCREENSHOT_BUFFER.isEmpty()) {
                mc.getTextureManager().release(SCREENSHOT_BUFFER.removeFirst());
            }
        }
        captureTick = 0;
        loadedFromDisk = false;
        LOGGER.info("[DistantClient] logout, buffer cleared");
    }

    private static void loadScreenshotsFromDisk(Minecraft mc) {
        File screenshotsDir = new File(mc.gameDirectory, "screenshots");
        if (!screenshotsDir.isDirectory()) return;
        File[] files = screenshotsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
        if (files == null || files.length == 0) {
            LOGGER.info("[DistantClient] no screenshots found on disk");
            return;
        }
        List<File> fileList = new ArrayList<>(java.util.Arrays.asList(files));
        Collections.shuffle(fileList, RNG);
        int toLoad = Math.min(fileList.size(), MAX_BUFFER);
        int loaded = 0;
        for (int i = 0; i < toLoad && SCREENSHOT_BUFFER.size() < MAX_BUFFER; i++) {
            File f = fileList.get(i);
            try {
                NativeImage img;
                try (var is = Files.newInputStream(f.toPath())) {
                    img = NativeImage.read(is);
                }
                if (img == null) continue;
                DynamicTexture dyn = new DynamicTexture(img);
                ResourceLocation tex = mc.getTextureManager().register("abnormalities_distant_disk_" + System.nanoTime() + "_" + loaded, dyn);
                SCREENSHOT_BUFFER.addLast(tex);
                loaded++;
            } catch (IOException e) {
                LOGGER.warn("[DistantClient] failed to load {}: {}", f.getName(), e.toString());
            }
        }
        LOGGER.info("[DistantClient] loaded {} screenshots from disk, buffer={}", loaded, SCREENSHOT_BUFFER.size());
    }

    private static void captureScreenshot(Minecraft mc) {
        NativeImage img = null;
        DynamicTexture dyn = null;
        try {
            RenderTarget target = mc.getMainRenderTarget();
            if (target == null) return;
            int w = target.width;
            int h = target.height;
            img = new NativeImage(w, h, false);
            RenderSystem.bindTexture(target.getColorTextureId());
            img.downloadTexture(0, true);
            img.flipY();
            dyn = new DynamicTexture(img);
            img = null;
            ResourceLocation tex = mc.getTextureManager().register("abnormalities_distant_capture_" + System.nanoTime(), dyn);
            dyn = null;
            SCREENSHOT_BUFFER.addLast(tex);
            LOGGER.info("[DistantClient] captured screenshot {}x{} buffer={}", w, h, SCREENSHOT_BUFFER.size());
            while (SCREENSHOT_BUFFER.size() > MAX_BUFFER) {
                ResourceLocation old = SCREENSHOT_BUFFER.removeFirst();
                mc.getTextureManager().release(old);
            }
        } catch (Exception e) {
            if (img != null) img.close();
            if (dyn != null) dyn.close();
            LOGGER.warn("[DistantClient] screenshot capture failed: {}", e.toString());
        }
    }
}
