package com.abnormalities.horror;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FakeChatManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|FakeChat");
    private static final Random RNG = new Random();
    private static final long FAKE_CHAT_COOLDOWN = 18000;
    private static final long FAKE_JOIN_COOLDOWN = 24000;
    private static final int MAX_EFFECTS_PER_MINUTE = 4;
    private static long nextChat = 0;
    private static long nextJoin = 0;
    private static int effectsThisMinute = 0;
    private static long lastMinuteReset = 0;

    private static final List<String> MESSAGES = List.of(
        "I see you.",
        "Can you see me?",
        "It was your fault.",
        "Help us.",
        "I am right behind you.",
        "null",
        "null.err",
        "000",
        "You should not have opened this world.",
        "It has been watching since the first day.",
        "Do not tell anyone.",
        "We know where you sleep."
    );

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!com.abnormalities.config.AbnormalitiesConfig.F4K3_ENABLED.get()) return;
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        long now = srv.getTickCount();
        if (nextChat == 0) {
            nextChat = now + FAKE_CHAT_COOLDOWN;
            nextJoin = now + FAKE_JOIN_COOLDOWN;
            return;
        }
        if (now >= nextChat) {
            nextChat = now + FAKE_CHAT_COOLDOWN;
            sendFakeChat(srv);
        }
        if (now >= nextJoin) {
            nextJoin = now + FAKE_JOIN_COOLDOWN;
            sendFakeJoinLeave(srv);
        }
    }

    private static void sendFakeChat(MinecraftServer srv) {
        var players = srv.getPlayerList().getPlayers();
        if (players.isEmpty()) return;
        ServerPlayer target = players.get(RNG.nextInt(players.size()));
        String msg = MESSAGES.get(RNG.nextInt(MESSAGES.size()));
        LOGGER.info("[FakeChat] {} -> {}", target.getName().getString(), msg);
        sendSystem(target, msg);
        triggerMechanicalEffect(target);
    }

    private static void sendFakeJoinLeave(MinecraftServer srv) {
        var players = srv.getPlayerList().getPlayers();
        if (players.isEmpty()) return;
        String name = com.abnormalities.FakeNames.NAMES.get(RNG.nextInt(com.abnormalities.FakeNames.NAMES.size()));
        String msg = RNG.nextBoolean() ? name + " joined the game" : name + " left the game";
        LOGGER.info("[FakeChat] broadcast: {}", msg);
        for (ServerPlayer p : players) {
            sendYellow(p, msg);
        }
    }

    public static void forceChat(ServerPlayer player) {
        if (player.connection == null) return;
        String msg = MESSAGES.get(RNG.nextInt(MESSAGES.size()));
        sendSystem(player, msg);
        triggerMechanicalEffect(player);
    }

    public static void forceJoinLeave() {
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        sendFakeJoinLeave(srv);
    }

    private static void triggerMechanicalEffect(ServerPlayer target) {
        if (target.level() instanceof ServerLevel level) {
            long now = level.getGameTime();
            if (now - lastMinuteReset > 1200) {
                effectsThisMinute = 0;
                lastMinuteReset = now;
            }
            if (effectsThisMinute >= MAX_EFFECTS_PER_MINUTE) return;
            effectsThisMinute++;
            switch (RNG.nextInt(4)) {
                case 0 -> particleBurst(level, target);
                case 1 -> caveSound(level, target);
                case 2 -> blindnessPulse(target);
                case 3 -> swapInventoryItems(target);
            }
        }
    }

    private static void particleBurst(ServerLevel level, ServerPlayer target) {
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() / 2.0;
        double z = target.getZ();
        // tried using different particle types but soul fire looks best
        // level.sendParticles(ParticleTypes.DRAGON_BREATH, x, y, z, 20, 0.3, 0.3, 0.3, 0.01);
        // level.sendParticles(ParticleTypes.SOUL, x, y, z, 20, 0.3, 0.3, 0.3, 0.01);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 20, 0.3, 0.3, 0.3, 0.01);
        LOGGER.info("[FakeChat] soul fire particles at {} ({},{},{})", target.getName().getString(), (int) x, (int) y, (int) z);
    }

    private static void caveSound(ServerLevel level, ServerPlayer target) {
        double x = target.getX() + (RNG.nextDouble() - 0.5) * 16.0;
        double y = target.getY() + (RNG.nextDouble() - 0.5) * 8.0;
        double z = target.getZ() + (RNG.nextDouble() - 0.5) * 16.0;
        level.playSound(null, x, y, z, SoundEvents.AMBIENT_CAVE.get(), SoundSource.AMBIENT, 2.0f, 0.5f + RNG.nextFloat() * 0.4f);
        LOGGER.info("[FakeChat] cave sound offset from {}", target.getName().getString());
    }

    private static void blindnessPulse(ServerPlayer target) {
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 5, 0, false, false, false));
        LOGGER.info("[FakeChat] blindness pulse on {}", target.getName().getString());
    }

    private static void swapInventoryItems(ServerPlayer target) {
        var inv = target.getInventory();
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (!inv.getItem(i).isEmpty()) candidates.add(i);
        }
        if (candidates.size() < 2) return;
        int a = candidates.get(RNG.nextInt(candidates.size()));
        int b;
        do { b = candidates.get(RNG.nextInt(candidates.size())); } while (b == a);
        ItemStack sa = inv.getItem(a).copy();
        ItemStack sb = inv.getItem(b).copy();
        inv.setItem(a, sb);
        inv.setItem(b, sa);
        LOGGER.info("[FakeChat] swapped slots {} and {} on {}", a, b, target.getName().getString());
    }

    private static void sendSystem(ServerPlayer player, String text) {
        if (player.connection == null) return;
        player.connection.send(new ClientboundSystemChatPacket(Component.literal(text), false));
    }

    private static void sendYellow(ServerPlayer player, String text) {
        if (player.connection == null) return;
        player.connection.send(new ClientboundSystemChatPacket(
                Component.literal(text).withStyle(net.minecraft.ChatFormatting.YELLOW), false));
    }
}
