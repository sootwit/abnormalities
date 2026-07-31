package com.abnormalities.horror;

import com.abnormalities.config.AbnormalitiesConfig;
import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

public class OtherController {
    private static final Map<UUID, OtherState> ACTIVE = new HashMap<>();
    private static final Map<UUID, Integer> COOLDOWNS = new HashMap<>();
    private static final Random RNG = new Random();

    private static class OtherState {
        UUID fakeId;
        String fakeName;
        String victimName;
        int ticksLeft;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        var overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        if (!AbnormalitiesConfig.OTHER_ENABLED.get()) return;
        long currentDay = overworld.getDayTime() / 24000L;
        if (currentDay < AbnormalitiesConfig.GRACE_PERIOD_DAYS.get()) return;

        COOLDOWNS.replaceAll((u, cd) -> cd - 1);
        COOLDOWNS.values().removeIf(cd -> cd <= 0);

        Iterator<Map.Entry<UUID, OtherState>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            OtherState s = e.getValue();
            ServerPlayer target = overworld.getServer().getPlayerList().getPlayer(e.getKey());
            if (target == null || !target.isAlive() || !target.level().equals(overworld)) {
                it.remove();
                COOLDOWNS.put(e.getKey(), AbnormalitiesConfig.OTHER_COOLDOWN.get());
                continue;
            }
            s.ticksLeft--;
            if (s.ticksLeft <= 0) {
                removeFake(target, s);
                it.remove();
                COOLDOWNS.put(e.getKey(), AbnormalitiesConfig.OTHER_COOLDOWN.get());
            }
        }

        if (overworld.getGameTime() % 120 != 0) return;
        if (overworld.random.nextInt(AbnormalitiesConfig.OTHER_SPAWN_WEIGHT.get()) != 0) return;
        for (var sp : new java.util.ArrayList<>(overworld.getServer().getPlayerList().getPlayers())) {
            if (ACTIVE.containsKey(sp.getUUID()) || COOLDOWNS.containsKey(sp.getUUID())) continue;
            if (overworld.random.nextInt(4) != 0) continue;
            startFake(sp, overworld);
            break;
        }
    }

    private static void startFake(ServerPlayer target, ServerLevel level) {
        OtherState s = new OtherState();
        s.fakeId = UUID.randomUUID();
            var online = new java.util.ArrayList<>(level.getServer().getPlayerList().getPlayers());
        if (online.size() > 1) {
            ServerPlayer victim = online.get(RNG.nextInt(online.size()));
            while (victim == target) victim = online.get(RNG.nextInt(online.size()));
            s.victimName = victim.getName().getString();
        } else {
            s.victimName = target.getName().getString();
        }
        s.fakeName = mutateName(s.victimName);
        s.ticksLeft = 100 + RNG.nextInt(140);
        ACTIVE.put(target.getUUID(), s);
        addFake(target, s);
        sendJoinMessage(target, s);
    }

    private static String mutateName(String name) {
        if (name.isEmpty()) return "unknown";
        int idx = RNG.nextInt(name.length());
        char c = name.charAt(idx);
        char nc;
        do {
            nc = (char) ('a' + RNG.nextInt(26));
        } while (nc == Character.toLowerCase(c));
        if (Character.isUpperCase(c)) nc = Character.toUpperCase(nc);
        return name.substring(0, idx) + nc + name.substring(idx + 1);
    }

    private static void addFake(ServerPlayer target, OtherState s) {
        GameProfile profile = new GameProfile(s.fakeId, s.fakeName);
        var entry = new ClientboundPlayerInfoUpdatePacket.Entry(s.fakeId, profile, true, 1,
                GameType.SURVIVAL, Component.literal(s.fakeName), null);
        net.minecraft.network.FriendlyByteBuf buf = new net.minecraft.network.FriendlyByteBuf(
                io.netty.buffer.Unpooled.buffer());
        buf.writeEnumSet(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED), ClientboundPlayerInfoUpdatePacket.Action.class);
        buf.writeCollection(List.of(entry), (b, e) -> {
            b.writeUUID(e.profileId());
            b.writeUtf(e.profile().getName(), 16);
            b.writeGameProfileProperties(e.profile().getProperties());
            b.writeBoolean(true);
        });
        target.connection.send(new ClientboundPlayerInfoUpdatePacket(buf));
    }

    private static void removeFake(ServerPlayer target, OtherState s) {
        target.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(s.fakeId)));
        sendLeaveMessage(target, s);
    }

    private static void sendJoinMessage(ServerPlayer target, OtherState s) {
        target.connection.send(new ClientboundSystemChatPacket(
                Component.literal(s.fakeName + " joined the game").withStyle(ChatFormatting.YELLOW), false));
    }

    private static void sendLeaveMessage(ServerPlayer target, OtherState s) {
        target.connection.send(new ClientboundSystemChatPacket(
                Component.literal(s.fakeName + " left the game").withStyle(ChatFormatting.YELLOW), false));
    }

    public static void forceStart(ServerPlayer player) {
        if (ACTIVE.containsKey(player.getUUID())) return;
        startFake(player, (ServerLevel) player.level());
    }

    public static boolean isActive(UUID uuid) {
        return ACTIVE.containsKey(uuid);
    }

    @SubscribeEvent
    public static void onPlayerLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() != null) {
            UUID uuid = event.getEntity().getUUID();
            ACTIVE.remove(uuid);
            COOLDOWNS.put(uuid, AbnormalitiesConfig.OTHER_COOLDOWN.get());
        }
    }
}
