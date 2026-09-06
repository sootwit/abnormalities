package com.abnormalities.client;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;

public class OverlayManager {
    private static final TreeMap<Integer, BiConsumer<Integer, Integer>> OVERLAYS = new TreeMap<>();

    public static void register(int priority, BiConsumer<Integer, Integer> renderer) {
        OVERLAYS.put(priority, renderer);
    }

    public static void unregister(int priority) {
        OVERLAYS.remove(priority);
    }

    public static boolean hasOverlays() {
        return !OVERLAYS.isEmpty();
    }

    public static void renderAll(int sw, int sh) {
        for (BiConsumer<Integer, Integer> renderer : OVERLAYS.values()) {
            renderer.accept(sw, sh);
        }
    }
}
