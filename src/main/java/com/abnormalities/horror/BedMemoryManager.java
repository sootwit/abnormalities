package com.abnormalities.horror;

import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.registry.ModEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BedMemoryManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|BedMemory");
    private static final Map<UUID, List<BlockPos>> KNOWN_BEDS = new HashMap<>();
    private static final Map<UUID, Map<BlockPos, Integer>> REPEAT_COUNT = new HashMap<>();
    private static final int MAX_BEDS = 8;
    private static final int MATCH_RADIUS = 3;
    private static final int REPEAT_THRESHOLD = 3;
    private static boolean debug_mode = false;

    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (!AbnormalitiesConfig.B3D_ENABLED.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide) return;
        if (event.wakeImmediately()) return;

        long currentDay = sp.level().getDayTime() / 24000L;
        if (currentDay < AbnormalitiesConfig.GRACE_PERIOD_DAYS.get()) return;

        BlockPos bedPos = sp.getSleepingPos().orElse(null);
        if (bedPos == null) return;
        handleWake(sp, bedPos);
    }

    private static void handleWake(ServerPlayer player, BlockPos bedPos) {
        UUID uuid = player.getUUID();
        List<BlockPos> beds = KNOWN_BEDS.computeIfAbsent(uuid, k -> new ArrayList<>());

        BlockPos matched = null;
        for (BlockPos b : beds) {
            if (b.distSqr(bedPos) <= MATCH_RADIUS * MATCH_RADIUS) { matched = b; break; }
        }

        if (matched == null) {
            beds.add(bedPos.immutable());
            if (beds.size() > MAX_BEDS) {
                BlockPos evicted = beds.remove(0);
                Map<BlockPos, Integer> counts = REPEAT_COUNT.get(uuid);
                if (counts != null) counts.remove(evicted);
            }
            REPEAT_COUNT.computeIfAbsent(uuid, k -> new HashMap<>()).put(bedPos.immutable(), 1);
            return;
        }

        Map<BlockPos, Integer> counts = REPEAT_COUNT.computeIfAbsent(uuid, k -> new HashMap<>());
        int count = counts.getOrDefault(matched, 1) + 1;
        counts.put(matched, count);
        player.level().playSound(null, bedPos, SoundEvents.AMBIENT_CAVE.get(), SoundSource.AMBIENT, 6.0f, 0.3f);
        com.abnormalities.WhisperManager.sendActionBar(player, "...you slept here before...", ChatFormatting.DARK_GRAY);
        if (debug_mode) LOGGER.info("[BedMemory] {} repeat {} at {}", player.getName().getString(), count, bedPos);

        if (count >= REPEAT_THRESHOLD) {
            counts.put(matched, 0);
            ModEvents.forceNurSpawn(player);
            LOGGER.info("[BedMemory] {} slept in the same bed {} times and it remembers", player.getName().getString(), REPEAT_THRESHOLD);
        }
    }

    public static void forceHunt(ServerPlayer player) {
        ModEvents.forceNurSpawn(player);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        KNOWN_BEDS.remove(uuid);
        REPEAT_COUNT.remove(uuid);
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onWorldUnload(net.minecraftforge.event.level.LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        if (sl.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;
        KNOWN_BEDS.clear();
        REPEAT_COUNT.clear();
    }
}
