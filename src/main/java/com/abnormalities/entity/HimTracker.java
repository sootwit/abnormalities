package com.abnormalities.entity;

import com.abnormalities.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.io.File;
import java.io.IOException;

public class HimTracker {
    private static final String DATA_NAME = "abnormalities_him.nbt";

    private static int totalKills = 0;
    private static int bossKills = 0;
    private static boolean pendingBoss = false;
    private static File dataFile = null;
    private static boolean loaded = false;

    public static int getTotalKills() {
        return totalKills;
    }

    public static int getBossKills() {
        return bossKills;
    }

    public static int getEscalationTier() {
        return bossKills;
    }

    public static void himKilled(ServerPlayer killer) {
        totalKills++;
        if (totalKills % 10 == 0) {
            pendingBoss = true;
        }
        save();
    }

    public static void bossKilled(ServerPlayer killer) {
        totalKills++;
        bossKills++;
        pendingBoss = false;
        save();
    }

    public static boolean hasPendingBoss() {
        return pendingBoss;
    }

    public static boolean takePendingBoss() {
        if (!pendingBoss) return false;
        pendingBoss = false;
        save();
        return true;
    }

    private static void save() {
        if (dataFile == null) return;
        CompoundTag tag = new CompoundTag();
        tag.putInt("total", totalKills);
        tag.putInt("boss", bossKills);
        try {
            dataFile.getParentFile().mkdirs();
            NbtIo.write(tag, dataFile);
        } catch (IOException ignored) {}
    }

    private static void load() {
        if (dataFile == null || !dataFile.exists()) return;
        try {
            CompoundTag tag = NbtIo.read(dataFile);
            if (tag == null) return;
            totalKills = tag.getInt("total");
            bossKills = tag.getInt("boss");
        } catch (IOException ignored) {}
    }

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (loaded) return;
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        if (sl.dimension() != Level.OVERWORLD) return;
        dataFile = sl.getServer().getWorldPath(LevelResource.ROOT).resolve(DATA_NAME).toFile();
        load();
        loaded = true;
    }

    @SubscribeEvent
    public static void onWorldSave(LevelEvent.Save event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        if (sl.dimension() != Level.OVERWORLD) return;
        if (dataFile != null) save();
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        if (sl.dimension() != Level.OVERWORLD) return;
        if (dataFile != null) save();
        loaded = false;
        totalKills = 0;
        bossKills = 0;
        dataFile = null;
    }
}