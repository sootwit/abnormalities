package com.abnormalities.horror;

import com.abnormalities.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class DarkAreaSoundManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|DarkArea");
    private static final Random RNG = new Random();
    private static final Map<UUID, Long> NEXT_THUMP = new HashMap<>();
    private static final Map<UUID, Long> NEXT_DOOR = new HashMap<>();
    private static final Map<UUID, Long> NEXT_SLOWED = new HashMap<>();
    private static final Map<UUID, Long> NEXT_ANIMAL = new HashMap<>();
    private static final Map<UUID, Long> NEXT_BREAK = new HashMap<>();
    private static final Map<UUID, Long> NEXT_WALK = new HashMap<>();
    private static final Map<UUID, Boolean> WAS_DARK = new HashMap<>();
    private static final List<ScheduledSound> SOUND_QUEUE = new ArrayList<>();

    private record ScheduledSound(long fireTick, SoundEvent sound, double x, double y, double z, float vol, float pitch, UUID player) {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var srv = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        long now = overworld.getGameTime();

        Iterator<ScheduledSound> qit = SOUND_QUEUE.iterator();
        while (qit.hasNext()) {
            ScheduledSound s = qit.next();
            if (now < s.fireTick) continue;
            qit.remove();
            ServerPlayer sp = srv.getPlayerList().getPlayer(s.player());
            if (sp == null || sp.connection == null) continue;
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                net.minecraft.core.Holder.direct(s.sound()), SoundSource.MASTER,
                s.x(), s.y(), s.z(), s.vol(), s.pitch(), 0));
        }

        for (ServerPlayer player : new java.util.ArrayList<>(srv.getPlayerList().getPlayers())) {
            if (player.level().dimension() != Level.OVERWORLD) continue;
            if (player.tickCount % 20 != 0) continue;

            BlockPos pos = player.blockPosition();
            int blockLight = player.level().getBrightness(LightLayer.BLOCK, pos);
            int skyLight = player.level().getBrightness(LightLayer.SKY, pos);
            boolean dark = blockLight < 5 && skyLight < 5 && !player.level().canSeeSky(pos);

            if (!dark) {
                WAS_DARK.put(player.getUUID(), false);
                continue;
            }
            Boolean wasDark = WAS_DARK.get(player.getUUID());
            if (wasDark == null || !wasDark) {
                LOGGER.debug("[DarkArea] {} entered dark area at ({}, {}, {})", player.getName().getString(), pos.getX(), pos.getY(), pos.getZ());
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                    net.minecraft.core.Holder.direct(ModSounds.LOWFREQ.get()), SoundSource.MASTER,
                    player.getX(), player.getY(), player.getZ(), 0.5f, 1.0f, 0));
            }
            WAS_DARK.put(player.getUUID(), true);

            Long nt = NEXT_THUMP.get(player.getUUID());
            if (nt == null) {
                NEXT_THUMP.put(player.getUUID(), now + 600 + RNG.nextInt(600));
            } else if (now >= nt) {
                double ox = (RNG.nextDouble() - 0.5) * 12;
                double oz = (RNG.nextDouble() - 0.5) * 12;
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                    net.minecraft.core.Holder.direct(ModSounds.THUMP.get()), SoundSource.MASTER,
                    player.getX() + ox, player.getY(), player.getZ() + oz,
                    0.8f, 0.5f + RNG.nextFloat() * 0.5f, 0));
                NEXT_THUMP.put(player.getUUID(), now + 800 + RNG.nextInt(800));
            }

            Long nd = NEXT_DOOR.get(player.getUUID());
            if (nd == null) {
                NEXT_DOOR.put(player.getUUID(), now + 4000 + RNG.nextInt(4000));
            } else if (now >= nd) {
                double ox = (RNG.nextDouble() - 0.5) * 16;
                double oz = (RNG.nextDouble() - 0.5) * 16;
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                    net.minecraft.core.Holder.direct(net.minecraft.sounds.SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR), SoundSource.MASTER,
                    player.getX() + ox, player.getY(), player.getZ() + oz,
                    0.6f, 0.8f + RNG.nextFloat() * 0.4f, 0));
                LOGGER.debug("[DarkArea] {} zombie door sound at ({}, {}, {})", player.getName().getString(), (int)(player.getX() + ox), (int)player.getY(), (int)(player.getZ() + oz));
                NEXT_DOOR.put(player.getUUID(), now + 4000 + RNG.nextInt(4000));
            }

            Long ns = NEXT_SLOWED.get(player.getUUID());
            if (ns == null) {
                NEXT_SLOWED.put(player.getUUID(), now + 1200 + RNG.nextInt(1200));
            } else if (now >= ns) {
                com.abnormalities.horror.SlowedMusicManager.forcePlay();
                NEXT_SLOWED.put(player.getUUID(), now + 1200 + RNG.nextInt(1200));
            }

            Long na = NEXT_ANIMAL.get(player.getUUID());
            if (na == null) {
                NEXT_ANIMAL.put(player.getUUID(), now + 500 + RNG.nextInt(600));
            } else if (now >= na) {
                com.abnormalities.horror.AnimalNoiseManager.forcePlay(player);
                NEXT_ANIMAL.put(player.getUUID(), now + 500 + RNG.nextInt(600));
            }

            Long nb = NEXT_BREAK.get(player.getUUID());
            if (nb == null) {
                NEXT_BREAK.put(player.getUUID(), now + 600 + RNG.nextInt(1200));
            } else if (now >= nb) {
                BlockPos breakPos = findNearbyBlockPos(overworld, pos, 10);
                if (breakPos != null) scheduleMiningSequence(player, overworld, breakPos, now);
                NEXT_BREAK.put(player.getUUID(), now + 900 + RNG.nextInt(1600));
            }

            Long nw = NEXT_WALK.get(player.getUUID());
            if (nw == null) {
                NEXT_WALK.put(player.getUUID(), now + 500 + RNG.nextInt(1000));
            } else if (now >= nw) {
                BlockPos floorPos = findNearbyFloorPos(overworld, pos, 12);
                if (floorPos != null) scheduleFootstepSequence(player, overworld, floorPos, now);
                NEXT_WALK.put(player.getUUID(), now + 800 + RNG.nextInt(1500));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() == null) return;
        UUID uuid = event.getEntity().getUUID();
        NEXT_THUMP.remove(uuid);
        NEXT_DOOR.remove(uuid);
        NEXT_SLOWED.remove(uuid);
        NEXT_ANIMAL.remove(uuid);
        NEXT_BREAK.remove(uuid);
        NEXT_WALK.remove(uuid);
        WAS_DARK.remove(uuid);
        SOUND_QUEUE.removeIf(s -> s.player().equals(uuid));
    }

    private static BlockPos findNearbyBlockPos(ServerLevel level, BlockPos center, int radius) {
        for (int i = 0; i < 12; i++) {
            int ox = RNG.nextInt(radius * 2 + 1) - radius;
            int oy = RNG.nextInt(7) - 3;
            int oz = RNG.nextInt(radius * 2 + 1) - radius;
            BlockPos p = center.offset(ox, oy, oz);
            BlockState s = level.getBlockState(p);
            if (s.isAir() || s.liquid() || s.getBlock() == Blocks.BEDROCK) continue;
            return p;
        }
        return null;
    }

    private static BlockPos findNearbyFloorPos(ServerLevel level, BlockPos center, int radius) {
        for (int i = 0; i < 12; i++) {
            int ox = RNG.nextInt(radius * 2 + 1) - radius;
            int oz = RNG.nextInt(radius * 2 + 1) - radius;
            for (int dy = 3; dy >= -3; dy--) {
                BlockPos gp = center.offset(ox, dy, oz);
                BlockState gs = level.getBlockState(gp);
                if (gs.isAir() || gs.liquid() || gs.getBlock() == Blocks.BEDROCK) continue;
                if (level.getBlockState(gp.above()).isAir()) return gp;
                break;
            }
        }
        return null;
    }

    private static void scheduleMiningSequence(ServerPlayer player, ServerLevel level, BlockPos pos, long now) {
        BlockState state = level.getBlockState(pos);
        var st = state.getSoundType();
        int hits = 3 + RNG.nextInt(4);
        long t = now + 1;
        for (int i = 0; i < hits; i++) {
            SOUND_QUEUE.add(new ScheduledSound(t, st.getHitSound(),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    0.45f + RNG.nextFloat() * 0.25f, 0.85f + RNG.nextFloat() * 0.3f, player.getUUID()));
            t += 4 + RNG.nextInt(4);
        }
        SOUND_QUEUE.add(new ScheduledSound(t, st.getBreakSound(),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                0.7f, 0.9f + RNG.nextFloat() * 0.2f, player.getUUID()));
        LOGGER.debug("[DarkArea] mining sequence at {} ({}x {} then break)", pos, hits, st.getHitSound().getLocation());
    }

    private static void scheduleFootstepSequence(ServerPlayer player, ServerLevel level, BlockPos startFloor, long now) {
        BlockState state = level.getBlockState(startFloor);
        var st = state.getSoundType();
        int steps = 4 + RNG.nextInt(5);
        double dirX = RNG.nextDouble() - 0.5;
        double dirZ = RNG.nextDouble() - 0.5;
        double len = Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (len < 0.01) { dirX = 1; dirZ = 0; len = 1; }
        dirX /= len;
        dirZ /= len;
        long t = now + 1;
        for (int i = 0; i < steps; i++) {
            double sx = startFloor.getX() + 0.5 + dirX * i;
            double sz = startFloor.getZ() + 0.5 + dirZ * i;
            SOUND_QUEUE.add(new ScheduledSound(t, st.getStepSound(),
                    sx, startFloor.getY() + 1, sz,
                    0.35f + RNG.nextFloat() * 0.2f, 0.9f + RNG.nextFloat() * 0.2f, player.getUUID()));
            t += 6 + RNG.nextInt(3);
        }
        LOGGER.debug("[DarkArea] footstep sequence from {} ({} steps on {})", startFloor, steps, st.getStepSound().getLocation());
    }

    public static void forceDarkArea(ServerPlayer player) {
        if (player.connection == null) return;
        LOGGER.info("[DarkArea] {} forced dark area event", player.getName().getString());
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            net.minecraft.core.Holder.direct(ModSounds.LOWFREQ.get()), SoundSource.MASTER,
            player.getX(), player.getY(), player.getZ(), 0.5f, 1.0f, 0));
        double ox = (RNG.nextDouble() - 0.5) * 12;
        double oz = (RNG.nextDouble() - 0.5) * 12;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            net.minecraft.core.Holder.direct(ModSounds.THUMP.get()), SoundSource.MASTER,
            player.getX() + ox, player.getY(), player.getZ() + oz,
            0.8f, 0.5f + RNG.nextFloat() * 0.5f, 0));
        ServerLevel level = (ServerLevel) player.level();
        BlockPos breakPos = findNearbyBlockPos(level, player.blockPosition(), 10);
        if (breakPos == null) breakPos = player.blockPosition().below();
        scheduleMiningSequence(player, level, breakPos, level.getGameTime());
        BlockPos floorPos = findNearbyFloorPos(level, player.blockPosition(), 12);
        if (floorPos == null) floorPos = player.blockPosition().below();
        scheduleFootstepSequence(player, level, floorPos, level.getGameTime());
    }
}