package com.abnormalities.horror;

import com.abnormalities.WhisperManager;
import com.abnormalities.registry.ModEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BedMemoryManager {
    private static final int MAX_MEMORY = 8;
    private static final int HUNT_DURATION = 600;
    private static final int HUNT_RADIUS = 16;
    private static final int REPEAT_NUR_THRESHOLD = 3;
    private static final int NEAR_BED_DIST = 3;

    private static final List<String> HUNT_LINES = List.of(
        "it knows this room.",
        "it is watching the door.",
        "it is closer now."
    );

    private static final Map<UUID, List<BlockPos>> BEDS = new HashMap<>();
    private static final Map<UUID, Map<BlockPos, Integer>> REPEATS = new HashMap<>();
    private static final Map<UUID, HuntState> HUNTS = new HashMap<>();
    private static File dataFile = null;
    private static boolean loaded = false;
    private static long lastSaveTime = 0;

    private static class HuntState {
        BlockPos bed;
        int remaining;
        int lineIndex;
        HuntState(BlockPos bed) {
            this.bed = bed;
            this.remaining = HUNT_DURATION;
            this.lineIndex = 0;
        }
    }

    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (!(player instanceof ServerPlayer sp)) return;
        if (event.wakeImmediately()) return;

        BlockPos bed = findBed(sp);
        UUID uuid = sp.getUUID();
        List<BlockPos> beds = BEDS.computeIfAbsent(uuid, k -> new ArrayList<>());
        Map<BlockPos, Integer> reps = REPEATS.computeIfAbsent(uuid, k -> new HashMap<>());

        boolean repeat = false;
        BlockPos matched = bed;
        for (BlockPos known : beds) {
            if (known.distSqr(bed) <= NEAR_BED_DIST * NEAR_BED_DIST) {
                repeat = true;
                matched = known;
                break;
            }
        }

        if (repeat) {
            int count = reps.merge(matched, 1, Integer::sum);
            HUNTS.remove(uuid);
            WhisperManager.sendWhisper(sp, "you should not sleep in the same place twice.");
            sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                SoundEvents.AMBIENT_CAVE.get(), SoundSource.MASTER, 6.0f, 0.3f);
            if (count >= REPEAT_NUR_THRESHOLD) {
                save();
                ModEvents.forceNurSpawn(sp);
                return;
            }
            HUNTS.put(uuid, new HuntState(matched));
        } else {
            reps.put(bed, 1);
            if (beds.size() >= MAX_MEMORY) {
                BlockPos evicted = beds.remove(0);
                reps.remove(evicted);
            }
            beds.add(bed);
        }
        save();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        for (var it = HUNTS.entrySet().iterator(); it.hasNext();) {
            var e = it.next();
            ServerPlayer p = srv.getPlayerList().getPlayer(e.getKey());
            if (p == null || p.connection == null || !p.isAlive()) {
                it.remove();
                continue;
            }
            HuntState hunt = e.getValue();
            hunt.remaining--;
            if (hunt.remaining <= 0) {
                it.remove();
                continue;
            }
            if (hunt.remaining % 100 == 0) {
                BlockPos playerPos = p.blockPosition();
                if (playerPos.distSqr(hunt.bed) <= HUNT_RADIUS * HUNT_RADIUS) {
                    String line = HUNT_LINES.get(hunt.lineIndex % HUNT_LINES.size());
                    hunt.lineIndex++;
                    WhisperManager.sendWhisper(p, line);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (loaded) return;
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        if (sl.dimension() != Level.OVERWORLD) return;
        dataFile = sl.getServer().getWorldPath(LevelResource.ROOT).resolve("data/abnormalities_beds.nbt").toFile();
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
        BEDS.clear();
        REPEATS.clear();
        HUNTS.clear();
        dataFile = null;
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() == null) return;
        HUNTS.remove(event.getEntity().getUUID());
        save(true);
    }

    private static BlockPos findBed(ServerPlayer sp) {
        BlockPos sleeping = sp.getSleepingPos().orElse(null);
        if (sleeping != null) return sleeping;
        BlockPos pos = sp.blockPosition();
        for (int dx = -3; dx <= 3; dx++)
            for (int dy = -3; dy <= 3; dy++)
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos p = pos.offset(dx, dy, dz);
                    if (sp.level().getBlockState(p).getBlock() instanceof BedBlock) return p;
                }
        return pos;
    }

    private static void save() {
        save(false);
    }

    private static void save(boolean force) {
        if (dataFile == null) return;
        long now = System.currentTimeMillis();
        if (!force && now - lastSaveTime < 5000) return;
        lastSaveTime = now;
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (var e : BEDS.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("u", e.getKey());
            ListTag bl = new ListTag();
            for (BlockPos p : e.getValue()) {
                CompoundTag bc = new CompoundTag();
                bc.putInt("x", p.getX());
                bc.putInt("y", p.getY());
                bc.putInt("z", p.getZ());
                bl.add(bc);
            }
            t.put("beds", bl);
            ListTag rl = new ListTag();
            for (var re : REPEATS.getOrDefault(e.getKey(), Map.of()).entrySet()) {
                CompoundTag rc = new CompoundTag();
                rc.putInt("x", re.getKey().getX());
                rc.putInt("y", re.getKey().getY());
                rc.putInt("z", re.getKey().getZ());
                rc.putInt("c", re.getValue());
                rl.add(rc);
            }
            t.put("reps", rl);
            list.add(t);
        }
        tag.put("players", list);
        try {
            dataFile.getParentFile().mkdirs();
            NbtIo.write(tag, dataFile);
        } catch (IOException ignored) {}
    }

    private static void load() {
        if (dataFile == null || !dataFile.exists()) return;
        BEDS.clear();
        REPEATS.clear();
        try {
            CompoundTag tag = NbtIo.read(dataFile);
            if (tag == null) return;
            ListTag list = tag.getList("players", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag t = list.getCompound(i);
                UUID uuid = t.getUUID("u");
                List<BlockPos> beds = new ArrayList<>();
                ListTag bl = t.getList("beds", Tag.TAG_COMPOUND);
                for (int j = 0; j < bl.size(); j++) {
                    CompoundTag bc = bl.getCompound(j);
                    beds.add(new BlockPos(bc.getInt("x"), bc.getInt("y"), bc.getInt("z")));
                }
                BEDS.put(uuid, beds);
                Map<BlockPos, Integer> reps = new HashMap<>();
                ListTag rl = t.getList("reps", Tag.TAG_COMPOUND);
                for (int j = 0; j < rl.size(); j++) {
                    CompoundTag rc = rl.getCompound(j);
                    reps.put(new BlockPos(rc.getInt("x"), rc.getInt("y"), rc.getInt("z")), rc.getInt("c"));
                }
                REPEATS.put(uuid, reps);
            }
        } catch (IOException ignored) {}
    }
}
