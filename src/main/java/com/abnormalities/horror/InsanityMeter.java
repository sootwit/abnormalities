package com.abnormalities.horror;

import com.abnormalities.WhisperManager;
import com.abnormalities.entity.NurEntity;
import com.abnormalities.registry.ModEntities;
import com.abnormalities.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InsanityMeter {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|InsanityMeter");
    private static final double START_INSANITY = 100.0;
    private static final double RISE_NIGHT = 0.04;
    private static final double RISE_DARK = 0.04;
    private static final double RISE_CAVE = 0.04;
    private static final double FALL_SAFE = 0.06;
    private static final double MAX = 1000.0;
    private static final double MIN = 0.0;
    private static final int WHISPER_INTERVAL = 600;
    private static final int DISPLAY_INTERVAL = 800;
    private static final int DISPLAY_THRESHOLD = 300;
    private static final int BLINDNESS_PULSE_INTERVAL = 100;
    private static final int BLINDNESS_PULSE_DURATION = 5;
    private static final int BLINDNESS_CONT_INTERVAL = 50;
    private static final int BLINDNESS_CONT_DURATION = 10;
    private static final int NAUSEA_INTERVAL = 400;
    private static final int NAUSEA_DURATION = 200;
    private static final long SAVE_THROTTLE_MS = 5000;
    private static final Random RNG = new Random();
    private static final Map<UUID, Double> INSANITY = new HashMap<>();
    private static final Map<UUID, Long> NEXT_WHISPER = new HashMap<>();
    private static final Map<UUID, Long> NEXT_DISPLAY = new HashMap<>();
    private static final Map<UUID, Long> NEXT_BLINDNESS = new HashMap<>();
    private static final Map<UUID, Long> NEXT_NAUSEA = new HashMap<>();
    private static File dataFile = null;
    private static boolean loaded = false;
    private static long lastSaveTime = 0;

    private static final List<String> WHISPERS = List.of(
        "Your eyes are playing tricks on you.",
        "Do not turn around.",
        "It is inside your head now.",
        "You are not yourself anymore."
    );

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.side != LogicalSide.SERVER) return;
        if (!com.abnormalities.config.AbnormalitiesConfig.INSANITY_ENABLED.get()) return;
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
            LOGGER.debug("[InsanityMeter] {} insanity={:.1f} delta={:.2f}", sp.getName().getString(), raw, delta);
        }
        int val = (int) raw;
        long now = sp.server.getTickCount();
        Long nw = NEXT_WHISPER.get(uuid);
        if (nw == null) {
            NEXT_WHISPER.put(uuid, now + WHISPER_INTERVAL);
        } else if (now >= nw) {
            if (val > 600) {
                WhisperManager.sendWhisper(sp, pickWhisper(val));
                caveSound(sp);
                placeSculk(sp);
            }
            if (val > 900) {
                spawnFakeNur(sp);
            }
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
        if (val > 900) {
            Long nb = NEXT_BLINDNESS.getOrDefault(uuid, now);
            if (now >= nb) {
                sp.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_CONT_DURATION, 0, false, false, false));
                sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                        ModSounds.HEARTBEAT_SOUND.get(), SoundSource.MASTER, 1.5f, 0.5f);
                NEXT_BLINDNESS.put(uuid, now + BLINDNESS_CONT_INTERVAL);
            }
            Long nn = NEXT_NAUSEA.getOrDefault(uuid, now);
            if (now >= nn) {
                sp.addEffect(new MobEffectInstance(MobEffects.CONFUSION, NAUSEA_DURATION, 0, false, false, false));
                NEXT_NAUSEA.put(uuid, now + NAUSEA_INTERVAL);
            }
        } else if (val > 750) {
            Long nb = NEXT_BLINDNESS.getOrDefault(uuid, now);
            if (now >= nb) {
                sp.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_PULSE_DURATION, 0, false, false, false));
                sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                        ModSounds.HEARTBEAT_SOUND.get(), SoundSource.MASTER, 1.5f, 0.5f);
                NEXT_BLINDNESS.put(uuid, now + BLINDNESS_PULSE_INTERVAL);
            }
            NEXT_NAUSEA.remove(uuid);
        } else {
            NEXT_BLINDNESS.remove(uuid);
            NEXT_NAUSEA.remove(uuid);
        }
    }

    public static void forceSpike(ServerPlayer player) {
        UUID uuid = player.getUUID();
        INSANITY.put(uuid, 900.0);
        NEXT_WHISPER.put(uuid, 0L);
        NEXT_DISPLAY.put(uuid, 0L);
        NEXT_BLINDNESS.put(uuid, 0L);
        NEXT_NAUSEA.put(uuid, 0L);
        LOGGER.info("[InsanityMeter] {} force spike to 900", player.getName().getString());
        WhisperManager.sendWhisper(player, pickWhisper(900));
        player.displayClientMessage(Component.literal("insanity: 900").withStyle(ChatFormatting.GOLD), true);
        save(true);
    }

    public static void forceReset(ServerPlayer player) {
        UUID uuid = player.getUUID();
        INSANITY.put(uuid, START_INSANITY);
        NEXT_WHISPER.remove(uuid);
        NEXT_DISPLAY.remove(uuid);
        NEXT_BLINDNESS.remove(uuid);
        NEXT_NAUSEA.remove(uuid);
        save(true);
    }

    private static String pickWhisper(int val) {
        if (val >= 900) return WHISPERS.get(RNG.nextInt(WHISPERS.size()));
        if (val >= 750) return WHISPERS.get(RNG.nextInt(3));
        if (val >= 600) return WHISPERS.get(RNG.nextInt(2));
        return WHISPERS.get(0);
    }

    private static void caveSound(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        double x = player.getX() + (RNG.nextDouble() - 0.5) * 16.0;
        double y = player.getY() + (RNG.nextDouble() - 0.5) * 8.0;
        double z = player.getZ() + (RNG.nextDouble() - 0.5) * 16.0;
        level.playSound(null, x, y, z, SoundEvents.AMBIENT_CAVE.get(), SoundSource.AMBIENT, 2.0f, 0.5f + RNG.nextFloat() * 0.4f);
    }

    private static void placeSculk(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        var sculk = Blocks.SCULK_VEIN.defaultBlockState();
        for (int i = 0; i < 5; i++) {
            double ox = (RNG.nextDouble() - 0.5) * 6;
            double oz = (RNG.nextDouble() - 0.5) * 6;
            BlockPos pos = BlockPos.containing(player.getX() + ox, player.getY(), player.getZ() + oz);
            if (level.getBlockState(pos).canBeReplaced() && level.getBlockState(pos.below()).canOcclude()) {
                level.setBlockAndUpdate(pos, sculk);
                return;
            }
        }
    }

    private static void spawnFakeNur(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        double angle = RNG.nextDouble() * Math.PI * 2;
        double dist = 15 + RNG.nextDouble() * 10;
        double sx = player.getX() + Math.cos(angle) * dist;
        double sz = player.getZ() + Math.sin(angle) * dist;
        int sy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) sx, (int) sz);
        BlockPos spawnPos = BlockPos.containing(sx, sy, sz);
        if (!level.getBlockState(spawnPos.below()).canOcclude()) return;
        if (!level.getBlockState(spawnPos).canBeReplaced()) return;
        NurEntity nur = ModEntities.NUR.get().create(level);
        if (nur == null) return;
        nur.moveTo(sx + 0.5, sy + 1, sz + 0.5, 0, 0);
        nur.currentState = NurEntity.State.DUMMY;
        nur.currentTarget = player;
        level.addFreshEntity(nur);
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
        NEXT_BLINDNESS.clear();
        NEXT_NAUSEA.clear();
        dataFile = null;
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() == null) return;
        UUID uuid = event.getEntity().getUUID();
        NEXT_WHISPER.remove(uuid);
        NEXT_DISPLAY.remove(uuid);
        NEXT_BLINDNESS.remove(uuid);
        NEXT_NAUSEA.remove(uuid);
        save(true);
    }
}
