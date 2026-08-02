package com.abnormalities.horror;

import com.abnormalities.AbnormalitiesMod;
import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.network.Vr9pPacket;
import com.abnormalities.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

public class LureController {
    private static final Map<UUID, LureState> ACTIVE = new HashMap<>();
    private static final Map<UUID, Integer> COOLDOWNS = new HashMap<>();
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("1ull");

    private static class LureState {
        BlockPos soundPos;
        int approaches = 0;
        int nextMelodyAt = 40;
        int totalTicks = 0;
        boolean hunting = false;
        int huntTicks = 0;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        var overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        if (!AbnormalitiesConfig.LURE_ENABLED.get()) return;
        long currentDay = overworld.getDayTime() / 24000L;
        if (currentDay < AbnormalitiesConfig.GRACE_PERIOD_DAYS.get()) return;

        COOLDOWNS.replaceAll((u, cd) -> cd - 1);
        COOLDOWNS.values().removeIf(cd -> cd <= 0);

        Iterator<Map.Entry<UUID, LureState>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            UUID uuid = e.getKey();
            LureState s = e.getValue();
            ServerPlayer player = overworld.getServer().getPlayerList().getPlayer(uuid);
            if (player == null || !player.isAlive() || !player.level().equals(overworld)) {
                it.remove();
                COOLDOWNS.put(uuid, AbnormalitiesConfig.LURE_COOLDOWN.get());
                continue;
            }
            s.totalTicks++;
            if (s.totalTicks > 7200) {
                it.remove();
                COOLDOWNS.put(uuid, AbnormalitiesConfig.LURE_COOLDOWN.get());
                continue;
            }
            s.nextMelodyAt--;
            if (s.nextMelodyAt <= 0) {
                s.nextMelodyAt = 38;
                playMelody(overworld, s.soundPos);
            }
            double dist = player.distanceToSqr(s.soundPos.getX() + 0.5, s.soundPos.getY() + 0.5, s.soundPos.getZ() + 0.5);
            if (!s.hunting && dist < 144.0D) {
                s.approaches++;
                if (s.approaches >= 3) {
                    s.hunting = true;
                    s.soundPos = new BlockPos(player.blockPosition());
                    s.huntTicks = 60;
                    LOGGER.info("{} 1ull hunting", player.getName().getString());
                } else {
                    relocate(overworld, s, player);
                }
            }
            if (s.hunting) {
                s.huntTicks--;
                if (s.huntTicks <= 0) {
                    it.remove();
                    COOLDOWNS.put(uuid, AbnormalitiesConfig.LURE_COOLDOWN.get());
                    punish(player);
                }
            }
        }

        if (overworld.getGameTime() % 80 != 0) return;
        if (overworld.random.nextInt(com.abnormalities.entity.HimTracker.weighted(AbnormalitiesConfig.LURE_SPAWN_WEIGHT.get())) != 0) return;
        for (var sp : new java.util.ArrayList<>(overworld.getServer().getPlayerList().getPlayers())) {
            if (ACTIVE.containsKey(sp.getUUID()) || COOLDOWNS.containsKey(sp.getUUID())) continue;
            if (!sp.level().equals(overworld)) continue;
            if (sp.isSleeping()) continue;
            long tod = overworld.getDayTime() % 24000L;
            if (tod < 13000L || tod > 23000L) continue;
            if (overworld.random.nextInt(3) != 0) continue;
            startLure(sp, overworld);
            break;
        }
    }

    private static void startLure(ServerPlayer player, ServerLevel level) {
        SisterController.onLureWarning(player);
        LureState s = new LureState();
        double angle = level.random.nextDouble() * Math.PI * 2;
        double dist = 26.0D + level.random.nextDouble() * 34.0D;
        int sx = player.blockPosition().getX() + (int) Math.round(Math.cos(angle) * dist);
        int sz = player.blockPosition().getZ() + (int) Math.round(Math.sin(angle) * dist);
        int sy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, sx, sz);
        s.soundPos = new BlockPos(sx, sy, sz);
        ACTIVE.put(player.getUUID(), s);
        playMelody(level, s.soundPos);
        LOGGER.info("{} 1ull started at {}", player.getName().getString(), s.soundPos);
    }

    private static void relocate(ServerLevel level, LureState s, ServerPlayer player) {
        double angle = level.random.nextDouble() * Math.PI * 2;
        double dist = 35.0D + level.random.nextDouble() * 20.0D;
        int sx = player.blockPosition().getX() + (int) Math.round(Math.cos(angle) * dist);
        int sz = player.blockPosition().getZ() + (int) Math.round(Math.sin(angle) * dist);
        int sy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, sx, sz);
        s.soundPos = new BlockPos(sx, sy, sz);
    }

    private static void playMelody(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                SoundEvents.NOTE_BLOCK_BELL.get(), SoundSource.MASTER, 12.0f, 1.4f);
        level.getServer().execute(() -> {
            level.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                    SoundEvents.NOTE_BLOCK_BELL.get(), SoundSource.MASTER, 12.0f, 1.1f);
        });
    }

    private static void punish(ServerPlayer player) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            net.minecraft.core.Holder.direct(ModSounds.NUR_SOUND.get()),
            SoundSource.MASTER, player.getX(), player.getY(), player.getZ(), 10.0f, 1.0f, 0));
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            net.minecraft.core.Holder.direct(ModSounds.VR9P_STOP.get()),
            SoundSource.MASTER, player.getX(), player.getY(), player.getZ(), 6.0f, 0.5f, 0));
        AbnormalitiesMod.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new Vr9pPacket(2, 0));
        float dmg = 10.0F;
        player.hurt(player.damageSources().genericKill(), dmg);
        var mode = AbnormalitiesConfig.LURE_PUNISH.get();
        if (mode == AbnormalitiesConfig.PunishMode.CRASH) {
            AbnormalitiesMod.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new com.abnormalities.network.CrashPacket());
        } else if (mode == AbnormalitiesConfig.PunishMode.KICK) {
            var srv = player.getServer();
            if (srv != null) {
                srv.tell(new net.minecraft.server.TickTask(srv.getTickCount() + 10, () -> {
                    if (player.connection != null) {
                        AbnormalitiesMod.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                                new Vr9pPacket(-1, 0));
                        com.abnormalities.horror.SisterController.onKickWarning(player);
                        player.connection.disconnect(Component.literal("1ULL"));
                    }
                }));
            }
        } else {
            AbnormalitiesMod.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new Vr9pPacket(-1, 0));
        }
    }

    public static void forceStart(ServerPlayer player) {
        if (ACTIVE.containsKey(player.getUUID())) return;
        startLure(player, (ServerLevel) player.level());
    }

    public static boolean isActive(UUID uuid) {
        return ACTIVE.containsKey(uuid);
    }

    @SubscribeEvent
    public static void onPlayerLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() != null) {
            UUID uuid = event.getEntity().getUUID();
            ACTIVE.remove(uuid);
            COOLDOWNS.put(uuid, AbnormalitiesConfig.LURE_COOLDOWN.get());
        }
    }
}
