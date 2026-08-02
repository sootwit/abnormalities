package com.abnormalities.horror;

import com.abnormalities.config.AbnormalitiesConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

public class StillnessManager {
    private static final Map<UUID, Integer> STILL_TICKS = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        var overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        if (!AbnormalitiesConfig.H01D_ENABLED.get()) return;
        long currentDay = overworld.getDayTime() / 24000L;
        if (currentDay < AbnormalitiesConfig.GRACE_PERIOD_DAYS.get()) return;
        if (overworld.getGameTime() % 20 != 0) return;

        for (var sp : new java.util.ArrayList<>(overworld.getServer().getPlayerList().getPlayers())) {
            if (sp.isSleeping()) {
                STILL_TICKS.remove(sp.getUUID());
                continue;
            }
            double moved = sp.getDeltaMovement().horizontalDistanceSqr();
            boolean isMoving = moved > 0.0001;
            if (isMoving) {
                STILL_TICKS.remove(sp.getUUID());
                continue;
            }
            int ticks = STILL_TICKS.getOrDefault(sp.getUUID(), 0) + 1;
            STILL_TICKS.put(sp.getUUID(), ticks);
            if (ticks >= AbnormalitiesConfig.H01D_THRESHOLD.get()) {
                STILL_TICKS.put(sp.getUUID(), 0);
                triggerStillness(sp, overworld);
            }
        }
    }

    private static void triggerStillness(ServerPlayer sp, ServerLevel level) {
        SisterController.onHoldWarning(sp);
        int roll = level.random.nextInt(4);
        if (roll == 0) {
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                net.minecraft.core.Holder.direct(com.abnormalities.registry.ModSounds.WHISPER_SOUND.get()),
                net.minecraft.sounds.SoundSource.MASTER, sp.getX(), sp.getY(), sp.getZ(), 3.0f, 0.5f, 0));
        } else if (roll == 1) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double dist = 8.0D + level.random.nextDouble() * 6.0D;
            double sx = sp.getX() + Math.cos(angle) * dist;
            double sz = sp.getZ() + Math.sin(angle) * dist;
            int sy = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) sx, (int) sz);
            var nur = com.abnormalities.registry.ModEntities.NUR.get().create(level);
            if (nur != null) {
                nur.moveTo(sx + 0.5, sy + 1, sz + 0.5, 0, 0);
                nur.currentState = com.abnormalities.entity.NurEntity.State.STALKING;
                nur.currentTarget = sp;
                level.addFreshEntity(nur);
            }
        } else {
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSystemChatPacket(
                net.minecraft.network.chat.Component.literal("...").withStyle(net.minecraft.ChatFormatting.YELLOW, net.minecraft.ChatFormatting.ITALIC), false));
        }
    }

    public static void forceTrigger(ServerPlayer player) {
        triggerStillness(player, (ServerLevel) player.level());
    }
}
