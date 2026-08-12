package com.abnormalities.horror;

import com.abnormalities.ReputationManager;
import com.abnormalities.config.AbnormalitiesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class B3drockManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|B3drock");
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        var overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        if (!AbnormalitiesConfig.B3DROCK_ENABLED.get()) return;
        long currentDay = overworld.getDayTime() / 24000L;
        if (currentDay < AbnormalitiesConfig.GRACE_PERIOD_DAYS.get()) return;

        COOLDOWNS.entrySet().removeIf(e -> overworld.getGameTime() >= e.getValue());

        if (overworld.getGameTime() % 200 != 0) return;

        for (ServerPlayer sp : overworld.players()) {
            UUID uuid = sp.getUUID();
            if (COOLDOWNS.containsKey(uuid)) continue;
            if (ReputationManager.getRep(sp) >= AbnormalitiesConfig.B3DROCK_MAX_REP.get()) continue;
            if (sp.isSleeping()) continue;

            int chance = AbnormalitiesConfig.B3DROCK_CHANCE.get();
            if (chance <= 0 || overworld.random.nextInt(chance) != 0) continue;

            triggerB3drock(sp, overworld);
            break;
        }
    }

    private static void triggerB3drock(ServerPlayer player, ServerLevel level) {
        UUID uuid = player.getUUID();
        long cooldownUntil = level.getGameTime() + AbnormalitiesConfig.B3DROCK_COOLDOWN.get();
        COOLDOWNS.put(uuid, cooldownUntil);

        int minSize = AbnormalitiesConfig.B3DROCK_MIN_SIZE.get();
        int maxSize = AbnormalitiesConfig.B3DROCK_MAX_SIZE.get();
        int sizeX = minSize + level.random.nextInt(maxSize - minSize + 1);
        int sizeZ = minSize + level.random.nextInt(maxSize - minSize + 1);

        double angle = level.random.nextDouble() * Math.PI * 2;
        double dist = 16.0D + level.random.nextDouble() * 112.0D;
        int centerX = (int) (player.getX() + Math.cos(angle) * dist);
        int centerZ = (int) (player.getZ() + Math.sin(angle) * dist);

        int startX = centerX - sizeX / 2;
        int startZ = centerZ - sizeZ / 2;

        int placed = 0;
        for (int x = startX; x < startX + sizeX; x++) {
            for (int z = startZ; z < startZ + sizeZ; z++) {
                for (int y = 0; y < 320; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.getBlockState(pos).is(Blocks.BEDROCK)) {
                        level.setBlock(pos, Blocks.BEDROCK.defaultBlockState(), 34);
                        placed++;
                    }
                }
            }
        }

        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            net.minecraft.core.Holder.direct(com.abnormalities.registry.ModSounds.NUR_SOUND.get()),
            SoundSource.MASTER, player.getX(), player.getY(), player.getZ(), 10.0f, 0.3f, 0));

        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSystemChatPacket(
            net.minecraft.network.chat.Component.literal("the earth has spoken."), false));

        LOGGER.info("[B3drock] {} triggered, placed {} bedrock blocks at ({}, 0, {}) size {}x{}x320",
            player.getName().getString(), placed, centerX, centerZ, sizeX, sizeZ);
    }

    public static void forceB3drock(ServerPlayer player) {
        var level = (ServerLevel) player.level();
        triggerB3drock(player, level);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() != null) {
            COOLDOWNS.remove(event.getEntity().getUUID());
        }
    }
}
