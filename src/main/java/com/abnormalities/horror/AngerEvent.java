package com.abnormalities.horror;

import com.abnormalities.config.AbnormalitiesConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AngerEvent extends AbstractHorrorEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|Ang3r");
    private static final Map<UUID, Integer> ANGER_TICKS = new HashMap<>();
    private static final Random RNG = new Random();

    private static final String[] INSULTS = {
        "fuck", "shit", "bitch", "ass", "damn", "hell", "stupid", "idiot", "hate", "kill", "trash", "garbage", "noob", "loser", "suck"
    };

    private static final String[] ANGRY_REPLIES = {
        "watch your mouth.",
        "we heard that.",
        "say it again. i dare you.",
        "that was rude.",
        "you should not have said that.",
        "we do not like that tone.",
        "careful.",
        "we remember words like that.",
        "apologize.",
        "do not talk to us like that."
    };

    public AngerEvent() {
        super("ang3r", 80, 1.2, 0, 900, 36000, false);
    }

    @Override
    public boolean canTrigger(ServerPlayer player, long currentTick) {
        return AbnormalitiesConfig.ANG3R_ENABLED.get() && !player.level().isClientSide;
    }

    @Override
    public void execute(ServerPlayer player) {
        startAnger(player, AbnormalitiesConfig.ANG3R_DURATION.get());
    }

    public static void forceAnger(ServerPlayer player) {
        startAnger(player, AbnormalitiesConfig.ANG3R_DURATION.get());
    }

    private static void startAnger(ServerPlayer player, int ticks) {
        ANGER_TICKS.put(player.getUUID(), ticks);
        LOGGER.info("[Ang3r] {} is now angry for {}t", player.getName().getString(), ticks);
        sendWhisper(player, ANGRY_REPLIES[RNG.nextInt(ANGRY_REPLIES.length)]);
    }

    private static void sendWhisper(ServerPlayer player, String text) {
        player.connection.send(new ClientboundSystemChatPacket(
                Component.literal(text).withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), false));
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        if (!AbnormalitiesConfig.ANG3R_ENABLED.get()) return;
        ServerPlayer player = event.getPlayer();
        UUID uuid = player.getUUID();
        String msg = event.getMessage().getString().toLowerCase();
        int anger = ANGER_TICKS.getOrDefault(uuid, 0);
        if (anger > 0) {
            event.setCanceled(true);
            String reversed = new StringBuilder(event.getMessage().getString()).reverse().toString();
            for (var p : player.getServer().getPlayerList().getPlayers()) {
                p.connection.send(new ClientboundSystemChatPacket(
                        Component.literal("<" + player.getName().getString() + "> " + reversed), false));
            }
            if (RNG.nextInt(3) == 0) {
                sendWhisper(player, ANGRY_REPLIES[RNG.nextInt(ANGRY_REPLIES.length)]);
            }
            return;
        }
        for (String insult : INSULTS) {
            if (msg.contains(insult)) {
                if (RNG.nextInt(4) == 0) {
                    startAnger(player, AbnormalitiesConfig.ANG3R_DURATION.get());
                }
                break;
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (ANGER_TICKS.isEmpty()) return;
        var it = ANGER_TICKS.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            int ticks = e.getValue();
            if (ticks <= 1) {
                it.remove();
                continue;
            }
            e.setValue(ticks - 1);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        ANGER_TICKS.remove(event.getEntity().getUUID());
    }
}
