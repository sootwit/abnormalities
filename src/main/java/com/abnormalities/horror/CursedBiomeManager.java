package com.abnormalities.horror;

import com.abnormalities.ReputationManager;
import com.abnormalities.config.AbnormalitiesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CursedBiomeManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|CursedBiome");
    private static final long SAVE_THROTTLE_MS = 5000;
    private static final Random RNG = new Random();
    private static final Map<UUID, List<CursedBiome>> BIOMES = new HashMap<>();
    private static File dataFile = null;
    private static boolean loaded = false;
    private static long lastSaveTime = 0;

    public record CursedBiome(int cx, int cz, int curseLevel, int requiredRep, String biomeId, boolean cleansed) {
        public boolean contains(BlockPos pos, ServerLevel level) {
            int dx = pos.getX() - cx;
            int dz = pos.getZ() - cz;
            if ((dx * dx + dz * dz) > 2500) return false;
            Holder<Biome> holder = level.getBiome(pos);
            Optional<ResourceKey<Biome>> key = holder.unwrapKey();
            return key.isPresent() && key.get().location().toString().equals(biomeId);
        }

        public double distanceTo(BlockPos pos) {
            double dx = cx - pos.getX();
            double dz = cz - pos.getZ();
            return Math.sqrt(dx * dx + dz * dz);
        }
    }

    public static boolean isInCursedBiome(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD) return false;
        ServerLevel sl = (ServerLevel) player.level();
        List<CursedBiome> biomes = BIOMES.getOrDefault(player.getUUID(), List.of());
        for (CursedBiome b : biomes) {
            if (b.cleansed()) continue;
            if (b.contains(player.blockPosition(), sl)) return true;
        }
        return false;
    }

    public static double getEventMultiplier(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD) return 1.0;
        ServerLevel sl = (ServerLevel) player.level();
        List<CursedBiome> biomes = BIOMES.getOrDefault(player.getUUID(), List.of());
        double max = 1.0;
        for (CursedBiome b : biomes) {
            if (b.cleansed()) continue;
            if (!b.contains(player.blockPosition(), sl)) continue;
            double mult = 1.0 + (b.curseLevel() / 1000.0);
            if (mult > max) max = mult;
        }
        return max;
    }

    public static double getWindMultiplier(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD) return 1.0;
        ServerLevel sl = (ServerLevel) player.level();
        List<CursedBiome> biomes = BIOMES.getOrDefault(player.getUUID(), List.of());
        for (CursedBiome b : biomes) {
            if (b.cleansed()) continue;
            if (b.contains(player.blockPosition(), sl)) {
                return AbnormalitiesConfig.CURSED_BIOME_WIND_MULTIPLIER.get();
            }
        }
        return 1.0;
    }

    public static int getHostileSpawnMultiplier(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD) return 1;
        ServerLevel sl = (ServerLevel) player.level();
        List<CursedBiome> biomes = BIOMES.getOrDefault(player.getUUID(), List.of());
        for (CursedBiome b : biomes) {
            if (b.cleansed()) continue;
            if (b.contains(player.blockPosition(), sl)) {
                return AbnormalitiesConfig.CURSED_BIOME_HOSTILE_SPAWN_MULT.get();
            }
        }
        return 1;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!AbnormalitiesConfig.CURSED_BIOME_ENABLED.get()) return;
        if (!loaded || dataFile == null) return;
        var srv = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        for (ServerPlayer player : new ArrayList<>(srv.getPlayerList().getPlayers())) {
            if (player.level().dimension() != Level.OVERWORLD) continue;
            if (player.tickCount % 20 != 0) continue;

            int rep = ReputationManager.getRep(player);
            List<CursedBiome> biomes = BIOMES.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
            boolean changed = false;

            if (rep >= 2200) {
                for (int i = biomes.size() - 1; i >= 0; i--) {
                    CursedBiome b = biomes.get(i);
                    if (!b.cleansed()) {
                        biomes.set(i, new CursedBiome(b.cx(), b.cz(), 0, b.requiredRep(), b.biomeId(), true));
                        changed = true;
                    }
                }
                if (changed) save();
                continue;
            }

            Holder<Biome> biomeHolder = overworld.getBiome(player.blockPosition());
            Optional<ResourceKey<Biome>> optKey = biomeHolder.unwrapKey();
            if (optKey.isPresent()) {
                ResourceLocation biomeKey = optKey.get().location();
                boolean alreadyTracked = false;
                for (CursedBiome b : biomes) {
                    if (b.biomeId().equals(biomeKey.toString()) && !b.cleansed() && b.contains(player.blockPosition(), overworld)) {
                        alreadyTracked = true;
                        break;
                    }
                }
                if (!alreadyTracked && biomes.stream().filter(b -> !b.cleansed()).count() < AbnormalitiesConfig.CURSED_BIOME_MAX.get()) {
                    if (rep < 2200 && RNG.nextInt(AbnormalitiesConfig.CURSED_BIOME_CHANCE.get()) == 0) {
                        int reqRep = 1500 + RNG.nextInt(801);
        biomes.add(new CursedBiome(player.blockPosition().getX(), player.blockPosition().getZ(),
            (int) AbnormalitiesConfig.CURSED_BIOME_CURSE_RATE.get().doubleValue(), reqRep, biomeKey.toString(), false));
                        changed = true;
                    }
                }
            }

            for (int i = biomes.size() - 1; i >= 0; i--) {
                CursedBiome b = biomes.get(i);
                if (b.cleansed()) continue;
                if (b.contains(player.blockPosition(), overworld)) {
                    if (rep > b.requiredRep()) {
                        int drain = AbnormalitiesConfig.CURSED_BIOME_DRAIN_RATE.get().intValue();
                        int newLevel = b.curseLevel() - drain;
                        if (newLevel <= 0) {
                            biomes.set(i, new CursedBiome(b.cx(), b.cz(), 0, b.requiredRep(), b.biomeId(), true));
                        } else {
                            biomes.set(i, new CursedBiome(b.cx(), b.cz(), newLevel, b.requiredRep(), b.biomeId(), false));
                        }
                        changed = true;
                    }
                }
            }

            if (biomes.size() > AbnormalitiesConfig.CURSED_BIOME_MAX.get()) {
                while (biomes.size() > AbnormalitiesConfig.CURSED_BIOME_MAX.get()) {
                    biomes.remove(0);
                }
                changed = true;
            }

            if (changed) save();
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel sl)) return;
        if (sl.dimension() != Level.OVERWORLD) return;
        if (sl.getServer().getTickCount() % 40 != 0) return;

        var srv = sl.getServer();
        for (ServerPlayer player : srv.getPlayerList().getPlayers()) {
            if (player.level().dimension() != Level.OVERWORLD) continue;
            List<CursedBiome> biomes = BIOMES.getOrDefault(player.getUUID(), List.of());
            for (CursedBiome b : biomes) {
                if (b.cleansed()) continue;
                if (!b.contains(player.blockPosition(), sl)) continue;

                float intensity = b.curseLevel() / 1000.0f;
                if (intensity > 0.2f) {
                    for (int i = 0; i < (int)(intensity * 5); i++) {
                        double ox = (RNG.nextDouble() - 0.5) * 20;
                        double oz = (RNG.nextDouble() - 0.5) * 20;
                        sl.sendParticles(ParticleTypes.LARGE_SMOKE,
                            player.getX() + ox, player.getY() + 2, player.getZ() + oz,
                            1, 0, 0.1, 0, 0.02);
                    }
                }

                if (intensity > 0.4f && RNG.nextInt(3) == 0) {
                    sl.playSound(null, player.getX() + (RNG.nextDouble() - 0.5) * 16,
                        player.getY() + (RNG.nextDouble() - 0.5) * 8,
                        player.getZ() + (RNG.nextDouble() - 0.5) * 16,
                        SoundEvents.AMBIENT_CAVE.get(), SoundSource.AMBIENT,
                        intensity * 3.0f, 0.5f + RNG.nextFloat() * 0.3f);
                }

                if (intensity > 0.6f && RNG.nextInt(10) == 0) {
                    List<Animal> animals = sl.getEntitiesOfClass(Animal.class,
                        player.getBoundingBox().inflate(32), a -> a.isAlive());
                    if (!animals.isEmpty()) {
                        Animal target = animals.get(RNG.nextInt(animals.size()));
                        target.discard();
                    }
                }
            }
        }
    }

    public static boolean forceCursed(ServerPlayer player) {
        List<CursedBiome> biomes = BIOMES.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
        Holder<Biome> biomeHolder = ((ServerLevel) player.level()).getBiome(player.blockPosition());
        Optional<ResourceKey<Biome>> optKey2 = biomeHolder.unwrapKey();
        ResourceLocation biomeKey = optKey2.isPresent() ? optKey2.get().location() : null;
        if (biomeKey == null) return false;
        for (CursedBiome b : biomes) {
            if (!b.cleansed() && b.contains(player.blockPosition(), (ServerLevel) player.level())) return false;
        }
        int reqRep = 1500 + RNG.nextInt(801);
        biomes.add(new CursedBiome(player.blockPosition().getX(), player.blockPosition().getZ(),
            1000, reqRep, biomeKey.toString(), false));
        LOGGER.info("[CursedBiome] {} cursed biome at ({}, {}) biome={} reqRep={}", player.getName().getString(), player.blockPosition().getX(), player.blockPosition().getZ(), biomeKey, reqRep);
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
        for (var entry : BIOMES.entrySet()) {
            ListTag list = new ListTag();
            for (CursedBiome b : entry.getValue()) {
                CompoundTag t = new CompoundTag();
                t.putInt("cx", b.cx());
                t.putInt("cz", b.cz());
                t.putInt("cl", b.curseLevel());
                t.putInt("rr", b.requiredRep());
                t.putString("bio", b.biomeId());
                t.putBoolean("cln", b.cleansed());
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
        BIOMES.clear();
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
                List<CursedBiome> biomes = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag t = list.getCompound(i);
                    biomes.add(new CursedBiome(t.getInt("cx"), t.getInt("cz"),
                        t.getInt("cl"), t.getInt("rr"), t.getString("bio"), t.getBoolean("cln")));
                }
                BIOMES.put(uuid, biomes);
            }
        } catch (IOException ignored) {}
    }

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (loaded) return;
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        if (sl.dimension() != Level.OVERWORLD) return;
        dataFile = sl.getServer().getWorldPath(LevelResource.ROOT).resolve("data/abnormalities_cursedbiomes.nbt").toFile();
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
        BIOMES.clear();
        dataFile = null;
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (dataFile != null) save(true);
    }
}
