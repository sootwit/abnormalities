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
    private static final List<String> VR9P_WARNINGS = List.of(
            "He's going to test you. Move when he says CONTINUE, freeze when he says STOP.",
            "The face is coming. Remember the rules.",
            "He will ask for stillness. Give him stillness.",
            "He tests the body. Don't fail."
    );
    private static final List<String> STARGAZED_WARNINGS = List.of(
            "He's watching closely. Do NOTHING when he stops you.",
            "He will ask for silence. Give him nothing.",
            "This one is stricter. Don't blink.",
            "He wants perfection. You will fail."
    );
    private static final List<String> HUSH_WARNINGS = List.of(
            "They're all going to look at you. Don't be scared.",
            "Everything will stop and stare. It's just a moment.",
            "The eyes are coming. All of them.",
            "Don't move when they watch."
    );
    private static final List<String> LURE_WARNINGS = List.of(
            "Something is calling. Don't answer.",
            "I hear music. You shouldn't follow it.",
            "The melody is a trap. It always is.",
            "Don't go toward the sound."
    );
    private static final List<String> TOXIC_WARNINGS = List.of(
            "The world is going to rewind. Hold on.",
            "Something is pulling time back. Don't fight it.",
            "Your body will move without you. Let it.",
            "You're about to lose a minute. Breathe."
    );
    private static final List<String> STILL_WARNINGS = List.of(
            "Everything is going to hold its breath.",
            "The world will go quiet. Breathe.",
            "Time stops soon. It's not forever.",
            "The silence is coming."
    );
    private static final List<String> MINER_WARNINGS = List.of(
            "Something is digging toward you.",
            "I hear picks below. It's coming up.",
            "There's a tunnel being made. Not by you.",
            "Something underground wants to meet you."
    );
    private static final List<String> WRONG_WARNINGS = List.of(
            "Be careful what you craft today.",
            "Some things come out wrong. Check your hands.",
            "The recipe looks right. It isn't.",
            "Don't hold what you make too long."
    );
    private static final List<String> SIGN_WARNINGS = List.of(
            "I left you a message.",
            "Look at the walls.",
            "Someone wrote to you. Read it.",
            "There's a sign near you. Not yours."
    );
    private static final List<String> BR34TH_WARNINGS = List.of(
            "You're going to feel like you're drowning. You're not.",
            "Take a breath. It's not real.",
            "Your lungs will lie to you soon.",
            "Water isn't there. Remember that."
    );
    private static final List<String> HOLD_WARNINGS = List.of(
            "Don't stand still too long.",
            "Keep moving. It watches the still.",
            "The still ones are the easy ones.",
            "Don't give it a moment to notice you."
    );
    private static final List<String> CIRCLE_WARNINGS = List.of(
            "You'll wake in a circle. Don't panic.",
            "Sleep well. Something will wait.",
            "The ring is for you. It's a gift.",
            "Wake gently. The torches are lit."
    );
    private static final List<String> MISPLACE_WARNINGS = List.of(
            "Your things will shift. It's not you.",
            "Check your pockets. Something moved.",
            "The inventory lies. I'm sorry.",
            "Count your items. Then count again."
    );
    private static final List<String> VISIT_WARNINGS = List.of(
            "Someone visited while you were away.",
            "Check your chests. There's a gift.",
            "It left something for you. Be careful.",
            "Your home was touched."
    );
    private static final List<String> WAKE_WARNINGS = List.of(
            "You won't wake where you slept.",
            "The bed moves you. Go with it.",
            "You'll be somewhere else when you wake.",
            "The house rearranges itself."
    );
    private static final List<String> FAREWELLS = List.of(
            "I must go now. Maybe in another world.",
            "They need me. Farewell."
    );

    private static UUID SISTER_UUID = null;
    private static String displayName = "Sister";
    private static boolean joined = false;
    private static long joinedDay = -1;
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

    private static String pick(List<String> pool) {
        return pool.get(new Random().nextInt(pool.size()));
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

    public static void warn(ServerPlayer target, List<String> pool) {
        if (!joined || target == null) return;
        PENDING.add(new Warning(target, pick(pool)));
    }

    public static void warnNow(ServerPlayer target, List<String> pool) {
        if (!joined || target == null) return;
        if (target.connection != null) {
            target.connection.send(new ClientboundSystemChatPacket(
                    Component.literal("<" + displayName + "> " + pick(pool)).withStyle(ChatFormatting.LIGHT_PURPLE), false));
        }
    }

    public static void onNurWarning(ServerPlayer target) {
        warn(target, NUR_WARNINGS);
    }

    public static void onK3wWarning(ServerPlayer target) {
        warn(target, K3W_WARNINGS);
    }

    public static void onVr9pWarning(ServerPlayer target, boolean stargazed) {
        warnNow(target, stargazed ? STARGAZED_WARNINGS : VR9P_WARNINGS);
    }

    public static void onHushWarning(ServerPlayer target) {
        warnNow(target, HUSH_WARNINGS);
    }

    public static void onLureWarning(ServerPlayer target) {
        warnNow(target, LURE_WARNINGS);
    }

    public static void onToxicWarning(ServerPlayer target) {
        warnNow(target, TOXIC_WARNINGS);
    }

    public static void onStillWarning(ServerPlayer target) {
        warnNow(target, STILL_WARNINGS);
    }

    public static void onMinerWarning(ServerPlayer target) {
        warnNow(target, MINER_WARNINGS);
    }

    public static void onWrongWarning(ServerPlayer target) {
        warn(target, WRONG_WARNINGS);
    }

    public static void onSignWarning(ServerPlayer target) {
        warnNow(target, SIGN_WARNINGS);
    }

    public static void onBreathWarning(ServerPlayer target) {
        warnNow(target, BR34TH_WARNINGS);
    }

    public static void onHoldWarning(ServerPlayer target) {
        warnNow(target, HOLD_WARNINGS);
    }

    public static void onCircleWarning(ServerPlayer target) {
        warnNow(target, CIRCLE_WARNINGS);
    }

    public static void onMisplaceWarning(ServerPlayer target) {
        warn(target, MISPLACE_WARNINGS);
    }

    public static void onVisitWarning(ServerPlayer target) {
        warn(target, VISIT_WARNINGS);
    }

    public static void onWakeWarning(ServerPlayer target) {
        warnNow(target, WAKE_WARNINGS);
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
        String msg = pick(FAREWELLS);
        for (var p : level.getServer().getPlayerList().getPlayers()) {
            if (p.connection != null) {
                p.connection.send(new ClientboundSystemChatPacket(
                        Component.literal("<" + displayName + "> " + msg).withStyle(ChatFormatting.LIGHT_PURPLE), false));
            }
            removeFake(p);
        }
        joined = false;
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
