package com.abnormalities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ReputationManager {
    private static final Map<UUID, Integer> REP = new HashMap<>();
    private static final int NEUTRAL = 1250;
    private static final int MIN = 0;
    private static final int MAX = 2500;
    private static File dataFile;
    private static boolean loaded = false;

    public static int getRep(Player player) {
        return getRep(player.getUUID());
    }

    public static int getRep(UUID uuid) {
        return REP.getOrDefault(uuid, NEUTRAL);
    }

    public static void setRep(Player player, int value) {
        int clamped = Math.max(MIN, Math.min(MAX, value));
        REP.put(player.getUUID(), clamped);
        save();
    }

    public static void addRep(Player player, int delta) {
        if (delta == 0) return;
        int current = getRep(player);
        int clamped = Math.max(MIN, Math.min(MAX, current + delta));
        int actualDelta = clamped - current;
        if (actualDelta == 0) return;
        REP.put(player.getUUID(), clamped);
        save();
    }

    public static void addRep(UUID playerUUID, int delta) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        Player player = server.getPlayerList().getPlayer(playerUUID);
        if (player != null) addRep(player, delta);
    }

    public static String getTierLabel(int rep) {
        if (rep <= 299) return "horrible";
        if (rep <= 899) return "bad";
        if (rep <= 1599) return "neutral";
        if (rep <= 2199) return "good";
        return "godly";
    }

    public static double getWeightMultiplier(int rep, double hostilityFactor) {
        double mult = 1.0 + ((NEUTRAL - rep) / (double) NEUTRAL) * hostilityFactor;
        return Math.max(0.1, Math.min(3.0, mult));
    }

    public static double getWeightMultiplier(Player player, double hostilityFactor) {
        return getWeightMultiplier(getRep(player), hostilityFactor);
    }

    private static void save() {
        if (dataFile == null) return;
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (var e : REP.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("u", e.getKey());
            t.putInt("v", e.getValue());
            list.add(t);
        }
        tag.put("reps", list);
        try {
            dataFile.getParentFile().mkdirs();
            NbtIo.write(tag, dataFile);
        } catch (IOException ignored) {}
    }

    private static void load() {
        if (dataFile == null || !dataFile.exists()) return;
        REP.clear();
        try {
            CompoundTag tag = NbtIo.read(dataFile);
            if (tag == null) return;
            ListTag list = tag.getList("reps", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag t = list.getCompound(i);
                REP.put(t.getUUID("u"), t.getInt("v"));
            }
        } catch (IOException ignored) {}
    }

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (loaded) return;
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        if (sl.dimension() != Level.OVERWORLD) return;
        dataFile = sl.getServer().getWorldPath(LevelResource.ROOT).resolve("data/abnormalities_rep.nbt").toFile();
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
        REP.clear();
        dataFile = null;
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() != null) {
            save();
        }
    }
}
