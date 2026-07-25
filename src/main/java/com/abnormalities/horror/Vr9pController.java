package com.abnormalities.horror;

import com.abnormalities.AbnormalitiesMod;
import com.abnormalities.network.Vr9pPacket;
import com.abnormalities.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

public class Vr9pController {
    private static final int WRONG_TICK_THRESHOLD = 20;
    private static final Map<UUID, Vr9pState> ACTIVE = new HashMap<>();
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("vr9p");

    private static class Vr9pState {
        int ticks = 0;
        boolean showingStop = true;
        int nextSwitchAt = 20 + new Random().nextInt(60);
        int totalTicks = 0;
        double lastX, lastZ;
        int graceTicks = 0;
        int startupTicks = 10;
        int wrongTicks = 0;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var overworld = ServerLifecycleHooks.getCurrentServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        for (var p : overworld.getServer().getPlayerList().getPlayers()) {
            if (p instanceof ServerPlayer) tickPlayer((ServerPlayer) p);
        }

        if (overworld.random.nextInt(400) != 0) return;

        for (var player : overworld.players()) {
            if (!(player instanceof ServerPlayer)) continue;
            ServerPlayer sp = (ServerPlayer) player;
            if (ACTIVE.containsKey(sp.getUUID())) continue;
            if (overworld.random.nextInt(3) != 0) continue;
            startVr9p(sp);
        }
    }

    private static void startVr9p(ServerPlayer player) {
        var st = new Vr9pState();
        st.lastX = player.getX();
        st.lastZ = player.getZ();
        st.graceTicks = 30;
        ACTIVE.put(player.getUUID(), st);
        sendState(player, 2, 0);
        LOGGER.info("vr9p started for {} (startup 10 ticks, then grace 30)", player.getName().getString());
    }

    public static void tickPlayer(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Vr9pState state = ACTIVE.get(uuid);
        if (state == null) return;
        if (state.graceTicks < 0) return;
        state.totalTicks++;

        if (state.startupTicks > 0) {
            state.startupTicks--;
            if (state.startupTicks == 0) LOGGER.info("{} startup done", player.getName().getString());
            return;
        }

        if (state.ticks == 0 && state.graceTicks == 0) {
            state.showingStop = player.getRandom().nextBoolean();
            state.graceTicks = 30;
            state.nextSwitchAt = 20 + player.getRandom().nextInt(60);
            LOGGER.info("{} first state: {}", player.getName().getString(), state.showingStop ? "STOP" : "CONTINUE");
            if (state.showingStop) {
                sendState(player, Vr9pPacket.STATE_STOP, 0);
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                    net.minecraft.core.Holder.direct(ModSounds.VR9P_STOP.get()),
                    SoundSource.MASTER, player.getX(), player.getY(), player.getZ(), 2.0f, 1.0f, 0));
            } else {
                sendState(player, Vr9pPacket.STATE_CONTINUE, 0);
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                    net.minecraft.core.Holder.direct(ModSounds.VR9P_CONTINUE.get()),
                    SoundSource.MASTER, player.getX(), player.getY(), player.getZ(), 2.0f, 1.0f, 0));
            }
            return;
        }

        state.ticks++;

        if (state.graceTicks > 0) {
            state.graceTicks--;
            if (state.graceTicks == 0) LOGGER.info("{} grace ended, wrongTicks={}", player.getName().getString(), state.wrongTicks);
            return;
        }

        double dx = player.getX() - state.lastX;
        double dz = player.getZ() - state.lastZ;
        boolean playerMoved = Math.sqrt(dx * dx + dz * dz) > 0.3;
        state.lastX = player.getX();
        state.lastZ = player.getZ();

        if (state.showingStop && playerMoved) {
            state.wrongTicks++;
            LOGGER.info("{} STOP+move wrongTicks={}/{}", player.getName().getString(), state.wrongTicks, WRONG_TICK_THRESHOLD);
            if (state.wrongTicks >= WRONG_TICK_THRESHOLD) {
                punish(player, uuid);
                return;
            }
        } else if (!state.showingStop && !playerMoved) {
            state.wrongTicks++;
            LOGGER.info("{} CONTINUE+still wrongTicks={}/{}", player.getName().getString(), state.wrongTicks, WRONG_TICK_THRESHOLD);
            if (state.wrongTicks >= WRONG_TICK_THRESHOLD) {
                punish(player, uuid);
                return;
            }
        } else {
            if (state.wrongTicks > 0) LOGGER.info("{} wrong state corrected", player.getName().getString());
            state.wrongTicks = 0;
        }

        if (state.ticks >= state.nextSwitchAt) {
            state.ticks = 0;
            state.showingStop = !state.showingStop;
            state.nextSwitchAt = 20 + new Random().nextInt(80);
            state.graceTicks = 30;
            if (state.showingStop) {
                sendState(player, Vr9pPacket.STATE_STOP, 0);
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                    net.minecraft.core.Holder.direct(ModSounds.VR9P_STOP.get()),
                    SoundSource.MASTER, player.getX(), player.getY(), player.getZ(), 2.0f, 1.0f, 0));
            } else {
                sendState(player, Vr9pPacket.STATE_CONTINUE, 0);
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                    net.minecraft.core.Holder.direct(ModSounds.VR9P_CONTINUE.get()),
                    SoundSource.MASTER, player.getX(), player.getY(), player.getZ(), 2.0f, 1.0f, 0));
            }
            LOGGER.info("{} switched to {}, grace 30, wrongTicks reset", player.getName().getString(), state.showingStop ? "STOP" : "CONTINUE");
        }

        if (state.totalTicks > 300) {
            ACTIVE.remove(uuid);
            sendState(player, Vr9pPacket.STATE_END, 0);
            LOGGER.info("{} vr9p ended (timeout)", player.getName().getString());
        }
    }

    private static void punish(ServerPlayer player, UUID uuid) {
        Vr9pState s = ACTIVE.get(uuid);
        if (s == null) return;
        s.graceTicks = -1;
        LOGGER.info("{} PUNISHED (was {})", player.getName().getString(), s.showingStop ? "STOP" : "CONTINUE");
        sendState(player, 2, 0);
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            net.minecraft.core.Holder.direct(ModSounds.NUR_SOUND.get()),
            SoundSource.MASTER, player.getX(), player.getY(), player.getZ(), 10.0f, 1.0f, 0));
        player.hurt(player.damageSources().genericKill(), 10.0F);
        var srv = player.getServer();
        if (srv != null) {
            srv.tell(new net.minecraft.server.TickTask(srv.getTickCount() + 10, () -> {
                ACTIVE.remove(uuid);
                sendState(player, Vr9pPacket.STATE_END, 0);
                player.connection.disconnect(Component.literal("STARGAZED"));
            }));
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
    }

    public static boolean isActive(UUID uuid) {
        return ACTIVE.containsKey(uuid);
    }
}