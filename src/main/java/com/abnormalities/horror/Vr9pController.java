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
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("vr9p");

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
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!AbnormalitiesConfig.VR9P_ENABLED.get()) return;
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        var overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        for (var p : overworld.getServer().getPlayerList().getPlayers()) {
            if (p instanceof ServerPlayer) tickPlayer((ServerPlayer) p);
        }

        COOLDOWNS.values().removeIf(cd -> cd <= 0);
        COOLDOWNS.replaceAll((u, cd) -> cd - 1);

        long currentDay = overworld.getDayTime() / 24000L;
        if (currentDay < AbnormalitiesConfig.GRACE_PERIOD_DAYS.get()) return;

        if (overworld.getGameTime() % 20 != 0) return;
        if (overworld.random.nextInt(AbnormalitiesConfig.VR9P_SPAWN_WEIGHT.get()) != 0) return;

        for (var player : overworld.players()) {
            if (!(player instanceof ServerPlayer)) continue;
            ServerPlayer sp = (ServerPlayer) player;
            if (ACTIVE.containsKey(sp.getUUID())) continue;
            if (COOLDOWNS.containsKey(sp.getUUID())) continue;
            if (overworld.random.nextInt(3) != 0) continue;
            startVr9p(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() != null) {
            UUID uuid = event.getEntity().getUUID();
            ACTIVE.remove(uuid);
            COOLDOWNS.remove(uuid);
        }
    }

    private static void startVr9p(ServerPlayer player) {
        var st = new Vr9pState();
        st.lastX = player.getX();
        st.lastZ = player.getZ();
        st.graceTicks = AbnormalitiesConfig.VR9P_GRACE_TICKS.get();
        st.startupTicks = 3;
        st.showingStop = player.getRandom().nextBoolean();
        int swMin = AbnormalitiesConfig.VR9P_SWITCH_MIN.get();
        int swMax = AbnormalitiesConfig.VR9P_SWITCH_MAX.get();
        st.nextSwitchAt = swMin + player.getRandom().nextInt(Math.max(1, swMax - swMin + 1));
        ACTIVE.put(player.getUUID(), st);
        sendState(player, 2, 0);
        playAmbience(player);
        LOGGER.info("vr9p started for {} (first state: {})", player.getName().getString(), st.showingStop ? "STOP" : "CONTINUE");
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
            sendState(player, Vr9pPacket.STATE_STOP, 0);
            playStop(player);
        } else {
            sendState(player, Vr9pPacket.STATE_CONTINUE, 0);
            playContinue(player);
        }
    }

    public static void tickPlayer(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Vr9pState state = ACTIVE.get(uuid);
        if (state == null) return;
        if (state.graceTicks < 0) return;
        state.totalTicks++;

        if (state.startupTicks > 0) {
            state.startupTicks--;
            if (state.startupTicks == 0) {
                showState(player, state);
                LOGGER.info("{} startup done, now showing {}", player.getName().getString(), state.showingStop ? "STOP" : "CONTINUE");
            }
            return;
        }

        if (state.totalTicks < AbnormalitiesConfig.VR9P_EVENT_DURATION.get() && state.totalTicks >= state.nextAmbienceAt) {
            playAmbience(player);
            state.nextAmbienceAt = state.totalTicks + 100;
        }

        if (state.graceTicks > 0) {
            state.graceTicks--;
            if (state.graceTicks == 0) {
                state.ticks = 0;
                LOGGER.info("{} grace ended", player.getName().getString());
            }
            return;
        }

        state.ticks++;

        double dx = player.getX() - state.lastX;
        double dz = player.getZ() - state.lastZ;
        double moveThreshold = AbnormalitiesConfig.VR9P_MOVE_THRESHOLD.get();
        boolean playerMoved = Math.sqrt(dx * dx + dz * dz) > moveThreshold;
        state.lastX = player.getX();
        state.lastZ = player.getZ();

        int wrongThreshold = AbnormalitiesConfig.VR9P_WRONG_THRESHOLD.get();

        if (state.showingStop && playerMoved) {
            state.wrongTicks++;
            if (state.wrongTicks >= wrongThreshold) {
                punish(player, uuid);
                return;
            }
        } else if (!state.showingStop && !playerMoved) {
            state.wrongTicks++;
            if (state.wrongTicks >= wrongThreshold) {
                punish(player, uuid);
                return;
            }
        } else {
            if (state.wrongTicks > 0) LOGGER.info("{} wrong state corrected", player.getName().getString());
            state.wrongTicks = 0;
        }

        if (state.ticks >= state.nextSwitchAt) {
            state.showingStop = player.getRandom().nextBoolean();
            int swMin = AbnormalitiesConfig.VR9P_SWITCH_MIN.get();
            int swMax = AbnormalitiesConfig.VR9P_SWITCH_MAX.get();
            state.nextSwitchAt = swMin + player.getRandom().nextInt(Math.max(1, swMax - swMin + 1));
            state.graceTicks = AbnormalitiesConfig.VR9P_GRACE_TICKS.get();
            state.ticks = 0;
            state.wrongTicks = 0;
            showState(player, state);
            LOGGER.info("{} switched to {}, grace {}", player.getName().getString(), state.showingStop ? "STOP" : "CONTINUE", state.graceTicks);
        }

        if (state.totalTicks >= AbnormalitiesConfig.VR9P_EVENT_DURATION.get()) {
            sendState(player, 2, 0);
            state.graceTicks = -1;
            LOGGER.info("{} vr9p ending (vr9phit 5 ticks)", player.getName().getString());
            var srv = ServerLifecycleHooks.getCurrentServer();
            if (srv != null) {
                srv.tell(new net.minecraft.server.TickTask(srv.getTickCount() + 5, () -> {
                    if (player.connection != null) {
                        stopAmbience(player);
                        sendState(player, -1, 0);
                    }
                    ACTIVE.remove(uuid);
                    COOLDOWNS.put(uuid, AbnormalitiesConfig.VR9P_COOLDOWN_TICKS.get());
                    LOGGER.info("{} vr9p ended", player.getName().getString());
                }));
            }
        }
    }

    private static void punish(ServerPlayer player, UUID uuid) {
        Vr9pState s = ACTIVE.get(uuid);
        if (s == null) return;
        int cd = AbnormalitiesConfig.VR9P_COOLDOWN_TICKS.get();
        s.graceTicks = -1;
        COOLDOWNS.put(uuid, cd);
        LOGGER.info("{} PUNISHED (was {})", player.getName().getString(), s.showingStop ? "STOP" : "CONTINUE");
        sendState(player, 2, 0);
        float dmg = AbnormalitiesConfig.VR9P_DAMAGE.get().floatValue();
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            net.minecraft.core.Holder.direct(ModSounds.NUR_SOUND.get()),
            SoundSource.MASTER, player.getX(), player.getY(), player.getZ(), 10.0f, 1.0f, 0));
        player.hurt(player.damageSources().genericKill(), dmg);
        var mode = AbnormalitiesConfig.VR9P_PUNISH.get();
        if (mode == AbnormalitiesConfig.PunishMode.CRASH) {
            AbnormalitiesMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new com.abnormalities.network.CrashPacket());
            ACTIVE.remove(uuid);
            COOLDOWNS.put(uuid, cd);
            return;
        }
        if (mode == AbnormalitiesConfig.PunishMode.KICK) {
            var srv = player.getServer();
            if (srv != null) {
                srv.tell(new net.minecraft.server.TickTask(srv.getTickCount() + 5, () -> {
                    ACTIVE.remove(uuid);
                    COOLDOWNS.put(uuid, cd);
                    if (player.connection != null) {
                        sendState(player, -1, 0);
                        player.connection.disconnect(Component.literal("STARGAZED"));
                    }
                }));
            }
        } else {
            stopAmbience(player);
            ACTIVE.remove(uuid);
            COOLDOWNS.put(uuid, cd);
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

    public static void cleanup(UUID uuid) {
        ACTIVE.remove(uuid);
        COOLDOWNS.put(uuid, AbnormalitiesConfig.VR9P_COOLDOWN_TICKS.get());
    }

    public static boolean isActive(UUID uuid) {
        return ACTIVE.containsKey(uuid);
    }
}