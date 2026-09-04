package com.abnormalities.horror;

import com.abnormalities.ReputationManager;
import com.abnormalities.config.AbnormalitiesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.level.BlockEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CursedHouseManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|CursedHouse");
    private static final long SAVE_THROTTLE_MS = 5000;
    private static final Random RNG = new Random();
    private static final Map<UUID, List<CursedHouse>> HOUSES = new HashMap<>();
    private static File dataFile = null;
    private static boolean loaded = false;
    private static long lastSaveTime = 0;

    public record CursedHouse(int x, int y, int z, int curseLevel, int requiredRep, boolean cleansed) {
        public double distanceTo(BlockPos pos) {
            double dx = x - pos.getX();
            double dy = y - pos.getY();
            double dz = z - pos.getZ();
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }

    public static double getEventMultiplier(ServerPlayer player) {
        List<CursedHouse> houses = HOUSES.getOrDefault(player.getUUID(), List.of());
        double max = 1.0;
        for (CursedHouse h : houses) {
            if (h.cleansed()) continue;
            if (h.distanceTo(player.blockPosition()) > 16) continue;
            double mult = 1.0 + (h.curseLevel() / 1000.0) * (AbnormalitiesConfig.CURSED_HOUSE_EVENT_MULT_MAX.get() - 1.0);
            if (mult > max) max = mult;
        }
        return max;
    }

    public static boolean hasNearbyCursedHouse(ServerPlayer player, int range) {
        List<CursedHouse> houses = HOUSES.getOrDefault(player.getUUID(), List.of());
        for (CursedHouse h : houses) {
            if (h.cleansed()) continue;
            if (h.curseLevel() < 500) continue;
            if (h.distanceTo(player.blockPosition()) <= range) return true;
        }
        return false;
    }

    public static CursedHouse getNearestCursedHouse(ServerPlayer player, int range) {
        List<CursedHouse> houses = HOUSES.getOrDefault(player.getUUID(), List.of());
        CursedHouse nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (CursedHouse h : houses) {
            if (h.cleansed()) continue;
            double dist = h.distanceTo(player.blockPosition());
            if (dist <= range && dist < nearestDist) {
                nearest = h;
                nearestDist = dist;
            }
        }
        return nearest;
    }

    public static double getWindMultiplier(ServerPlayer player) {
        List<CursedHouse> houses = HOUSES.getOrDefault(player.getUUID(), List.of());
        for (CursedHouse h : houses) {
            if (h.cleansed()) continue;
            if (h.distanceTo(player.blockPosition()) <= AbnormalitiesConfig.CURSED_HOUSE_DETECTION_RANGE.get()) {
                return AbnormalitiesConfig.CURSED_HOUSE_WIND_MULTIPLIER.get();
            }
        }
        return 1.0;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!AbnormalitiesConfig.CURSED_HOUSE_ENABLED.get()) return;
        if (!loaded || dataFile == null) return;
        var srv = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        for (ServerPlayer player : new ArrayList<>(srv.getPlayerList().getPlayers())) {
            if (player.level().dimension() != Level.OVERWORLD) continue;
            if (player.tickCount % 20 != 0) continue;

            List<CursedHouse> houses = HOUSES.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
            boolean changed = false;

            for (int i = houses.size() - 1; i >= 0; i--) {
                CursedHouse h = houses.get(i);
                if (h.cleansed()) continue;
                if (h.distanceTo(player.blockPosition()) <= AbnormalitiesConfig.CURSED_HOUSE_DETECTION_RANGE.get()) {
                    int newLevel = Math.min(1000, h.curseLevel() + AbnormalitiesConfig.CURSED_HOUSE_CURSE_RATE.get().intValue());
                    if (newLevel != h.curseLevel()) {
                        houses.set(i, new CursedHouse(h.x(), h.y(), h.z(), newLevel, h.requiredRep(), false));
                        changed = true;
                        LOGGER.debug("[CursedHouse] {} curse level {} -> {} at ({}, {}, {})", player.getName().getString(), h.curseLevel(), newLevel, h.x(), h.y(), h.z());
                    }
                }
            }

            for (int i = houses.size() - 1; i >= 0; i--) {
                CursedHouse h = houses.get(i);
                if (h.cleansed()) continue;
                int rep = ReputationManager.getRep(player);
                if (rep > h.requiredRep() && h.distanceTo(player.blockPosition()) <= 32) {
                    int newLevel = h.curseLevel() - AbnormalitiesConfig.CURSED_HOUSE_DRAIN_RATE.get().intValue();
                    if (newLevel <= 0) {
                        houses.set(i, new CursedHouse(h.x(), h.y(), h.z(), 0, h.requiredRep(), true));
                        LOGGER.info("[CursedHouse] {} cleansed house at ({}, {}, {})", player.getName().getString(), h.x(), h.y(), h.z());
                    } else {
                        houses.set(i, new CursedHouse(h.x(), h.y(), h.z(), newLevel, h.requiredRep(), false));
                    }
                    changed = true;
                }
            }

            if (changed) save();
        }
    }

    public static void trackHouse(ServerPlayer player, BlockPos pos) {
        List<CursedHouse> houses = HOUSES.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
        for (CursedHouse h : houses) {
            if (h.distanceTo(pos) < 8) return;
        }
        if (houses.size() >= 20) houses.remove(0);
        int reqRep = 1500 + RNG.nextInt(801);
        houses.add(new CursedHouse(pos.getX(), pos.getY(), pos.getZ(), 0, reqRep, false));
        save();
    }

    public static boolean forceCurse(ServerPlayer player) {
        List<CursedHouse> houses = HOUSES.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
        for (CursedHouse h : houses) {
            if (!h.cleansed() && h.distanceTo(player.blockPosition()) <= 32) {
                int idx = houses.indexOf(h);
                houses.set(idx, new CursedHouse(h.x(), h.y(), h.z(), 1000, h.requiredRep(), false));
                save();
                return true;
            }
        }
        BlockPos pos = player.blockPosition();
        int reqRep = 1500 + RNG.nextInt(801);
        houses.add(new CursedHouse(pos.getX(), pos.getY(), pos.getZ(), 1000, reqRep, false));
        save();
        return true;
    }

    private static void save() {
        save(false);
    }

    private static void save(boolean force) {
        if (dataFile == null) return;
        long now = System.currentTimeMillis();
        if (!force && now - lastSaveTime < SAVE_THROTTLE_MS) return;
        lastSaveTime = now;
        CompoundTag tag = new CompoundTag();
        CompoundTag playersTag = new CompoundTag();
        for (var entry : HOUSES.entrySet()) {
            ListTag list = new ListTag();
            for (CursedHouse h : entry.getValue()) {
                CompoundTag t = new CompoundTag();
                t.putInt("x", h.x());
                t.putInt("y", h.y());
                t.putInt("z", h.z());
                t.putInt("cl", h.curseLevel());
                t.putInt("rr", h.requiredRep());
                t.putBoolean("cln", h.cleansed());
                list.add(t);
            }
            playersTag.put(entry.getKey().toString(), list);
        }
        tag.put("players", playersTag);
        try {
            dataFile.getParentFile().mkdirs();
            NbtIo.writeCompressed(tag, dataFile);
        } catch (IOException ignored) {}
    }

    private static void load() {
        if (dataFile == null || !dataFile.exists()) return;
        HOUSES.clear();
        try {
            CompoundTag tag = NbtIo.readCompressed(dataFile);
            if (tag == null) return;
            CompoundTag playersTag = tag.getCompound("players");
            for (String key : playersTag.getAllKeys()) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(key);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                ListTag list = playersTag.getList(key, Tag.TAG_COMPOUND);
                List<CursedHouse> houses = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag t = list.getCompound(i);
                    houses.add(new CursedHouse(t.getInt("x"), t.getInt("y"), t.getInt("z"),
                        t.getInt("cl"), t.getInt("rr"), t.getBoolean("cln")));
                }
                HOUSES.put(uuid, houses);
            }
        } catch (IOException ignored) {}
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!AbnormalitiesConfig.CURSED_HOUSE_ENABLED.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().dimension() != Level.OVERWORLD) return;
        BlockState state = event.getPlacedBlock();
        if (state.getBlock() instanceof BedBlock || state.getBlock() instanceof ChestBlock) {
            trackHouse(player, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (loaded) return;
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        if (sl.dimension() != Level.OVERWORLD) return;
        dataFile = sl.getServer().getWorldPath(LevelResource.ROOT).resolve("data/abnormalities_cursedhouses.nbt").toFile();
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
        if (dataFile != null) save(true);
        loaded = false;
        HOUSES.clear();
        dataFile = null;
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (dataFile != null) save(true);
    }
}
