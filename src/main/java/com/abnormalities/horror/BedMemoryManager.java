package com.abnormalities.horror;

import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.entity.NurEntity;
import com.abnormalities.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BedMemoryManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|BedMemory");
    private static final Map<UUID, BlockPos> LAST_BED = new HashMap<>();
    private static final Map<UUID, Integer> SLEEP_COUNT = new HashMap<>();
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
        recordSleep(sp, bedPos);
    }

    private static void recordSleep(ServerPlayer player, BlockPos bedPos) {
        UUID uuid = player.getUUID();
        BlockPos last = LAST_BED.get(uuid);
        int count = (last != null && last.equals(bedPos)) ? SLEEP_COUNT.getOrDefault(uuid, 0) + 1 : 1;
        LAST_BED.put(uuid, bedPos);
        SLEEP_COUNT.put(uuid, count);
        if (debug_mode) LOGGER.info("[BedMemory] {} sleep count {}", player.getName().getString(), count);
        if (count >= REPEAT_THRESHOLD) {
            SLEEP_COUNT.put(uuid, 0);
            spawnHuntNur(player, bedPos);
        }
    }

    private static void spawnHuntNur(ServerPlayer player, BlockPos bedPos) {
        ServerLevel level = (ServerLevel) player.level();
        double angle = level.random.nextDouble() * Math.PI * 2;
        double dist = 10.0 + level.random.nextDouble() * 6.0;
        double sx = bedPos.getX() + 0.5 + Math.cos(angle) * dist;
        double sz = bedPos.getZ() + 0.5 + Math.sin(angle) * dist;
        int sy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) sx, (int) sz);
        BlockPos spawnPos = BlockPos.containing(sx, sy, sz);
        if (!level.getBlockState(spawnPos.below()).canOcclude()) return;
        if (!level.getBlockState(spawnPos).canBeReplaced()) return;

        NurEntity nur = ModEntities.NUR.get().create(level);
        if (nur == null) return;
        nur.moveTo(sx, sy, sz, 0, 0);
        nur.currentState = NurEntity.State.STALKING;
        level.addFreshEntity(nur);
        LOGGER.info("[BedMemory] {} slept in the same bed {} times and it noticed", player.getName().getString(), REPEAT_THRESHOLD);
    }

    public static void forceHunt(ServerPlayer player) {
        BlockPos bedPos = player.getSleepingPos().orElse(player.blockPosition());
        spawnHuntNur(player, bedPos);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        LAST_BED.remove(uuid);
        SLEEP_COUNT.remove(uuid);
    }
}
