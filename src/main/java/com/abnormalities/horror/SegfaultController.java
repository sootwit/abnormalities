package com.abnormalities.horror;

import com.abnormalities.AbnormalitiesMod;
import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.network.SegfaultPacket;
import com.abnormalities.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SegfaultController {
    private static final Map<UUID, SegfaultState> ACTIVE = new HashMap<>();
    private static final Map<UUID, Integer> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, PendingStart> PENDING_STARTS = new HashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|Segfault");

    private static class PendingStart {
        int ticksLeft;
        boolean stargazed;
        PendingStart(int ticksLeft, boolean stargazed) {
            this.ticksLeft = ticksLeft;
            this.stargazed = stargazed;
        }
    }

    private static class SegfaultState {
        int ticks = 0;
        boolean showingStop = true;
        int nextSwitchAt = 999;
        int totalTicks = 0;
        double lastX, lastZ;
        int graceTicks = 0;
        int startupTicks = 3;
        int wrongTicks = 0;
        int nextAmbienceAt = 100;
        boolean stargazed = false;
        int endPhaseTicks = -1;
        int punishTicks = -1;
        float lastYRot, lastXRot;
        int lastSlot = 0;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        var overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        var players = new java.util.ArrayList<>(overworld.getServer().getPlayerList().getPlayers());
        for (ServerPlayer p : players) {
            tickPlayer(p);
        }

        Iterator<Map.Entry<UUID, PendingStart>> pit = PENDING_STARTS.entrySet().iterator();
        while (pit.hasNext()) {
            var e = pit.next();
            ServerPlayer sp = overworld.getServer().getPlayerList().getPlayer(e.getKey());
            if (sp == null || ACTIVE.containsKey(e.getKey())) {
                pit.remove();
                continue;
            }
            e.getValue().ticksLeft--;
            if (e.getValue().ticksLeft <= 0) {
                startSegfault(sp, e.getValue().stargazed);
                pit.remove();
            }
        }

        COOLDOWNS.replaceAll((u, cd) -> cd - 1);
        COOLDOWNS.values().removeIf(cd -> cd <= 0);

        if (!com.abnormalities.config.AbnormalitiesConfig.SEGFAULT_ENABLED.get()) return;

        long currentDay = overworld.getDayTime() / 24000L;
        if (currentDay < AbnormalitiesConfig.GRACE_PERIOD_DAYS.get()) return;
        if (overworld.getGameTime() % 20 != 0) return;

        if (overworld.random.nextInt(com.abnormalities.entity.HimTracker.weighted(AbnormalitiesConfig.SEGFAULT_SPAWN_WEIGHT.get())) != 0) {
            if (AbnormalitiesConfig.SEGFAULT_STARGAZED_ENABLED.get()
                    && overworld.random.nextInt(com.abnormalities.entity.HimTracker.weighted(AbnormalitiesConfig.SEGFAULT_STARGAZED_SPAWN_WEIGHT.get())) == 0) {
                for (ServerPlayer sp : overworld.players()) {
                    if (ACTIVE.containsKey(sp.getUUID()) || COOLDOWNS.containsKey(sp.getUUID()) || PENDING_STARTS.containsKey(sp.getUUID())) continue;
                    if (overworld.random.nextInt(2) != 0) continue;
                    PENDING_STARTS.put(sp.getUUID(), new PendingStart(100, true));
                    break;
                }
            }
            return;
        }
        for (ServerPlayer sp : overworld.players()) {
            if (ACTIVE.containsKey(sp.getUUID()) || COOLDOWNS.containsKey(sp.getUUID()) || PENDING_STARTS.containsKey(sp.getUUID())) continue;
            if (overworld.random.nextInt(3) != 0) continue;
            PENDING_STARTS.put(sp.getUUID(), new PendingStart(200, false));
            break;
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() != null) {
            UUID uuid = event.getEntity().getUUID();
            ACTIVE.remove(uuid);
            COOLDOWNS.remove(uuid);
            PENDING_STARTS.remove(uuid);
        }
    }

    private static void startSegfault(ServerPlayer player) {
        startSegfault(player, false);
    }

    private static void startSegfault(ServerPlayer player, boolean stargazed) {
        var st = new SegfaultState();
        st.lastX = player.getX();
        st.lastZ = player.getZ();
        st.lastYRot = player.getYRot();
        st.lastXRot = player.getXRot();
        st.lastSlot = player.getInventory().selected;
        st.stargazed = stargazed;
        if (stargazed) {
            st.graceTicks = AbnormalitiesConfig.SEGFAULT_STARGAZED_GRACE_TICKS.get();
            st.startupTicks = 10;
            st.nextSwitchAt = AbnormalitiesConfig.SEGFAULT_STARGAZED_SWITCH_TICKS.get();
            st.showingStop = player.getRandom().nextBoolean();
        } else {
            st.graceTicks = AbnormalitiesConfig.SEGFAULT_GRACE_TICKS.get();
            st.startupTicks = 20;
            st.showingStop = player.getRandom().nextBoolean();
            int swMin = AbnormalitiesConfig.SEGFAULT_SWITCH_MIN.get();
            int swMax = AbnormalitiesConfig.SEGFAULT_SWITCH_MAX.get();
            st.nextSwitchAt = swMin + player.getRandom().nextInt(Math.max(1, swMax - swMin + 1));
        }
        ACTIVE.put(player.getUUID(), st);
        sendState(player, 2, 0);
        playAmbience(player);
        LOGGER.info("{} started ({}) first state: {}", player.getName().getString(), stargazed ? "STARGAZED" : "normal", st.showingStop ? "STOP" : "CONTINUE");
    }

    private static void playAmbience(ServerPlayer player) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            net.minecraft.core.Holder.direct(ModSounds.SEGFAULT_AMBIENCE.get()),
            SoundSource.MASTER, player.getX(), player.getY(), player.getZ(), 2.0f, 1.0f, 0));
    }

    private static void stopAmbience(ServerPlayer player) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(
            ModSounds.SEGFAULT_AMBIENCE.get().getLocation(), SoundSource.MASTER));
    }

    private static void playStop(ServerPlayer player) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            net.minecraft.core.Holder.direct(ModSounds.SEGFAULT_STOP.get()),
            SoundSource.MASTER, player.getX(), player.getY(), player.getZ(), 2.0f, 1.0f, 0));
    }

    private static void playContinue(ServerPlayer player) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            net.minecraft.core.Holder.direct(ModSounds.SEGFAULT_CONTINUE.get()),
            SoundSource.MASTER, player.getX(), player.getY(), player.getZ(), 2.0f, 1.0f, 0));
    }

    private static void showState(ServerPlayer player, SegfaultState state) {
        if (state.showingStop) {
            sendState(player, state.stargazed ? SegfaultPacket.STATE_STARGAZED_STOP : SegfaultPacket.STATE_STOP, 0);
            playStop(player);
        } else {
            sendState(player, state.stargazed ? SegfaultPacket.STATE_STARGAZED_CONTINUE : SegfaultPacket.STATE_CONTINUE, 0);
            playContinue(player);
        }
        com.abnormalities.hexnil.ScreenShakeManager.sendShake(player, 0.8f, 15);
    }

    private static int getDuration(SegfaultState state) {
        return state.stargazed ? AbnormalitiesConfig.SEGFAULT_STARGAZED_DURATION.get() : AbnormalitiesConfig.SEGFAULT_EVENT_DURATION.get();
    }

    public static void tickPlayer(ServerPlayer player) {
        UUID uuid = player.getUUID();
        SegfaultState state = ACTIVE.get(uuid);
        if (state == null) return;
        if (state.endPhaseTicks >= 0) {
            if (state.endPhaseTicks > 0) state.endPhaseTicks--;
            if (state.endPhaseTicks == 0) {
                stopAmbience(player);
                sendState(player, -1, 0);
                ACTIVE.remove(uuid);
                COOLDOWNS.put(uuid, AbnormalitiesConfig.SEGFAULT_COOLDOWN_TICKS.get());
                LOGGER.info("{} segfault ended", player.getName().getString());
            }
            return;
        }
        if (state.punishTicks >= 0) {
            if (state.punishTicks > 0) state.punishTicks--;
            if (state.punishTicks == 0) doPunish(player, uuid, state);
            return;
        }
        state.totalTicks++;

        if (state.startupTicks > 0) {
            state.startupTicks--;
            if (state.startupTicks == 0) showState(player, state);
            return;
        }

        if (state.totalTicks >= getDuration(state)) {
            sendState(player, 2, 0);
            state.endPhaseTicks = 5;
            LOGGER.info("{} segfault ending (segfaulthit 5 ticks)", player.getName().getString());
            return;
        }

        if (state.totalTicks < getDuration(state) && state.totalTicks >= state.nextAmbienceAt) {
            playAmbience(player);
            state.nextAmbienceAt = state.totalTicks + 100;
        }

        if (state.graceTicks > 0) {
            state.graceTicks--;
            if (state.graceTicks == 0) state.ticks = 0;
            return;
        }

        state.ticks++;

        double dx = player.getX() - state.lastX;
        double dz = player.getZ() - state.lastZ;
        double moveThreshold = AbnormalitiesConfig.SEGFAULT_MOVE_THRESHOLD.get();
        boolean playerMoved = Math.sqrt(dx * dx + dz * dz) > moveThreshold;
        if (Math.sqrt(dx * dx + dz * dz) > 10) {
            state.lastX = player.getX();
            state.lastZ = player.getZ();
            return;
        }
        state.lastX = player.getX();
        state.lastZ = player.getZ();
        float yawDelta = Math.abs(player.getYRot() - state.lastYRot);
        float pitchDelta = Math.abs(player.getXRot() - state.lastXRot);
        int curSlot = player.getInventory().selected;
        boolean slotChanged = curSlot != state.lastSlot;
        state.lastYRot = player.getYRot();
        state.lastXRot = player.getXRot();
        state.lastSlot = curSlot;

        if (state.showingStop) {
            if (playerMoved) addWrong(player, uuid, state);
            if (yawDelta > 1.0f || pitchDelta > 1.0f) strictViolation(player);
            if (slotChanged) strictViolation(player);
            if (player.containerMenu != player.inventoryMenu) strictViolation(player);
            if (player.swinging) strictViolation(player);
        } else if (!playerMoved) {
            addWrong(player, uuid, state);
        } else {
            if (state.wrongTicks > 0) state.wrongTicks = 0;
        }

        if (state.ticks >= state.nextSwitchAt) {
            state.showingStop = player.getRandom().nextBoolean();
            if (state.stargazed) {
                state.nextSwitchAt = AbnormalitiesConfig.SEGFAULT_STARGAZED_SWITCH_TICKS.get();
                state.graceTicks = AbnormalitiesConfig.SEGFAULT_STARGAZED_GRACE_TICKS.get();
            } else {
                int swMin = AbnormalitiesConfig.SEGFAULT_SWITCH_MIN.get();
                int swMax = AbnormalitiesConfig.SEGFAULT_SWITCH_MAX.get();
                state.nextSwitchAt = swMin + player.getRandom().nextInt(Math.max(1, swMax - swMin + 1));
                state.graceTicks = AbnormalitiesConfig.SEGFAULT_GRACE_TICKS.get();
            }
            state.ticks = 0;
            state.wrongTicks = 0;
            showState(player, state);
        }
    }

    private static void addWrong(ServerPlayer player, UUID uuid, SegfaultState state) {
        state.wrongTicks++;
        int threshold = state.stargazed ? AbnormalitiesConfig.SEGFAULT_STARGAZED_WRONG_THRESHOLD.get() : AbnormalitiesConfig.SEGFAULT_WRONG_THRESHOLD.get();
        if (state.wrongTicks >= threshold) startPunish(player, uuid, state);
    }

    public static void strictViolation(ServerPlayer player) {
        UUID uuid = player.getUUID();
        SegfaultState state = ACTIVE.get(uuid);
        if (state == null || !state.stargazed || !state.showingStop) return;
        if (state.graceTicks > 0) return;
        if (state.punishTicks >= 0 || state.endPhaseTicks >= 0) return;
        startPunish(player, uuid, state);
    }

    private static void startPunish(ServerPlayer player, UUID uuid, SegfaultState state) {
        if (state.punishTicks >= 0 || state.endPhaseTicks >= 0) return;
        state.punishTicks = 10;
        COOLDOWNS.put(uuid, AbnormalitiesConfig.SEGFAULT_COOLDOWN_TICKS.get());
        LOGGER.info("{} PUNISHED (was {}, {})", player.getName().getString(), state.showingStop ? "STOP" : "CONTINUE", state.stargazed ? "STARGAZED" : "normal");
        sendState(player, 2, 0);
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            net.minecraft.core.Holder.direct(ModSounds.NUR_SOUND.get()),
            SoundSource.MASTER, player.getX(), player.getY(), player.getZ(), 10.0f, 1.0f, 0));
        float dmg = AbnormalitiesConfig.SEGFAULT_DAMAGE.get().floatValue();
        player.hurt(player.damageSources().genericKill(), dmg);
    }

    private static void doPunish(ServerPlayer player, UUID uuid, SegfaultState state) {
        stopAmbience(player);
        ACTIVE.remove(uuid);
        var mode = state.stargazed ? AbnormalitiesConfig.SEGFAULT_STARGAZED_PUNISH.get() : AbnormalitiesConfig.SEGFAULT_PUNISH.get();
        if (mode == AbnormalitiesConfig.PunishMode.CRASH) {
            AbnormalitiesMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new com.abnormalities.network.CrashPacket());
        } else if (mode == AbnormalitiesConfig.PunishMode.KICK) {
            if (player.connection != null) {
                sendState(player, -1, 0);
                player.connection.disconnect(Component.literal(state.stargazed ? "0x0000.STARGAZED: Fatal Error" : "SEGFAULT at 0x0000: Memory access violation"));
            }
        } else {
            sendState(player, -1, 0);
        }
    }

    private static void sendState(ServerPlayer player, int state, int duration) {
        AbnormalitiesMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SegfaultPacket(state, duration));
    }

    public static void forceStart(ServerPlayer player) {
        if (ACTIVE.containsKey(player.getUUID())) return;
        startSegfault(player);
    }

    public static void forceStartStargazed(ServerPlayer player) {
        if (ACTIVE.containsKey(player.getUUID())) return;
        startSegfault(player, true);
    }

    public static void escViolation(ServerPlayer player) {
        strictViolation(player);
    }

    public static void cleanup(UUID uuid) {
        ACTIVE.remove(uuid);
        COOLDOWNS.put(uuid, AbnormalitiesConfig.SEGFAULT_COOLDOWN_TICKS.get());
    }

    public static boolean isActive(UUID uuid) {
        return ACTIVE.containsKey(uuid);
    }
}
