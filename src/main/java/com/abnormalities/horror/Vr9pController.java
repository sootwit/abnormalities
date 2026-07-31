package com.abnormalities.horror;

import com.abnormalities.AbnormalitiesMod;
import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.network.Vr9pPacket;
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

public class Vr9pController {
    private static final Map<UUID, Vr9pState> ACTIVE = new HashMap<>();
    private static final Map<UUID, Integer> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, PendingStart> PENDING_STARTS = new HashMap<>();
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("vr9p");

    private static class PendingStart {
        int ticksLeft;
        boolean stargazed;
        PendingStart(int ticksLeft, boolean stargazed) {
            this.ticksLeft = ticksLeft;
            this.stargazed = stargazed;
        }
    }

    private static class Vr9pState {
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
                startVr9p(sp, e.getValue().stargazed);
                pit.remove();
            }
        }

        COOLDOWNS.replaceAll((u, cd) -> cd - 1);
        COOLDOWNS.values().removeIf(cd -> cd <= 0);

        long currentDay = overworld.getDayTime() / 24000L;
        if (currentDay < AbnormalitiesConfig.GRACE_PERIOD_DAYS.get()) return;
        if (overworld.getGameTime() % 20 != 0) return;

        if (overworld.random.nextInt(AbnormalitiesConfig.VR9P_SPAWN_WEIGHT.get()) != 0) {
            if (AbnormalitiesConfig.VR9P_STARGAZED_ENABLED.get()
                    && overworld.random.nextInt(AbnormalitiesConfig.VR9P_STARGAZED_SPAWN_WEIGHT.get()) == 0) {
                for (ServerPlayer sp : overworld.players()) {
                    if (ACTIVE.containsKey(sp.getUUID()) || COOLDOWNS.containsKey(sp.getUUID()) || PENDING_STARTS.containsKey(sp.getUUID())) continue;
                    if (overworld.random.nextInt(2) != 0) continue;
                    SisterController.onVr9pWarning(sp, true);
                    PENDING_STARTS.put(sp.getUUID(), new PendingStart(100, true));
                    break;
                }
            }
            return;
        }
        for (ServerPlayer sp : overworld.players()) {
            if (ACTIVE.containsKey(sp.getUUID()) || COOLDOWNS.containsKey(sp.getUUID()) || PENDING_STARTS.containsKey(sp.getUUID())) continue;
            if (overworld.random.nextInt(3) != 0) continue;
            SisterController.onVr9pWarning(sp, false);
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

    private static void startVr9p(ServerPlayer player) {
        startVr9p(player, false);
    }

    private static void startVr9p(ServerPlayer player, boolean stargazed) {
        var st = new Vr9pState();
        st.lastX = player.getX();
        st.lastZ = player.getZ();
        st.lastYRot = player.getYRot();
        st.lastXRot = player.getXRot();
        st.lastSlot = player.getInventory().selected;
        st.stargazed = stargazed;
        if (stargazed) {
            st.graceTicks = AbnormalitiesConfig.VR9P_STARGAZED_GRACE_TICKS.get();
            st.startupTicks = 10;
            st.nextSwitchAt = AbnormalitiesConfig.VR9P_STARGAZED_SWITCH_TICKS.get();
            st.showingStop = player.getRandom().nextBoolean();
        } else {
            st.graceTicks = AbnormalitiesConfig.VR9P_GRACE_TICKS.get();
            st.startupTicks = 20;
            st.showingStop = player.getRandom().nextBoolean();
            int swMin = AbnormalitiesConfig.VR9P_SWITCH_MIN.get();
            int swMax = AbnormalitiesConfig.VR9P_SWITCH_MAX.get();
            st.nextSwitchAt = swMin + player.getRandom().nextInt(Math.max(1, swMax - swMin + 1));
        }
        ACTIVE.put(player.getUUID(), st);
        sendState(player, 2, 0);
        playAmbience(player);
        LOGGER.info("{} started ({}) first state: {}", player.getName().getString(), stargazed ? "STARGAZED" : "normal", st.showingStop ? "STOP" : "CONTINUE");
    }

    private static void playAmbience(ServerPlayer player) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            net.minecraft.core.Holder.direct(ModSounds.VR9P_AMBIENCE.get()),
            SoundSource.MASTER, player.getX(), player.getY(), player.getZ(), 2.0f, 1.0f, 0));
    }

    private static void stopAmbience(ServerPlayer player) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(
            ModSounds.VR9P_AMBIENCE.get().getLocation(), SoundSource.MASTER));
    }

    private static void playStop(ServerPlayer player) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            net.minecraft.core.Holder.direct(ModSounds.VR9P_STOP.get()),
            SoundSource.MASTER, player.getX(), player.getY(), player.getZ(), 2.0f, 1.0f, 0));
    }

    private static void playContinue(ServerPlayer player) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            net.minecraft.core.Holder.direct(ModSounds.VR9P_CONTINUE.get()),
            SoundSource.MASTER, player.getX(), player.getY(), player.getZ(), 2.0f, 1.0f, 0));
    }

    private static void showState(ServerPlayer player, Vr9pState state) {
        if (state.showingStop) {
            sendState(player, state.stargazed ? Vr9pPacket.STATE_STARGAZED_STOP : Vr9pPacket.STATE_STOP, 0);
            playStop(player);
        } else {
            sendState(player, state.stargazed ? Vr9pPacket.STATE_STARGAZED_CONTINUE : Vr9pPacket.STATE_CONTINUE, 0);
            playContinue(player);
        }
    }

    private static int getDuration(Vr9pState state) {
        return state.stargazed ? AbnormalitiesConfig.VR9P_STARGAZED_DURATION.get() : AbnormalitiesConfig.VR9P_EVENT_DURATION.get();
    }

    public static void tickPlayer(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Vr9pState state = ACTIVE.get(uuid);
        if (state == null) return;
        if (state.endPhaseTicks >= 0) {
            if (state.endPhaseTicks > 0) state.endPhaseTicks--;
            if (state.endPhaseTicks == 0) {
                stopAmbience(player);
                sendState(player, -1, 0);
                ACTIVE.remove(uuid);
                COOLDOWNS.put(uuid, AbnormalitiesConfig.VR9P_COOLDOWN_TICKS.get());
                if (!state.stargazed) ToxicController.onVr9pSuccess(player);
                LOGGER.info("{} vr9p ended", player.getName().getString());
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
            LOGGER.info("{} vr9p ending (vr9phit 5 ticks)", player.getName().getString());
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
        double moveThreshold = AbnormalitiesConfig.VR9P_MOVE_THRESHOLD.get();
        boolean playerMoved = Math.sqrt(dx * dx + dz * dz) > moveThreshold;
        if (Math.sqrt(dx * dx + dz * dz) > 10) {
            state.lastX = player.getX();
            state.lastZ = player.getZ();
            return;
        }
        state.lastX = player.getX();
        state.lastZ = player.getZ();

        if (state.showingStop) {
            if (playerMoved) addWrong(player, uuid, state);
            float yawDelta = Math.abs(player.getYRot() - state.lastYRot);
            float pitchDelta = Math.abs(player.getXRot() - state.lastXRot);
            state.lastYRot = player.getYRot();
            state.lastXRot = player.getXRot();
            if (yawDelta > 1.0f || pitchDelta > 1.0f) strictViolation(player);
            if (player.getInventory().selected != state.lastSlot) {
                state.lastSlot = player.getInventory().selected;
                strictViolation(player);
            }
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
                state.nextSwitchAt = AbnormalitiesConfig.VR9P_STARGAZED_SWITCH_TICKS.get();
                state.graceTicks = AbnormalitiesConfig.VR9P_STARGAZED_GRACE_TICKS.get();
            } else {
                int swMin = AbnormalitiesConfig.VR9P_SWITCH_MIN.get();
                int swMax = AbnormalitiesConfig.VR9P_SWITCH_MAX.get();
                state.nextSwitchAt = swMin + player.getRandom().nextInt(Math.max(1, swMax - swMin + 1));
                state.graceTicks = AbnormalitiesConfig.VR9P_GRACE_TICKS.get();
            }
            state.ticks = 0;
            state.wrongTicks = 0;
            showState(player, state);
        }
    }

    private static void addWrong(ServerPlayer player, UUID uuid, Vr9pState state) {
        state.wrongTicks++;
        int threshold = state.stargazed ? AbnormalitiesConfig.VR9P_STARGAZED_WRONG_THRESHOLD.get() : AbnormalitiesConfig.VR9P_WRONG_THRESHOLD.get();
        if (state.wrongTicks >= threshold) startPunish(player, uuid, state);
    }

    public static void strictViolation(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Vr9pState state = ACTIVE.get(uuid);
        if (state == null || !state.stargazed || !state.showingStop) return;
        if (state.graceTicks > 0) return;
        if (state.punishTicks >= 0 || state.endPhaseTicks >= 0) return;
        startPunish(player, uuid, state);
    }

    private static void startPunish(ServerPlayer player, UUID uuid, Vr9pState state) {
        if (state.punishTicks >= 0 || state.endPhaseTicks >= 0) return;
        state.punishTicks = 10;
        COOLDOWNS.put(uuid, AbnormalitiesConfig.VR9P_COOLDOWN_TICKS.get());
        LOGGER.info("{} PUNISHED (was {}, {})", player.getName().getString(), state.showingStop ? "STOP" : "CONTINUE", state.stargazed ? "STARGAZED" : "normal");
        sendState(player, 2, 0);
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            net.minecraft.core.Holder.direct(ModSounds.NUR_SOUND.get()),
            SoundSource.MASTER, player.getX(), player.getY(), player.getZ(), 10.0f, 1.0f, 0));
        float dmg = AbnormalitiesConfig.VR9P_DAMAGE.get().floatValue();
        player.hurt(player.damageSources().genericKill(), dmg);
    }

    private static void doPunish(ServerPlayer player, UUID uuid, Vr9pState state) {
        stopAmbience(player);
        ACTIVE.remove(uuid);
        var mode = state.stargazed ? AbnormalitiesConfig.VR9P_STARGAZED_PUNISH.get() : AbnormalitiesConfig.VR9P_PUNISH.get();
        if (mode == AbnormalitiesConfig.PunishMode.CRASH) {
            AbnormalitiesMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new com.abnormalities.network.CrashPacket());
        } else if (mode == AbnormalitiesConfig.PunishMode.KICK) {
            if (player.connection != null) {
                sendState(player, -1, 0);
                player.connection.disconnect(Component.literal("STARGAZED"));
            }
        } else {
            sendState(player, -1, 0);
        }
    }

    private static void sendState(ServerPlayer player, int state, int duration) {
        AbnormalitiesMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Vr9pPacket(state, duration));
    }

    public static void forceStart(ServerPlayer player) {
        if (ACTIVE.containsKey(player.getUUID())) return;
        startVr9p(player);
    }

    public static void forceStartStargazed(ServerPlayer player) {
        if (ACTIVE.containsKey(player.getUUID())) return;
        startVr9p(player, true);
    }

    public static void escViolation(ServerPlayer player) {
        strictViolation(player);
    }

    public static void cleanup(UUID uuid) {
        ACTIVE.remove(uuid);
        COOLDOWNS.put(uuid, AbnormalitiesConfig.VR9P_COOLDOWN_TICKS.get());
    }

    public static boolean isActive(UUID uuid) {
        return ACTIVE.containsKey(uuid);
    }
}
