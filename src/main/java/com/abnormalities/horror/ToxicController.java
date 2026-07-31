package com.abnormalities.horror;

import com.abnormalities.AbnormalitiesMod;
import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.network.ToxicPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

public class ToxicController {
    private static final Map<UUID, ToxicState> ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Long> LAST_NUR_SPAWN = new HashMap<>();
    private static class ToxicState {
        int ticksLeft;
        int rewindTicks;
        List<double[]> posHistory = new ArrayList<>();
        Map<BlockPos, BlockState> changedBlocks = new HashMap<>();
        List<MobKill> kills = new ArrayList<>();
        List<ItemStack[]> invSnapshots = new ArrayList<>();
    }

    private static class MobKill {
        EntityType<?> type;
        double x, y, z;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        var overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        if (!AbnormalitiesConfig.TOXIC_ENABLED.get()) return;
        long currentDay = overworld.getDayTime() / 24000L;
        if (currentDay < AbnormalitiesConfig.GRACE_PERIOD_DAYS.get()) return;

        COOLDOWNS.replaceAll((u, cd) -> cd - 1);
        COOLDOWNS.values().removeIf(cd -> cd <= 0);

        Iterator<Map.Entry<UUID, ToxicState>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            UUID uuid = e.getKey();
            ToxicState s = e.getValue();
            ServerPlayer player = overworld.getServer().getPlayerList().getPlayer(uuid);
            if (player == null || !player.isAlive()) {
                if (player != null && player.connection != null) {
                    AbnormalitiesMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ToxicPacket(ToxicPacket.STATE_END));
                }
                it.remove();
                COOLDOWNS.put(uuid, AbnormalitiesConfig.TOXIC_COOLDOWN.get().longValue());
                continue;
            }
            s.posHistory.add(new double[]{player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()});
            s.invSnapshots.add(snapshotInv(player));
            s.ticksLeft--;
            if (s.ticksLeft <= 0) {
                doRewind(player, s);
                it.remove();
                COOLDOWNS.put(uuid, AbnormalitiesConfig.TOXIC_COOLDOWN.get().longValue());
            }
        }

        if (overworld.getGameTime() % 100 != 0) return;
        for (var sp : new java.util.ArrayList<>(overworld.getServer().getPlayerList().getPlayers())) {
            if (ACTIVE.containsKey(sp.getUUID()) || COOLDOWNS.containsKey(sp.getUUID())) continue;
            int rep = com.abnormalities.ReputationManager.getRep(sp);
            if (rep >= AbnormalitiesConfig.TOXIC_MAX_REP.get()) continue;
            long sinceNur = overworld.getGameTime() - LAST_NUR_SPAWN.getOrDefault(sp.getUUID(), 0L);
            if (sinceNur < AbnormalitiesConfig.TOXIC_NUR_QUIET.get()) continue;
            if (Vr9pController.isActive(sp.getUUID())) continue;
            if (overworld.random.nextInt(AbnormalitiesConfig.TOXIC_CHANCE.get()) != 0) continue;
            startToxic(sp, overworld);
            break;
        }
    }

    public static void onVr9pSuccess(ServerPlayer player) {
        if (!AbnormalitiesConfig.TOXIC_ENABLED.get()) return;
        if (ACTIVE.containsKey(player.getUUID()) || COOLDOWNS.containsKey(player.getUUID())) return;
        int rep = com.abnormalities.ReputationManager.getRep(player);
        if (rep >= AbnormalitiesConfig.TOXIC_MAX_REP.get()) return;
        if (player.getRandom().nextInt(2) != 0) return;
        startToxic(player, (ServerLevel) player.level());
    }

    public static void recordNurSpawn(ServerPlayer player) {
        LAST_NUR_SPAWN.put(player.getUUID(), player.level().getGameTime());
    }

    private static void startToxic(ServerPlayer player, ServerLevel level) {
        ToxicState s = new ToxicState();
        s.ticksLeft = 60;
        s.rewindTicks = 100 + level.random.nextInt(400);
        ACTIVE.put(player.getUUID(), s);
        AbnormalitiesMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ToxicPacket(ToxicPacket.STATE_START));
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            net.minecraft.core.Holder.direct(com.abnormalities.registry.ModSounds.NUR_SOUND.get()),
            net.minecraft.sounds.SoundSource.MASTER, player.getX(), player.getY(), player.getZ(), 8.0f, 0.4f, 0));
    }

    private static ItemStack[] snapshotInv(Player player) {
        ItemStack[] snap = new ItemStack[player.getInventory().getContainerSize()];
        for (int i = 0; i < snap.length; i++) {
            ItemStack s = player.getInventory().getItem(i);
            snap[i] = s.isEmpty() ? ItemStack.EMPTY : s.copy();
        }
        return snap;
    }

    private static BlockPos findSafeSpot(ServerLevel level, BlockPos pos) {
        for (int dy = 0; dy <= 3; dy++) {
            BlockPos p = pos.above(dy);
            if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()) return p;
        }
        int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
        return new BlockPos(pos.getX(), top, pos.getZ());
    }

    private static void restoreInv(Player player, ItemStack[] snap) {
        player.getInventory().clearContent();
        for (int i = 0; i < snap.length && i < player.getInventory().getContainerSize(); i++) {
            player.getInventory().setItem(i, snap[i].copy());
        }
    }

    private static void doRewind(ServerPlayer player, ToxicState s) {
        if (!s.posHistory.isEmpty()) {
            double[] p = s.posHistory.get(s.posHistory.size() >= s.rewindTicks ? s.posHistory.size() - s.rewindTicks : 0);
            var level = (ServerLevel) player.level();
            BlockPos tp = new BlockPos((int) Math.floor(p[0]), (int) Math.floor(p[1]), (int) Math.floor(p[2]));
            BlockPos safe = findSafeSpot(level, tp);
            player.teleportTo(level, safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5, (float) p[3], (float) p[4]);
        }
        for (var entry : s.changedBlocks.entrySet()) {
            if (entry.getValue() == null) {
                player.level().setBlock(entry.getKey(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            } else {
                player.level().setBlock(entry.getKey(), entry.getValue(), 2);
            }
        }
        for (MobKill k : s.kills) {
            try {
                var mob = k.type.create(player.level());
                if (mob instanceof Mob m) {
                    m.moveTo(k.x + 0.5, k.y, k.z + 0.5, 0, 0);
                    player.level().addFreshEntity(m);
                }
            } catch (Exception ignored) {}
        }
        if (!s.invSnapshots.isEmpty()) {
            restoreInv(player, s.invSnapshots.get(0));
        }
        AbnormalitiesMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ToxicPacket(ToxicPacket.STATE_END));
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            net.minecraft.core.Holder.direct(com.abnormalities.registry.ModSounds.WHISPER_SOUND.get()),
            net.minecraft.sounds.SoundSource.MASTER, player.getX(), player.getY(), player.getZ(), 4.0f, 0.3f, 0));
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() == null) return;
        ToxicState s = ACTIVE.get(event.getPlayer().getUUID());
        if (s == null) return;
        if (event.getLevel().getBlockEntity(event.getPos()) != null) return;
        BlockState before = event.getLevel().getBlockState(event.getPos());
        s.changedBlocks.putIfAbsent(event.getPos().immutable(), before);
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player) {
            ToxicState s = ACTIVE.get(player.getUUID());
            if (s == null) return;
            s.changedBlocks.putIfAbsent(event.getPos().immutable(), null);
        }
    }

    @SubscribeEvent
    public static void onMobKill(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        ToxicState s = ACTIVE.get(player.getUUID());
        if (s == null) return;
        if (!(event.getEntity() instanceof Mob)) return;
        MobKill k = new MobKill();
        k.type = event.getEntity().getType();
        k.x = event.getEntity().getX();
        k.y = event.getEntity().getY();
        k.z = event.getEntity().getZ();
        s.kills.add(k);
    }

    public static void forceStart(ServerPlayer player) {
        if (ACTIVE.containsKey(player.getUUID())) return;
        startToxic(player, (ServerLevel) player.level());
    }
}
