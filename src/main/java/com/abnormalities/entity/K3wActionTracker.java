package com.abnormalities.entity;

import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

public class K3wActionTracker {
    private static final int CHAT_DELAY = 600;
    private static final int FORCED_SPAWN_DELAY = 300;
    private static final int SPAWN_DISTANCE = 64;
    private static final int POST_SPAWN_COOLDOWN = 4800;

    private static final Map<UUID, List<K3wEntity>> ACTIVE_CLONES = new HashMap<>();
    private static final Map<UUID, Integer> SPAWN_TIMERS = new HashMap<>();
    private static final Map<UUID, Boolean> MESSAGES_SENT = new HashMap<>();
    private static final Map<UUID, Boolean> FORCED_SPAWNS = new HashMap<>();
    private static final Map<UUID, List<K3wEntity.K3wAction>> ACTION_LOGS = new HashMap<>();
    private static final Map<UUID, Deque<double[]>> POSITION_BUFFERS = new HashMap<>();
    private static final Map<UUID, Integer> SPAWN_COOLDOWNS = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ServerLevel overworld = ServerLifecycleHooks.getCurrentServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        ACTIVE_CLONES.values().forEach(list -> list.removeIf(e -> !e.isAlive()));

        for (Player player : overworld.players()) {
            UUID uuid = player.getUUID();

            boolean tracked = SPAWN_TIMERS.containsKey(uuid) || ACTIVE_CLONES.containsKey(uuid);

            if (tracked) {
                Deque<double[]> posBuf = POSITION_BUFFERS.computeIfAbsent(uuid, k -> new ArrayDeque<>());
                posBuf.addLast(new double[]{player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(), player.getAbilities().flying ? 1 : 0});
                int followTicks = AbnormalitiesConfig.K3W_FOLLOW_TIME.get() * 20;
                if (posBuf.size() > followTicks + 40) {
                    posBuf.removeFirst();
                }
            }

            int cd = SPAWN_COOLDOWNS.getOrDefault(uuid, 0);
            if (cd > 0) {
                SPAWN_COOLDOWNS.put(uuid, cd - 1);
                continue;
            }
            if (SPAWN_COOLDOWNS.containsKey(uuid) && cd == 0) {
                SPAWN_COOLDOWNS.remove(uuid);
                if (SPAWN_TIMERS.containsKey(uuid)) {
                    cleanup(uuid);
                }
                continue;
            }

            if (!SPAWN_TIMERS.containsKey(uuid)) {
                if (overworld.random.nextInt(AbnormalitiesConfig.K3W_SPAWN_WEIGHT.get()) == 0 && overworld.isNight()) {
                    long currentDay = overworld.getDayTime() / 24000L;
                    if (currentDay >= AbnormalitiesConfig.GRACE_PERIOD_DAYS.get()) {
                        startSpawnSequence(player);
                    }
                }
                continue;
            }

            int timer = SPAWN_TIMERS.get(uuid);
            timer++;
            SPAWN_TIMERS.put(uuid, timer);

            boolean forced = FORCED_SPAWNS.getOrDefault(uuid, false);
            int spawnAt = forced ? FORCED_SPAWN_DELAY : CHAT_DELAY;
            int totalDelay = spawnAt + AbnormalitiesConfig.K3W_FOLLOW_TIME.get() * 20;

            if (!MESSAGES_SENT.getOrDefault(uuid, false) && timer >= spawnAt) {
                if (player instanceof ServerPlayer sp) {
                    sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSystemChatPacket(
                            Component.literal("<" + player.getName().getString() + "> run").withStyle(ChatFormatting.WHITE), false));
                }
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.AMBIENT_CAVE.get(), SoundSource.MASTER, 4.0f, 0.5f);
                MESSAGES_SENT.put(uuid, true);
            }

            if (timer >= totalDelay) {
                if (spawnClone(player)) {
                    SPAWN_TIMERS.put(uuid, 0);
                    MESSAGES_SENT.put(uuid, false);
                }
            }
        }
    }

    public static void forceK3wSpawn(Player player) {
        UUID uuid = player.getUUID();
        SPAWN_COOLDOWNS.remove(uuid);
        SPAWN_TIMERS.put(uuid, 0);
        MESSAGES_SENT.put(uuid, true);
        FORCED_SPAWNS.put(uuid, true);
        ACTION_LOGS.put(uuid, new ArrayList<>());
        POSITION_BUFFERS.put(uuid, new ArrayDeque<>());
        if (player instanceof ServerPlayer sp) {
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSystemChatPacket(
                    net.minecraft.network.chat.Component.literal("<" + player.getName().getString() + "> run").withStyle(net.minecraft.ChatFormatting.WHITE), false));
        }
    }

    public static double[] getDelayedPosition(Player player) {
        UUID uuid = player.getUUID();
        Deque<double[]> buf = POSITION_BUFFERS.get(uuid);
        if (buf == null || buf.isEmpty()) return null;
        int followTicks = AbnormalitiesConfig.K3W_FOLLOW_TIME.get() * 20;
        if (buf.size() < followTicks) return null;
        double[][] arr = buf.toArray(new double[0][]);
        int target = Math.max(0, arr.length - followTicks);
        double[] pt = arr[target];
        return new double[]{pt[0], pt[1], pt[2], pt[5]};
    }

    private static void startSpawnSequence(Player player) {
        UUID uuid = player.getUUID();
        SPAWN_TIMERS.put(uuid, 0);
        MESSAGES_SENT.put(uuid, false);
        ACTION_LOGS.put(uuid, new ArrayList<>());
        POSITION_BUFFERS.put(uuid, new ArrayDeque<>());
    }

    private static boolean spawnClone(Player player) {
        UUID uuid = player.getUUID();
        ServerLevel level = (ServerLevel) player.level();

        Deque<double[]> posBuf = POSITION_BUFFERS.getOrDefault(uuid, new ArrayDeque<>());
        if (posBuf.isEmpty()) {
            cleanup(uuid);
            return false;
        }

        if (ACTIVE_CLONES.getOrDefault(uuid, Collections.emptyList()).size() >= 2) {
            return false;
        }

        K3wEntity clone = ModEntities.K3W.get().create(level);
        if (clone == null) {
            cleanup(uuid);
            return false;
        }

        clone.setTargetPlayer(player);

        List<double[]> path = new ArrayList<>(posBuf);
        clone.setInitialPath(path);

        List<K3wEntity.K3wAction> actions = ACTION_LOGS.getOrDefault(uuid, new ArrayList<>());
        clone.setInitialActions(actions);

        level.addFreshEntity(clone);
        ACTIVE_CLONES.computeIfAbsent(uuid, k -> new ArrayList<>()).add(clone);
        ACTION_LOGS.put(uuid, new ArrayList<>());
        POSITION_BUFFERS.put(uuid, new ArrayDeque<>());
        SPAWN_COOLDOWNS.put(uuid, POST_SPAWN_COOLDOWN);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.AMBIENT_CAVE.get(), SoundSource.MASTER, 5.0f, 0.3f);
        return true;
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!AbnormalitiesConfig.K3W_BREAK_BLOCKS.get()) return;
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        UUID uuid = player.getUUID();
        if (!SPAWN_TIMERS.containsKey(uuid)) return;

        BlockPos pos = event.getPos();
        var state = event.getState();

        List<K3wEntity.K3wAction> log = ACTION_LOGS.computeIfAbsent(uuid, k -> new ArrayList<>());
        log.add(new K3wEntity.K3wAction(K3wEntity.K3wAction.ActionType.BREAK, pos.getX(), pos.getY(), pos.getZ(), state.getBlock()));

        List<K3wEntity> clones = ACTIVE_CLONES.getOrDefault(uuid, Collections.emptyList());
        for (K3wEntity clone : clones) {
            if (clone.isAlive()) {
                clone.recordBlockBreak(player, pos, state);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!AbnormalitiesConfig.K3W_PLACE_BLOCKS.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID uuid = player.getUUID();
        if (!SPAWN_TIMERS.containsKey(uuid)) return;

        BlockPos pos = event.getPos();
        var state = event.getState();

        List<K3wEntity.K3wAction> log = ACTION_LOGS.computeIfAbsent(uuid, k -> new ArrayList<>());
        log.add(new K3wEntity.K3wAction(K3wEntity.K3wAction.ActionType.PLACE, pos.getX(), pos.getY(), pos.getZ(), state.getBlock()));

        List<K3wEntity> clones = ACTIVE_CLONES.getOrDefault(uuid, Collections.emptyList());
        for (K3wEntity clone : clones) {
            if (clone.isAlive()) {
                clone.recordBlockPlace(player, pos, state);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!AbnormalitiesConfig.K3W_KILL_MOBS.get()) return;
        LivingEntity dead = event.getEntity();
        if (dead instanceof Player) return;
        if (dead.level().isClientSide) return;

        Entity source = event.getSource().getEntity();
        if (!(source instanceof ServerPlayer player)) return;
        UUID uuid = player.getUUID();
        if (!SPAWN_TIMERS.containsKey(uuid)) return;

        EntityType<?> type = dead.getType();

        List<K3wEntity.K3wAction> log = ACTION_LOGS.computeIfAbsent(uuid, k -> new ArrayList<>());
        log.add(new K3wEntity.K3wAction(K3wEntity.K3wAction.ActionType.KILL, dead.getX(), dead.getY(), dead.getZ(), type));

        List<K3wEntity> clones = ACTIVE_CLONES.getOrDefault(uuid, Collections.emptyList());
        for (K3wEntity clone : clones) {
            if (clone.isAlive()) {
                clone.recordMobKill(player, dead.getX(), dead.getY(), dead.getZ(), type);
            }
        }
    }

    private static void cleanup(UUID uuid) {
        ACTIVE_CLONES.remove(uuid);
        SPAWN_TIMERS.remove(uuid);
        MESSAGES_SENT.remove(uuid);
        FORCED_SPAWNS.remove(uuid);
        ACTION_LOGS.remove(uuid);
        POSITION_BUFFERS.remove(uuid);
        SPAWN_COOLDOWNS.remove(uuid);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() != null) {
            cleanup(event.getEntity().getUUID());
        }
    }
}
