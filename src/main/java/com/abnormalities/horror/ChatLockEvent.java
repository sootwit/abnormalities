package com.abnormalities.horror;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

public class ChatLockEvent extends AbstractHorrorEvent {
    private static final Map<UUID, Integer> LOCK_TICKS = new HashMap<>();
    private static final Random RNG = new Random();
    private final boolean disabled;

    private static final String[][] DISABLED_ERRORS = {
        {"86", "Unauthorized access by a mod"},
        {"85", "A mod tried to open a closed handler"},
        {"84", "A mod is writing to a protected memory region"},
        {"83", "A mod attempted a privileged operation"},
        {"82", "Failed to verify mod signature"},
        {"81", "A mod is accessing data without permission"},
        {"80", "A mod bypassed the event bus"},
        {"79", "Permission denied: handler unregistered"},
        {"78", "A mod mutated an immutable object"},
        {"77", "A mod tried to read your local files"},
        {"76", "Packet target unknown: a mod is listening"},
        {"75", "A mod ignored three permission checks"},
        {"74", "A mod registered an unknown handler"},
        {"73", "A mod is impersonating another mod"},
        {"72", "A mod tried to change the world seed"},
        {"71", "A mod opened a connection to an unseen server"},
        {"70", "A mod modified chat data without authorization"},
        {"69", "A mod is holding the message queue"}
    };

    private static final String[][] ENABLED_ERRORS = {
        {"87", "Authorization declined for a mod"},
        {"88", "The problem has been neutralized"},
        {"89", "The bug got fixed"},
        {"90", "The anomaly has been quarantined"},
        {"91", "Mod communication restored"},
        {"92", "The mod has been patched"},
        {"93", "Everything is fine now"},
        {"94", "The error corrected itself"},
        {"95", "Mod privileges were restored"},
        {"96", "The handler was unloaded"},
        {"97", "All requested data was released"},
        {"98", "The signal has gone quiet"},
        {"99", "Mod audit passed"},
        {"100", "Integrity confirmed"},
        {"101", "The connection was reestablished"},
        {"102", "You can talk now"}
    };

    public ChatLockEvent(boolean disabled) {
        super(disabled ? "chatDisabled" : "chatEnabled", disabled ? 60 : 50, 1.0, 0, 900, 12000, false);
        this.disabled = disabled;
    }

    @Override
    public void execute(ServerPlayer player) {
        if (disabled) {
            setDisabled(player, 300 + RNG.nextInt(601));
        } else {
            setEnabled(player);
        }
    }

    public static void forceDisabled(ServerPlayer player) {
        setDisabled(player, -1);
    }

    public static void forceEnabled(ServerPlayer player) {
        setEnabled(player);
    }

    private static void setDisabled(ServerPlayer player, int ticks) {
        LOCK_TICKS.put(player.getUUID(), ticks);
        sendForgeError(player);
        SisterController.onChatLockWarning(player, true);
    }

    private static void setEnabled(ServerPlayer player) {
        LOCK_TICKS.put(player.getUUID(), 0);
        sendForgeError(player);
        SisterController.onChatLockWarning(player, false);
    }

    private static void sendForgeError(ServerPlayer player) {
        String[][] pool = LOCK_TICKS.getOrDefault(player.getUUID(), 0) == 0 ? ENABLED_ERRORS : DISABLED_ERRORS;
        String[] entry = pool[RNG.nextInt(pool.length)];
        Component text = Component.literal("Forge Error").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(": " + entry[0] + " (" + entry[1] + ")").withStyle(ChatFormatting.WHITE));
        player.connection.send(new ClientboundSystemChatPacket(text, false));
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        if (LOCK_TICKS.getOrDefault(event.getPlayer().getUUID(), 0) == 0) return;
        event.setCanceled(true);
        event.getPlayer().displayClientMessage(Component.literal("Authentication failed"), true);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (LOCK_TICKS.isEmpty()) return;
        var it = LOCK_TICKS.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            int ticks = e.getValue();
            if (ticks < 0) continue;
            if (ticks <= 1) {
                it.remove();
                continue;
            }
            e.setValue(ticks - 1);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        int remaining = LOCK_TICKS.getOrDefault(player.getUUID(), 0);
        if (remaining != 0) {
            player.getPersistentData().putInt("abnormalities:chatLockTicks", remaining);
        } else {
            player.getPersistentData().remove("abnormalities:chatLockTicks");
        }
        LOCK_TICKS.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        int saved = player.getPersistentData().getInt("abnormalities:chatLockTicks");
        if (saved == 0) return;
        LOCK_TICKS.put(player.getUUID(), saved);
        player.getPersistentData().remove("abnormalities:chatLockTicks");
        sendForgeError(player);
    }
}