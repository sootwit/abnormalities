package com.abnormalities.horror;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abnormalities.config.AbnormalitiesConfig;

public class ChatArgManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|ChatArg");
    private static final Random RNG = new Random();
/*     private static final int ANGER_DURATION = 36000; */ // config 
    private static final int HOSTILE_EXTRA_INTERVAL = 600;

    private static final Map<String, String> REPLIES = Map.of(
        "i love you", "< > uoy_wonk_i",
        "hi", "< > iH",
        "what do you want", "< > .evaeL_oT_uoY",
        "where are you", "< > .uoY_gnihctaW",
        "can you see me", "< > .uoY_eeS_naC_I",
        "who are you", "< > ?I_mA_ohW",
        "help", "< > .oN",
        "run", "< > .nur",
        "null", "< > .dneirF_dlO"
    );

    private static final String ANGRY_REPLY = "< > .daeD_eB_dluoW_uoY_ekil_htiW_em";

    private static final List<String> INSULTS = List.of(
        "kill yourself", "i hate you", "fuck you", "die", "shut up", "stupid", "sybau", "stfu"
    );

    private static final List<String> HOSTILE_POOL = List.of(
        "do not test me.", "you will regret that.", "i do not forgive.", "quiet. now."
    );

    private static final List<String> CALM_POOL = List.of(
        "fine.", "maybe i forgive you.", "we are even.", "do not do that again."
    );

    private static long angryUntil = 0;
    private static UUID angryPlayer = null;
    private static long nextExtra = 0;

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player.connection == null) return;
        String msg = event.getMessage().getString().trim().toLowerCase(Locale.ROOT);
        for (String insult : INSULTS) {
            if (matchesInsult(msg, insult)) {
                triggerAnger(player);
                return;
            }
        }
        if (REPLIES.containsKey(msg)) {
            boolean angry = serverTick() < angryUntil;
            String reply = angry ? hostileReply() : REPLIES.get(msg);
            LOGGER.debug("[ChatArg] {} said '{}', reply: {}", player.getName().getString(), msg, reply);
            sendReply(player, reply, !angry);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!com.abnormalities.config.AbnormalitiesConfig.ANG3R_ENABLED.get()) return;
        long now = serverTick();
        if (angryUntil == 0) return;
        if (now >= angryUntil) {
            ServerPlayer target = findAngry();
            if (target != null) sendReply(target, calmReply());
            clearAnger();
            return;
        }
        if (now < nextExtra) return;
        nextExtra = now + HOSTILE_EXTRA_INTERVAL;
        ServerPlayer target = findAngry();
        if (target == null) {
            clearAnger();
            return;
        }
        sendReply(target, hostileReply(), false);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (angryPlayer != null && angryPlayer.equals(player.getUUID())) clearAnger();
    }

    public static void forceAnger(ServerPlayer player) {
        triggerAnger(player);
    }

    private static boolean matchesInsult(String msg, String insult) {
        return msg.matches(".*\\b" + Pattern.quote(insult) + "\\b.*");
    }

    private static void triggerAnger(ServerPlayer player) {
        angryUntil = serverTick() + AbnormalitiesConfig.ANG3R_DURATION.get();
        angryPlayer = player.getUUID();
        nextExtra = serverTick() + HOSTILE_EXTRA_INTERVAL;
        LOGGER.info("[ChatArg] {} anger triggered for {}t", player.getName().getString(), AbnormalitiesConfig.ANG3R_DURATION.get());
        sendReply(player, ANGRY_REPLY, false);
        playCaveSound(player);
    }

    private static ServerPlayer findAngry() {
        var srv = ServerLifecycleHooks.getCurrentServer();
        return srv == null || angryPlayer == null ? null : srv.getPlayerList().getPlayer(angryPlayer);
    }

    private static void clearAnger() {
        angryUntil = 0;
        angryPlayer = null;
        nextExtra = 0;
    }

    private static long serverTick() {
        var srv = ServerLifecycleHooks.getCurrentServer();
        return srv == null ? 0 : srv.getTickCount();
    }

    private static String hostileReply() {
        return HOSTILE_POOL.get(RNG.nextInt(HOSTILE_POOL.size()));
    }

    private static String calmReply() {
        return CALM_POOL.get(RNG.nextInt(CALM_POOL.size()));
    }

    private static void sendReply(ServerPlayer player, String text) {
        sendReply(player, text, true);
    }

    private static void sendReply(ServerPlayer player, String text, boolean styled) {
        if (player.connection == null) return;
        Component component = styled
            ? Component.literal(text).withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC)
            : Component.literal(text);
        player.connection.send(new ClientboundSystemChatPacket(component, false));
    }

    private static void playCaveSound(ServerPlayer player) {
        if (player.connection == null) return;
        double ox = RNG.nextGaussian() * 2;
        double oy = RNG.nextGaussian();
        double oz = RNG.nextGaussian() * 2;
        player.connection.send(new ClientboundSoundPacket(
            SoundEvents.AMBIENT_CAVE, SoundSource.MASTER,
            player.getX() + ox, player.getY() + oy + 1, player.getZ() + oz,
            2.0f, 0.8f + RNG.nextFloat() * 0.4f, 0));
    }
}
