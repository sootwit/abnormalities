package com.abnormalities.horror;

import com.abnormalities.WhisperManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class InsanityMeter {
    private static final double START_INSANITY = 100.0;
    private static final double RISE_NIGHT = 0.04;
    private static final double RISE_DARK = 0.04;
    private static final double RISE_CAVE = 0.04;
    private static final double FALL_SAFE = 0.06;
    private static final double MAX = 1000.0;
    private static final double MIN = 0.0;
    private static final int WHISPER_INTERVAL = 200;
    private static final int DISPLAY_INTERVAL = 400;
    private static final int DISPLAY_THRESHOLD = 300;
    private static final long SAVE_THROTTLE_MS = 5000;
    private static final Random RNG = new Random();
    private static final Map<UUID, Double> INSANITY = new HashMap<>();
    private static final Map<UUID, Long> NEXT_WHISPER = new HashMap<>();
    private static final Map<UUID, Long> NEXT_DISPLAY = new HashMap<>();
    private static File dataFile = null;
    private static boolean loaded = false;
    private static long lastSaveTime = 0;

    private static final List<String> WHISPERS = List.of(
        "your eyes are playing tricks on you.",
        "there is something behind you. do not turn around.",
        "it is inside your head now.",
        "you are not yourself anymore."
    );

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.side != LogicalSide.SERVER) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.level().dimension() != Level.OVERWORLD) return;
        UUID uuid = sp.getUUID();
        Level level = sp.level();
        long dayTime = level.getDayTime() % 24000;
        boolean night = dayTime >= 13000 && dayTime < 23000;
        BlockPos pos = sp.blockPosition();
        int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
        double delta = 0;
        if (night) delta += RISE_NIGHT;
        if (blockLight < 5) delta += RISE_DARK;
        if (sp.getY() <= 45) delta += RISE_CAVE;
        if (!night && blockLight >= 8 && !level.canSeeSky(pos)) delta -= FALL_SAFE;
        double raw = INSANITY.getOrDefault(uuid, START_INSANITY);
        if (delta != 0) {
            raw = Math.max(MIN, Math.min(MAX, raw + delta));
            INSANITY.put(uuid, raw);
        }
        int val = (int) raw;
        long now = sp.server.getTickCount();
        Long nw = NEXT_WHISPER.get(uuid);
        if (nw == null) {
            NEXT_WHISPER.put(uuid, now + WHISPER_INTERVAL);
        } else if (now >= nw) {
            if (val > 600) WhisperManager.sendWhisper(sp, pickWhisper(val));
            NEXT_WHISPER.put(uuid, now + WHISPER_INTERVAL);
        }
        Long nd = NEXT_DISPLAY.get(uuid);
        if (nd == null) {
            NEXT_DISPLAY.put(uuid, now + DISPLAY_INTERVAL);
        } else if (now >= nd) {
            if (val > DISPLAY_THRESHOLD) {
                ChatFormatting color = val >= 900 ? ChatFormatting.GOLD : ChatFormatting.GRAY;
                sp.displayClientMessage(Component.literal("insanity: " + val).withStyle(color), true);
            }
            NEXT_DISPLAY.put(uuid, now + DISPLAY_INTERVAL);
        }
    }

    public static void forceSpike(ServerPlayer player) {
        UUID uuid = player.getUUID();
        INSANITY.put(uuid, 900.0);
        NEXT_WHISPER.put(uuid, 0L);
        NEXT_DISPLAY.put(uuid, 0L);
        WhisperManager.sendWhisper(player, pickWhisper(900));
        player.displayClientMessage(Component.literal("insanity: 900").withStyle(ChatFormatting.GOLD), true);
        save(true);
    }

    public static void forceReset(ServerPlayer player) {
        UUID uuid = player.getUUID();
        INSANITY.put(uuid, START_INSANITY);
        NEXT_WHISPER.remove(uuid);
        NEXT_DISPLAY.remove(uuid);
        save(true);
    }

    private static String pickWhisper(int val) {
        if (val >= 900) return WHISPERS.get(RNG.nextInt(WHISPERS.size()));
        if (val >= 750) return WHISPERS.get(RNG.nextInt(3));
        if (val >= 600) return WHISPERS.get(RNG.nextInt(2));
        return WHISPERS.get(0);
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
        ListTag list = new ListTag();
        for (var e : INSANITY.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("u", e.getKey());
            t.putDouble("v", e.getValue());
            list.add(t);
        }
        tag.put("insanities", list);
        try {
            dataFile.getParentFile().mkdirs();
            NbtIo.writeCompressed(tag, dataFile);
        } catch (IOException ignored) {}
    }

    private static void load() {
        if (dataFile == null || !dataFile.exists()) return;
        INSANITY.clear();
        try {
            CompoundTag tag = NbtIo.readCompressed(dataFile);
            if (tag == null) return;
            ListTag list = tag.getList("insanities", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag t = list.getCompound(i);
                INSANITY.put(t.getUUID("u"), t.getDouble("v"));
            }
        } catch (IOException ignored) {}
    }

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (loaded) return;
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        if (sl.dimension() != Level.OVERWORLD) return;
        dataFile = sl.getServer().getWorldPath(LevelResource.ROOT).resolve("data/abnormalities_insanity.nbt").toFile();
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
        INSANITY.clear();
        NEXT_WHISPER.clear();
        NEXT_DISPLAY.clear();
        dataFile = null;
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() == null) return;
        UUID uuid = event.getEntity().getUUID();
        NEXT_WHISPER.remove(uuid);
        NEXT_DISPLAY.remove(uuid);
        save(true);
    }
}
