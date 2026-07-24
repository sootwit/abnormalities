package com.abnormalities.horror;

import com.abnormalities.AbnormalitiesMod;
import com.abnormalities.network.Vr9pPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

public class Vr9pController {
    private static final Map<UUID, Vr9pState> ACTIVE = new HashMap<>();

    private static class Vr9pState {
        int ticks = 0;
        boolean showingStop = true;
        int nextSwitchAt = 20 + new Random().nextInt(60);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var overworld = ServerLifecycleHooks.getCurrentServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        if (overworld.random.nextInt(400) != 0) return;

        for (var player : overworld.players()) {
            if (!(player instanceof ServerPlayer)) continue;
            ServerPlayer sp = (ServerPlayer) player;
            if (ACTIVE.containsKey(sp.getUUID())) continue;
            if (overworld.random.nextInt(3) != 0) continue;
            var state = new Vr9pState();
            ACTIVE.put(sp.getUUID(), state);
            sendState(sp, Vr9pPacket.STATE_STOP, 40);
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                net.minecraft.core.Holder.direct(net.minecraft.sounds.SoundEvents.ANVIL_PLACE),
                SoundSource.MASTER, sp.getX(), sp.getY(), sp.getZ(), 2.0f, 0.5f, 0));
        }
    }

    public static void tickPlayer(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Vr9pState state = ACTIVE.get(uuid);
        if (state == null) return;
        state.ticks++;
        if (state.ticks >= state.nextSwitchAt) {
            state.ticks = 0;
            state.showingStop = !state.showingStop;
            state.nextSwitchAt = 20 + new Random().nextInt(80);
            if (state.showingStop) {
                sendState(player, Vr9pPacket.STATE_STOP, 40 + new Random().nextInt(60));
            } else {
                sendState(player, Vr9pPacket.STATE_CONTINUE, 20 + new Random().nextInt(40));
            }
            if (state.ticks > 200 && ACTIVE.size() > 5) {
                ACTIVE.remove(uuid);
                sendState(player, Vr9pPacket.STATE_END, 0);
            }
        }
    }

    private static void sendState(ServerPlayer player, int state, int duration) {
        AbnormalitiesMod.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Vr9pPacket(state, duration));
    }

    public static void cleanup(UUID uuid) {
        ACTIVE.remove(uuid);
    }

    public static boolean isActive(UUID uuid) {
        return ACTIVE.containsKey(uuid);
    }
}
