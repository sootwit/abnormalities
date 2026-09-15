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
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class FriendActionTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|FriendActionTracker");
    private static final int CHAT_DELAY = 600;
    private static final int FORCED_SPAWN_DELAY = 300;
    private static final int SPAWN_DISTANCE = 64;
    private static final int POST_SPAWN_COOLDOWN = 4800;
    /* private static final int MAX_CLONES = 3; */ // old 

    private static final Map<UUID, List<FriendEntity>> ACTIVE_CLONES = new HashMap<>();
    private static final Map<UUID, Integer> SPAWN_TIMERS = new HashMap<>();
    private static final Map<UUID, Boolean> MESSAGES_SENT = new HashMap<>();
    private static final Map<UUID, Boolean> FORCED_SPAWNS = new HashMap<>();
    private static final Map<UUID, List<FriendEntity.FriendAction>> ACTION_LOGS = new HashMap<>();
    private static final Map<UUID, Deque<double[]>> POSITION_BUFFERS = new HashMap<>();
    private static final Map<UUID, Integer> SPAWN_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Integer> FOLLOW_TIMES = new HashMap<>();
    private static final Map<UUID, Integer> SPAWN_TICKS = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        ACTIVE_CLONES.values().forEach(list -> list.removeIf(e -> !e.isAlive()));

        for (Player player : overworld.players()) {
            UUID uuid = player.getUUID();

            boolean tracked = SPAWN_TIMERS.containsKey(uuid) || ACTIVE_CLONES.containsKey(uuid);

            if (tracked) {
                Deque<double[]> posBuf = POSITION_BUFFERS.computeIfAbsent(uuid, k -> new ArrayDeque<>());
                posBuf.addLast(new double[]{player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(), player.getAbilities().flying ? 1 : 0, player.getHealth(), player.getMaxHealth()});
                int followTicks = FOLLOW_TIMES.getOrDefault(uuid, 140);
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
                FORCED_SPAWNS.remove(uuid);
                if (SPAWN_TIMERS.containsKey(uuid)) {
                    SPAWN_TIMERS.remove(uuid);
                    MESSAGES_SENT.remove(uuid);
                    FOLLOW_TIMES.remove(uuid);
                    SPAWN_TICKS.remove(uuid);
                }
                continue;
            }

            if (!SPAWN_TIMERS.containsKey(uuid)) {
                int friendWeight = AbnormalitiesConfig.FRIEND_SPAWN_WEIGHT.get();
                if (player.level().dimension() == com.abnormalities.sign.SignDimension.LEVEL_KEY) friendWeight = Math.max(1, friendWeight / 2);
                if (player.tickCount % 40 == 0 && AbnormalitiesConfig.FRIEND_ENABLED.get() && overworld.random.nextInt(com.abnormalities.entity.HimTracker.weighted(friendWeight)) == 0 && overworld.isNight()) {
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

            int spawnTick = SPAWN_TICKS.getOrDefault(uuid, 0);
            spawnTick++;
            SPAWN_TICKS.put(uuid, spawnTick);

            boolean forced = FORCED_SPAWNS.getOrDefault(uuid, false);
            int spawnAt = forced ? FORCED_SPAWN_DELAY : CHAT_DELAY;
            int totalDelay = spawnAt + FOLLOW_TIMES.getOrDefault(uuid, 140);

            if (!MESSAGES_SENT.getOrDefault(uuid, false) && timer >= spawnAt) {
                var server = ServerLifecycleHooks.getCurrentServer();
                if (server != null) {
                    for (var p : server.getPlayerList().getPlayers()) {
                        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSystemChatPacket(
                                Component.literal("<" + player.getName().getString() + "> run").withStyle(ChatFormatting.WHITE), false));
                    }
                }
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.AMBIENT_CAVE.get(), SoundSource.MASTER, 4.0f, 0.5f);
                MESSAGES_SENT.put(uuid, true);
            }

            if (timer >= totalDelay) {
                if (spawnClone(player)) {
                    SPAWN_TIMERS.put(uuid, 0);
                    MESSAGES_SENT.put(uuid, false);
                } else {
                    SPAWN_COOLDOWNS.put(uuid, 200);
                }
            }
        }

        long now = ServerLifecycleHooks.getCurrentServer().getTickCount();
        Iterator<Map.Entry<UUID, List<String[]>>> eit = PENDING_ECHOES.entrySet().iterator();
        while (eit.hasNext()) {
            var entry = eit.next();
            UUID euuid = entry.getKey();
            List<String[]> echoes = entry.getValue();
            Iterator<String[]> ei = echoes.iterator();
            while (ei.hasNext()) {
                String[] echo = ei.next();
                long targetTick = Long.parseLong(echo[1]);
                if (now >= targetTick) {
                    ei.remove();
                    ServerPlayer esp = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(euuid);
                    if (esp != null && esp.connection != null) {
                        esp.connection.send(new net.minecraft.network.protocol.game.ClientboundSystemChatPacket(
                            Component.literal("<" + esp.getName().getString() + "> " + echo[0]), false));
                    }
                    CHAT_COOLDOWN.remove(euuid);
                }
            }
            if (echoes.isEmpty()) eit.remove();
        }
    }

    public static boolean hasActiveClone(UUID uuid) {
        return SPAWN_TIMERS.containsKey(uuid) || ACTIVE_CLONES.containsKey(uuid);
    }

    public static boolean forceFriendSpawn(Player player) {
        UUID uuid = player.getUUID();
        List<FriendEntity> existing = ACTIVE_CLONES.getOrDefault(uuid, Collections.emptyList());
        if (existing.size() >= 2) {
            for (FriendEntity old : existing) {
                if (old.isAlive()) old.discard();
            }
        }
        SPAWN_COOLDOWNS.remove(uuid);
        SPAWN_TIMERS.put(uuid, 0);
        MESSAGES_SENT.put(uuid, true);
        FORCED_SPAWNS.put(uuid, true);
        ACTION_LOGS.put(uuid, new ArrayList<>());
        LOGGER.info("[FriendActionTracker] forced spawn for {}", player.getName().getString());
        if (player instanceof ServerPlayer) {
            var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                for (var p : server.getPlayerList().getPlayers()) {
                            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSystemChatPacket(
                                    net.minecraft.network.chat.Component.literal("<" + player.getName().getString() + "> run").withStyle(net.minecraft.ChatFormatting.WHITE), false));
                }
            }
        }
        return true;
    }

    public static double[] getDelayedPosition(Player player) {
        UUID uuid = player.getUUID();
        Deque<double[]> buf = POSITION_BUFFERS.get(uuid);
        if (buf == null || buf.isEmpty()) return null;
        int followTicks = FOLLOW_TIMES.getOrDefault(uuid, 100);
        if (buf.size() < followTicks) return null;
        int spawnTick = SPAWN_TICKS.getOrDefault(uuid, 0);
        int elapsed = Math.max(0, spawnTick - followTicks);
        double[][] arr = buf.toArray(new double[0][]);
        int target = Math.min(elapsed, arr.length - 1);
        target = Math.max(0, target);
        double[] pt = arr[target];
        LOGGER.debug("[FriendActionTracker] delayed position for {} buffer={} target={}", player.getName().getString(), buf.size(), target);
        return new double[]{pt[0], pt[1], pt[2], pt[5]};
    }

    public static double[] getDelayedHealth(Player player) {
        UUID uuid = player.getUUID();
        Deque<double[]> buf = POSITION_BUFFERS.get(uuid);
        if (buf == null || buf.isEmpty()) return null;
        int followTicks = FOLLOW_TIMES.getOrDefault(uuid, 100);
        if (buf.size() < followTicks) return null;
        int spawnTick = SPAWN_TICKS.getOrDefault(uuid, 0);
        int elapsed = Math.max(0, spawnTick - followTicks);
        double[][] arr = buf.toArray(new double[0][]);
        int target = Math.min(elapsed, arr.length - 1);
        target = Math.max(0, target);
        double[] pt = arr[target];
        return new double[]{pt[6], pt[7]};
    }

    public static int getFollowTime(UUID uuid) {
        return FOLLOW_TIMES.getOrDefault(uuid, 140);
    }

    private static void startSpawnSequence(Player player) {
        UUID uuid = player.getUUID();
        SPAWN_TIMERS.put(uuid, 0);
        MESSAGES_SENT.put(uuid, false);
        ACTION_LOGS.put(uuid, new ArrayList<>());
        FOLLOW_TIMES.put(uuid, 40 + player.getRandom().nextInt(121));
        SPAWN_TICKS.put(uuid, 0);
        LOGGER.info("[FriendActionTracker] spawn sequence started for {}", player.getName().getString());
    }

    private static boolean spawnClone(Player player) {
        UUID uuid = player.getUUID();
        ServerLevel level = (ServerLevel) player.level();

        Deque<double[]> posBuf = POSITION_BUFFERS.getOrDefault(uuid, new ArrayDeque<>());
        if (posBuf.isEmpty()) {
            cleanup(uuid);
            return false;
        }

        List<FriendEntity> existing = ACTIVE_CLONES.getOrDefault(uuid, Collections.emptyList());
        if (existing.size() >= 2) {
            for (FriendEntity old : existing) {
                if (old.isAlive()) old.discard();
            }
        }

        FriendEntity clone = ModEntities.FRIEND.get().create(level);
        if (clone == null) {
            cleanup(uuid);
            return false;
        }

        clone.setTargetPlayer(player);

        List<double[]> path = new ArrayList<>(posBuf);
        clone.setInitialPath(path);
        clone.initTimers();

        List<FriendEntity.FriendAction> actions = ACTION_LOGS.getOrDefault(uuid, new ArrayList<>());
        clone.setInitialActions(actions);

        level.addFreshEntity(clone);
        ACTIVE_CLONES.computeIfAbsent(uuid, k -> new ArrayList<>()).add(clone);
        ACTION_LOGS.put(uuid, new ArrayList<>());
        SPAWN_COOLDOWNS.put(uuid, POST_SPAWN_COOLDOWN);
        LOGGER.info("[FriendActionTracker] clone spawned for {} at {} {} {}", player.getName().getString(), (int)player.getX(), (int)player.getY(), (int)player.getZ());

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.AMBIENT_CAVE.get(), SoundSource.MASTER, 5.0f, 0.3f);
        return true;
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!AbnormalitiesConfig.FRIEND_BREAK_BLOCKS.get()) return;
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        UUID uuid = player.getUUID();
        if (!SPAWN_TIMERS.containsKey(uuid) && !ACTIVE_CLONES.containsKey(uuid)) return;

        BlockPos pos = event.getPos();
        var state = event.getState();

        List<FriendEntity.FriendAction> log = ACTION_LOGS.computeIfAbsent(uuid, k -> new ArrayList<>());
        log.add(new FriendEntity.FriendAction(FriendEntity.FriendAction.ActionType.BREAK, pos.getX(), pos.getY(), pos.getZ(), state));
        LOGGER.debug("[FriendActionTracker] recorded break at {} {} {} for {}", pos.getX(), pos.getY(), pos.getZ(), player.getName().getString());

        List<FriendEntity> clones = ACTIVE_CLONES.getOrDefault(uuid, Collections.emptyList());
        for (FriendEntity clone : clones) {
            if (clone.isAlive()) {
                clone.recordBlockBreak(player, pos, state);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!AbnormalitiesConfig.FRIEND_PLACE_BLOCKS.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID uuid = player.getUUID();
        if (!SPAWN_TIMERS.containsKey(uuid) && !ACTIVE_CLONES.containsKey(uuid)) return;

        BlockPos pos = event.getPos();
        var state = event.getState();

        List<FriendEntity.FriendAction> log = ACTION_LOGS.computeIfAbsent(uuid, k -> new ArrayList<>());
        log.add(new FriendEntity.FriendAction(FriendEntity.FriendAction.ActionType.PLACE, pos.getX(), pos.getY(), pos.getZ(), state));
        LOGGER.debug("[FriendActionTracker] recorded place at {} {} {} for {}", pos.getX(), pos.getY(), pos.getZ(), player.getName().getString());

        List<FriendEntity> clones = ACTIVE_CLONES.getOrDefault(uuid, Collections.emptyList());
        for (FriendEntity clone : clones) {
            if (clone.isAlive()) {
                clone.recordBlockPlace(player, pos, state);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!AbnormalitiesConfig.FRIEND_KILL_MOBS.get()) return;
        LivingEntity dead = event.getEntity();
        if (dead instanceof Player) return;
        if (dead.level().isClientSide) return;

        Entity source = event.getSource().getEntity();
        if (!(source instanceof ServerPlayer player)) return;
        UUID uuid = player.getUUID();
        if (!SPAWN_TIMERS.containsKey(uuid) && !ACTIVE_CLONES.containsKey(uuid)) return;

        EntityType<?> type = dead.getType();

        List<FriendEntity.FriendAction> log = ACTION_LOGS.computeIfAbsent(uuid, k -> new ArrayList<>());
        log.add(new FriendEntity.FriendAction(FriendEntity.FriendAction.ActionType.KILL, dead.getX(), dead.getY(), dead.getZ(), type));
        LOGGER.debug("[FriendActionTracker] recorded kill {} at {} {} {} for {}", type.getDescriptionId(), (int)dead.getX(), (int)dead.getY(), (int)dead.getZ(), player.getName().getString());

        List<FriendEntity> clones = ACTIVE_CLONES.getOrDefault(uuid, Collections.emptyList());
        for (FriendEntity clone : clones) {
            if (clone.isAlive()) {
                clone.recordMobKill(player, dead.getX(), dead.getY(), dead.getZ(), type);
            }
        }
    }

    private static void cleanup(UUID uuid) {
        LOGGER.debug("[FriendActionTracker] cleanup for {}", uuid);
        List<FriendEntity> clones = ACTIVE_CLONES.remove(uuid);
        if (clones != null) {
            for (FriendEntity e : clones) {
                if (e.isAlive()) e.discard();
            }
        }
        SPAWN_TIMERS.remove(uuid);
        MESSAGES_SENT.remove(uuid);
        FORCED_SPAWNS.remove(uuid);
        ACTION_LOGS.remove(uuid);
        POSITION_BUFFERS.remove(uuid);
        FOLLOW_TIMES.remove(uuid);
        SPAWN_TICKS.remove(uuid);
        SPAWN_COOLDOWNS.remove(uuid);
        CHAT_COOLDOWN.remove(uuid);
        PENDING_ECHOES.remove(uuid);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() != null) {
            cleanup(event.getEntity().getUUID());
        }
    }

    private static final Set<UUID> CHAT_COOLDOWN = new HashSet<>();
    private static final Map<UUID, List<String[]>> PENDING_ECHOES = new HashMap<>();

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        UUID uuid = player.getUUID();
        List<FriendEntity> clones = ACTIVE_CLONES.get(uuid);
        if (clones == null || clones.isEmpty()) return;
        if (clones.stream().noneMatch(FriendEntity::isAlive)) return;
        if (CHAT_COOLDOWN.contains(uuid)) return;
        String msg = event.getMessage().getString();
        int delayTicks = FOLLOW_TIMES.getOrDefault(uuid, 140);
        var server = player.getServer();
        if (server == null) return;
        CHAT_COOLDOWN.add(uuid);
        PENDING_ECHOES.computeIfAbsent(uuid, k -> new ArrayList<>()).add(new String[]{msg, String.valueOf(server.getTickCount() + delayTicks)});
    }
}
