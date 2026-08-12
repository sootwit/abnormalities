package com.abnormalities.thewind;

import com.abnormalities.config.AbnormalitiesConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class WindErrorHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|TheWind|Error");
    private static final Map<UUID, Long> ERROR_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Integer> DISMISS_COUNTS = new HashMap<>();
    private static final Map<UUID, Integer> ACTIVE_FREEZES = new HashMap<>();

    private static final String[] TITLES = {
        "THE_WIND",
        "THE_WIND",
        "THE_WIND",
        "System Error",
        "THE_WIND"
    };

    private static final String[] MESSAGES = {
        "THE WIND IS STRONG TODAY, ISN'T IT %s?",
        "I'M CURIOUS ABOUT YOU, %s.",
        "DO YOU FEEL THAT, %s?",
        "IT'S WATCHING, %s.",
        "YOU SHOULDN'T HAVE LOOKED, %s.",
        "THERE IS NO ESCAPE, %s.",
        "THE WIND REMEMBERS, %s.",
        "YOU LEFT THE DOOR OPEN, %s."
    };

    private static final String[] DISMISS_MESSAGES = {
        "the wind grows stronger...",
        "you shouldn't have done that.",
        "it knows you're afraid.",
        "dismissed. for now.",
        "the wind doesn't forget."
    };

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!AbnormalitiesConfig.TW_ENABLED.get()) return;

        var srv = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;

        Iterator<Map.Entry<UUID, Integer>> freezeIt = ACTIVE_FREEZES.entrySet().iterator();
        while (freezeIt.hasNext()) {
            var entry = freezeIt.next();
            ServerPlayer player = srv.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !player.isAlive()) {
                freezeIt.remove();
                continue;
            }
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                freezeIt.remove();
                LOGGER.info("[THE_WIND|Error] Freeze complete for {}", player.getName().getString());
            } else {
                entry.setValue(remaining);
            }
        }
    }

    public static void triggerError(ServerPlayer player) {
        if (!AbnormalitiesConfig.TW_ENABLED.get()) return;
        if (!AbnormalitiesConfig.TW_WIND_ERROR_ENABLED.get()) return;
        UUID uuid = player.getUUID();

        long now = player.level().getGameTime();
        long cooldown = AbnormalitiesConfig.TW_WIND_ERROR_COOLDOWN.get();
        if (ERROR_COOLDOWNS.containsKey(uuid) && now - ERROR_COOLDOWNS.get(uuid) < cooldown) return;

        ERROR_COOLDOWNS.put(uuid, now);

        String username = System.getProperty("user.name", "player");
        String title = TITLES[new Random().nextInt(TITLES.length)];
        String message = String.format(MESSAGES[new Random().nextInt(MESSAGES.length)], username);

        int dismissCount = DISMISS_COUNTS.getOrDefault(uuid, 0);
        double multiplier = Math.pow(1.5, dismissCount);

        LOGGER.info("[THE_WIND|Error] Triggering error for {} (dismisses={}, multiplier={})", player.getName().getString(), dismissCount, String.format("%.1f", multiplier));

        com.abnormalities.AbnormalitiesMod.CHANNEL.send(
            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
            new com.abnormalities.network.WindErrorPacket(title, message, (float) multiplier));

        ACTIVE_FREEZES.put(uuid, 60);
    }

    public static void onDismiss(ServerPlayer player) {
        UUID uuid = player.getUUID();
        int count = DISMISS_COUNTS.getOrDefault(uuid, 0) + 1;
        DISMISS_COUNTS.put(uuid, count);

        String msg = DISMISS_MESSAGES[new Random().nextInt(DISMISS_MESSAGES.length)];
        player.displayClientMessage(Component.literal(msg).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), false);

        LOGGER.info("[THE_WIND|Error] {} dismissed (total dismisses={}, event multiplier now x{})", player.getName().getString(), count, String.format("%.1f", Math.pow(1.5, count)));
    }

    public static void onClose(ServerPlayer player) {
        UUID uuid = player.getUUID();
        DISMISS_COUNTS.remove(uuid);

        player.connection.disconnect(Component.literal("THE_WIND").withStyle(ChatFormatting.RED));
        LOGGER.info("[THE_WIND|Error] {} closed program (forced disconnect)", player.getName().getString());
    }

    public static float getEventMultiplier(ServerPlayer player) {
        int count = DISMISS_COUNTS.getOrDefault(player.getUUID(), 0);
        return (float) Math.pow(1.5, count);
    }

    public static void forceError(ServerPlayer player) {
        ERROR_COOLDOWNS.remove(player.getUUID());
        triggerError(player);
    }
}
