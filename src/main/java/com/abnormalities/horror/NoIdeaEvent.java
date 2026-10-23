package com.abnormalities.horror;

import com.abnormalities.AbnormalitiesMod;
import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.network.NoIdeaPacket;
import com.abnormalities.registry.ModSounds;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mod.EventBusSubscriber(modid = AbnormalitiesMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NoIdeaEvent extends AbstractHorrorEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|NoIdea");
    private static final Map<UUID, Long> ACTIVE = new HashMap<>();
    private static final ScheduledExecutorService BURST_EXEC = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Abnormalities-NoIdea-Burst");
        t.setDaemon(true);
        return t;
    });

    public NoIdeaEvent() {
        super("you_have_no_idea", 30, 1.2, 0, 2500, 72000, false);
    }

    @Override
    public boolean canTrigger(ServerPlayer player, long currentTick) {
        return !player.level().isClientSide && AbnormalitiesConfig.NO_IDEA_ENABLED.get();
    }

    @Override
    public void execute(ServerPlayer player) {
        int duration = AbnormalitiesConfig.NO_IDEA_DURATION.get();
        HorrorEventPool.clearAllOngoing();
        ACTIVE.put(player.getUUID(), player.level().getGameTime() + duration);
        AbnormalitiesMod.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), new NoIdeaPacket(0, duration));
        burst(player);
        LOGGER.info("[NoIdea] fired on {}", player.getName().getString());
    }

    @Override
    public void onCleanup(ServerPlayer player) {
        ACTIVE.remove(player.getUUID());
        AbnormalitiesMod.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), new NoIdeaPacket(1, 0));
    }

    @SubscribeEvent
    public static void onWorlTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (ACTIVE.isEmpty()) return;
        MinecraftServer srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        long gt = overworld.getGameTime();

        for (ServerPlayer sp : new java.util.ArrayList<>(srv.getPlayerList().getPlayers())) {
            UUID uid = sp.getUUID();
            Long end = ACTIVE.get(uid);
            if (end == null) continue;
            if (gt >= end) {
                ACTIVE.remove(uid);
                AbnormalitiesMod.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp), new NoIdeaPacket(1, 0));
                continue;
            }
            sp.setDeltaMovement(0.0, 0.0, 0.0);
            sp.hasImpulse = false;
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            ACTIVE.remove(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(net.minecraftforge.event.level.LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel sl && sl.dimension() == Level.OVERWORLD) {
            for (UUID uid : new java.util.ArrayList<>(ACTIVE.keySet())) {
                for (ServerPlayer sp : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                    if (sp.getUUID().equals(uid)) {
                        AbnormalitiesMod.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp), new NoIdeaPacket(1, 0));
                    }
                }
            }
            ACTIVE.clear();
        }
    }

    public static void fire(ServerPlayer player) {
        if (!AbnormalitiesConfig.NO_IDEA_ENABLED.get()) return;
        int duration = AbnormalitiesConfig.NO_IDEA_DURATION.get();
        HorrorEventPool.clearAllOngoing();
        ACTIVE.put(player.getUUID(), player.level().getGameTime() + duration);
        AbnormalitiesMod.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), new NoIdeaPacket(0, duration));
        burst(player);
        LOGGER.info("[NoIdea] fired on {}", player.getName().getString());
    }

    private static void burst(ServerPlayer player) {
        for (int i = 0; i < 4; i++) {
            final int delay = i;
            BURST_EXEC.schedule(() -> {
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                        net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(ModSounds.NO_IDEA_SOUND.get()),
                        SoundSource.MASTER, player.getX(), player.getY(), player.getZ(), 1.0F, 1.0F,
                        player.getRandom().nextLong()));
            }, delay, TimeUnit.MILLISECONDS);
        }
    }
}
