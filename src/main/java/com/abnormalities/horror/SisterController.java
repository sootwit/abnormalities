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

public class SisterController {
    private static final List<String> NUR_WARNINGS = List.of(
            "The shy one is coming.",
            "He's a bit shy. Ignore him.",
            "Don't look at him. He doesn't like it.",
            "He gets faster the further you run. Strange, isn't it?",
            "He won't stop until you're gone."
    );
    private static final List<String> K3W_WARNINGS = List.of(
            "He will try to copy you. Sleep.",
            "He wants friends. Not in the way you think. Don't let him.",
            "He's wearing your face. That's not you.",
            "Whatever you did today, he'll undo it.",
            "He follows your steps. Leave none."
    );
    private static final List<String> FAREWELLS = List.of(
            "I must go now. Maybe in another world.",
            "They need me. Farewell."
    );

    private static UUID SISTER_UUID = null;
    private static String displayName = "Sister";
    private static boolean joined = false;
    private static long joinedDay = -1;
    private static long leftDay = -1;
    private static final List<Warning> PENDING = new ArrayList<>();
    private static boolean everJoined = false;

    private static class Warning {
        final ServerPlayer target;
        final String message;
        int ticksLeft = 40;
        Warning(ServerPlayer target, String message) {
            this.target = target;
            this.message = message;
        }
    }

    private static UUID sisterUuid() {
        if (SISTER_UUID == null) {
            UUID u;
            do {
                u = UUID.randomUUID();
            } while ((u.hashCode() & 1) == 0);
            SISTER_UUID = u;
        }
        return SISTER_UUID;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        var overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        if (!AbnormalitiesConfig.SISTER_ENABLED.get()) return;
        long currentDay = overworld.getDayTime() / 24000L;
        if (currentDay < AbnormalitiesConfig.GRACE_PERIOD_DAYS.get()) return;

        if (joined) {
            if (currentDay - joinedDay >= AbnormalitiesConfig.SISTER_STAY_DAYS.get()) {
                leave(overworld);
            }
        } else if (!everJoined && currentDay <= AbnormalitiesConfig.SISTER_MAX_DAY.get()) {
            if (overworld.getGameTime() % 400 == 0 && overworld.random.nextInt(AbnormalitiesConfig.SISTER_CHANCE.get()) == 0) {
                join(overworld);
            }
        }

        Iterator<Warning> wit = PENDING.iterator();
        while (wit.hasNext()) {
            Warning w = wit.next();
            w.ticksLeft--;
            if (w.ticksLeft <= 0) {
                if (w.target.connection != null) {
                    w.target.connection.send(new ClientboundSystemChatPacket(
                            Component.literal("<" + displayName + "> " + w.message).withStyle(ChatFormatting.LIGHT_PURPLE), false));
                }
                wit.remove();
            }
        }
    }

    public static void onNurWarning(ServerPlayer target) {
        if (!joined) return;
        if (target == null) return;
        PENDING.add(new Warning(target, NUR_WARNINGS.get(new Random().nextInt(NUR_WARNINGS.size()))));
    }

    public static void onK3wWarning(ServerPlayer target) {
        if (!joined) return;
        if (target == null) return;
        PENDING.add(new Warning(target, K3W_WARNINGS.get(new Random().nextInt(K3W_WARNINGS.size()))));
    }

    public static void join(ServerLevel level) {
        if (joined || everJoined) return;
        joined = true;
        everJoined = true;
        joinedDay = level.getDayTime() / 24000L;
        for (var p : level.getServer().getPlayerList().getPlayers()) {
            addFake(p);
        }
        level.playSound(null, 0, 0, 0, net.minecraft.sounds.SoundEvents.AMBIENT_CAVE.get(),
                net.minecraft.sounds.SoundSource.MASTER, 6.0f, 0.4f);
    }

    public static void forceJoin(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        if (joined) return;
        joined = true;
        everJoined = true;
        joinedDay = level.getDayTime() / 24000L;
        for (var p : level.getServer().getPlayerList().getPlayers()) {
            addFake(p);
        }
    }

    public static void forceLeave(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        if (!joined) return;
        leave(level);
    }

    private static void leave(ServerLevel level) {
        String msg = FAREWELLS.get(new Random().nextInt(FAREWELLS.size()));
        for (var p : level.getServer().getPlayerList().getPlayers()) {
            if (p.connection != null) {
                p.connection.send(new ClientboundSystemChatPacket(
                        Component.literal("<" + displayName + "> " + msg).withStyle(ChatFormatting.LIGHT_PURPLE), false));
            }
            removeFake(p);
        }
        joined = false;
        leftDay = level.getDayTime() / 24000L;
        PENDING.clear();
    }

    private static void addFake(ServerPlayer target) {
        GameProfile profile = new GameProfile(sisterUuid(), displayName);
        var entry = new ClientboundPlayerInfoUpdatePacket.Entry(sisterUuid(), profile, true, 1,
                GameType.SURVIVAL, Component.literal(displayName), null);
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

    private static void removeFake(ServerPlayer target) {
        target.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(sisterUuid())));
    }

    @SubscribeEvent
    public static void onPlayerLogin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (joined && event.getEntity() instanceof ServerPlayer sp && sp.connection != null) {
            addFake(sp);
        }
    }

    public static boolean isJoined() {
        return joined;
    }
}
