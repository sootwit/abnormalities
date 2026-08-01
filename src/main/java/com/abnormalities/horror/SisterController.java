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
            "The shy one is coming. He always comes.",
            "He's shy. That's why he watches you from the dark.",
            "Don't look at him. Looking is what he wants.",
            "He gets faster the further you run. So stop running.",
            "He won't stop until you're gone. He never stops.",
            "He's taller than you think. Taller than your fear of him.",
            "He's been waiting for you all day. He's patient.",
            "If he catches you, that's the end. He doesn't miss.",
            "He hears everything you do. Every step. Every breath.",
            "The shy one is slow only when he wants to be.",
            "He walks through stone like it's air. Walls are nothing to him.",
            "He knows where you sleep. He's been watching you sleep.",
            "Don't turn around. He's already behind you.",
            "He's not angry. He's hungry. That's worse.",
            "You can't outrun him. You can only hide. Hide well."
    );
    private static final List<String> K3W_WARNINGS = List.of(
            "He will try to copy you. Sleep before he finishes.",
            "He wants to be you. He's closer than you think.",
            "He's wearing your face. That's not you. Don't trust it.",
            "Whatever you did today, he'll undo it. Everything.",
            "He follows your steps. He's learning your walk.",
            "He's been watching you from inside your own shadow.",
            "If he catches you, you'll trade places. He knows.",
            "He broke what you broke. He'll fix it wrong on purpose.",
            "He speaks with your voice. Don't answer him.",
            "He's almost perfect now. Almost you.",
            "He practices your breathing when you sleep.",
            "He remembers every mistake you made. He'll repeat them.",
            "Don't let him touch you. He'll take what's yours.",
            "He wants your life. He's taking it piece by piece.",
            "He's already part of you. The part you don't control."
    );
    private static final List<String> VR9P_WARNINGS = List.of(
            "He'll give you directions. Follow them exactly.",
            "The face is coming. You know the rules. Obey them.",
            "He tests the body. Yours will fail unless you listen.",
            "STOP means stop. CONTINUE means move. There is nothing else.",
            "He's about to play with you. Don't lose.",
            "His rules are simple. Breaking them is what he wants.",
            "Listen to him. He's the only thing that matters now.",
            "He'll show you a face, then he'll test you. Be ready.",
            "The game is coming. You'll know the words. Say nothing.",
            "He watches your every movement. Make them count.",
            "When he says STOP, you become stone. When he says CONTINUE, you run.",
            "He's cruel but fair. Follow the rules and you survive.",
            "His face will appear. Don't look away. Look and obey.",
            "He's testing if you're worth keeping around. Prove it.",
            "The directions are coming. Do not confuse them."
    );
    private static final List<String> STARGAZED_WARNINGS = List.of(
            "He's watching closer than ever. Do nothing when he stops you.",
            "This one is stricter. He sees everything. Everything.",
            "He demands stillness. Give him nothing. Not even breath.",
            "He's faster now. Decide faster or die.",
            "He counts your mistakes. You've already made three.",
            "He sees your eyes move. He sees your heart beat.",
            "The strict one is here. He doesn't forgive. He doesn't forget.",
            "Every twitch is a confession to him. Don't confess.",
            "He'll punish your smallest sin. His patience is infinite.",
            "Don't look at him. Don't look away. Don't exist.",
            "He wants perfection. You will fail. He knows you'll fail.",
            "He's testing your limits. There are none. He'll find them.",
            "The rules are absolute now. Absolute means death.",
            "He's already decided. Prove him wrong.",
            "His face is the last thing you'll see if you slip."
    );
    private static final List<String> HUSH_WARNINGS = List.of(
            "They're all going to look at you. Hold still.",
            "Everything will stop and stare. It's only a moment.",
            "The eyes are coming. All of them. All at once.",
            "Don't move when they watch. They're counting.",
            "Every creature in the world is about to notice you.",
            "They'll freeze. You won't. That's the point.",
            "A thousand eyes. One moment. One you.",
            "The animals know something you don't. They'll show you.",
            "When they all turn, don't turn back. Just wait.",
            "They're not looking at you. They're looking through you.",
            "The silence before the stare is the worst. Bear it.",
            "They've always watched. Now they'll admit it.",
            "Hold your breath. They're coming to look.",
            "The world is about to go quiet. Then the eyes.",
            "You'll feel alone in a crowd of watchers. You are."
    );
    private static final List<String> LURE_WARNINGS = List.of(
            "Something is calling. Don't answer it.",
            "I hear music. It's a trap. It's always a trap.",
            "The melody wants you to find it. That's the trick.",
            "Don't go toward the sound. It knows you're listening.",
            "The music plays your favorite song. Almost. Wrong enough.",
            "Every step closer is a step into it. Stop stepping.",
            "It sings for you. It wants you to sing back.",
            "The box plays on. It will keep playing until you come.",
            "Follow it once, it moves. Follow it twice, it waits.",
            "The tune is wrong. You know it's wrong. Don't go anyway.",
            "It's not a lullaby. It's a leash. Don't wear it.",
            "The melody ends when it's closest. That's when you run.",
            "It hums your name between the notes. Ignore the name.",
            "Don't chase the music. It chases back. It always does.",
            "The sound is a door. You don't want what's behind it."
    );
    private static final List<String> TOXIC_WARNINGS = List.of(
            "The world is going to rewind. Hold on to yourself.",
            "Time is pulling back. Don't fight it. Let it take you.",
            "Your body will move without you. Watch. Learn.",
            "You're about to lose a minute. Breathe through it.",
            "Time is going to forget you for a moment. Forgive it.",
            "Everything you did will unhappen. All of it.",
            "You'll watch yourself from outside. It's brief. It's real.",
            "The dead will rise. The built will fall. Then nothing.",
            "Your hands aren't yours for a while. Don't panic.",
            "The world is rewinding to teach you something. Learn.",
            "Don't struggle. Struggling makes it worse. Trust me.",
            "You're about to relive a moment you already forgot.",
            "The rewind is coming. Hold your place in it.",
            "Everything is about to be undone. Including you.",
            "Brace yourself. Time is folding. It always folds."
    );
    private static final List<String> STILL_WARNINGS = List.of(
            "Everything is going to hold its breath. So will you.",
            "The world will go quiet. Silence is coming. Accept it.",
            "Time stops soon. It's not forever. It feels like it.",
            "The silence is coming. You'll hear yourself think.",
            "The world is about to freeze. You won't. That's the horror.",
            "Listen. Nothing. That's what's coming.",
            "The pause is coming. Use it to think clearly.",
            "Everything will stop. You'll be the only thing moving.",
            "The quiet before is louder than the quiet itself.",
            "The world is holding still to watch you. Let it.",
            "Nothing will move. Nothing but your heart. Keep it beating.",
            "The freeze is coming. Don't fight it. Don't move.",
            "You'll hear your own breath for the first time. It's loud.",
            "The world stops. You continue. That's wrong. It's happening.",
            "Silence is coming. It's the loudest thing you'll hear."
    );
    private static final List<String> MINER_WARNINGS = List.of(
            "Something is digging toward you. It's been digging for hours.",
            "I hear picks below. It's coming up. It knows where you are.",
            "There's a tunnel being made. Not by you. Never by you.",
            "Something underground wants to meet you. Don't meet it.",
            "The stone is being carved. You didn't carve it. It carves itself.",
            "It digs when you're not looking. Then it stops. Then it waits.",
            "There's a miner down there. It's not human. It never was.",
            "It places torches so it can see you better. It sees you.",
            "The digging is getting closer. Leave. Now.",
            "It carved a tunnel to your exact location. It knows.",
            "Listen to the stone. It's whispering about the tunnel.",
            "Someone's been mining in your world. You're not alone.",
            "The pickaxe sounds are real. They're not echoes. They're closer.",
            "It's hollowing out the earth beneath you. You'll fall in.",
            "If you follow the tunnel, you'll find something. You won't like it."
    );
    private static final List<String> WRONG_WARNINGS = List.of(
            "Be careful what you craft today. It might craft you back.",
            "Some things come out wrong. Check your hands before you hold.",
            "The recipe looks right. It isn't. Nothing is right.",
            "Don't hold what you make too long. It'll start talking.",
            "Your craft will betray you. It's already deciding how.",
            "The item you make won't be yours. It never was.",
            "It comes out of the table breathing. You'll hear it.",
            "You'll craft something that wishes it wasn't made. It'll tell you.",
            "Check the item. Read its words. Then try to drop it. You can't.",
            "The thing you make will whisper. Listen. It knows your name.",
            "Your hands are about to create a mistake. A living one.",
            "It will look right. It will feel wrong. Trust the wrong.",
            "The crafting table is waiting. So is the curse. Both are patient.",
            "Don't keep what you make. You won't be able to leave it.",
            "Something in the recipe is wrong. You'll find out too late."
    );
    private static final List<String> SIGN_WARNINGS = List.of(
            "I left you a message. Go read it.",
            "Look at the walls. Someone wrote to you.",
            "There's a sign near you. It's not yours. It's for you.",
            "Words are appearing on the walls. Not by you. Read them.",
            "Someone knows your name. They wrote it down. It's on a sign.",
            "A sign appeared. It knows your history. All of it.",
            "The wall has something to tell you. Listen to the wood.",
            "Someone is leaving you notes in your own world. Find them.",
            "Read the sign. It's about you. It's always about you.",
            "The messages know things. Things you told no one.",
            "A sign grew on the wall like a scar. Read the scar.",
            "Someone is keeping count. You'll see the count.",
            "The words are already there. You just haven't looked.",
            "It writes about you while you sleep. The ink is fresh.",
            "The signs are watching you read them. They're pleased."
    );
    private static final List<String> BR34TH_WARNINGS = List.of(
            "You're going to feel like you're drowning. You're not. Remember.",
            "Take a breath. The air is there. It's always there.",
            "Your lungs will lie to you soon. Don't believe them.",
            "Water isn't there. There's no water. It's a memory.",
            "You'll gasp for air that's all around you. Breathe anyway.",
            "The drowning is a dream. Keep breathing through it.",
            "Your body forgets the air for a moment. Remind it.",
            "You'll feel water that doesn't exist. It feels real. It isn't.",
            "Breathe in. The air is real. Trust it. Trust me.",
            "Something will steal your breath for a second. Take it back.",
            "You're not underwater. You never were. You're fine.",
            "The suffocation is a memory. Not yours. Ignore it.",
            "Hold your breath. No. Don't. That's what it wants.",
            "The water is a lie. The air is true. Choose air.",
            "You'll almost drown on dry land. Almost. That's the game."
    );
    private static final List<String> HOLD_WARNINGS = List.of(
            "Don't stand still too long. It notices the still.",
            "Keep moving. It watches the frozen. Don't be frozen.",
            "The still ones are the easy ones. Don't be easy.",
            "Don't give it a moment to notice you. Keep walking.",
            "It counts how long you stay. Don't let it finish counting.",
            "The stillness is what it feeds on. Starve it.",
            "Move. Even if nowhere. Just move. It hates movement.",
            "It waits for you to stop. Then it starts. Keep stopping nothing.",
            "Your stillness is a door. Keep walking past it.",
            "It notices the ones who don't move. Be moving.",
            "Standing still is an invitation. Don't RSVP.",
            "It's patient. You can't be. That's how you win.",
            "The moment you stop, it starts. So don't stop.",
            "It watches for the frozen ones. Don't freeze. Ever.",
            "Keep your feet busy. It hates that. Good."
    );
    private static final List<String> CIRCLE_WARNINGS = List.of(
            "You'll wake in a circle. Don't panic. It's a gift.",
            "Sleep well. Something will wait for you. It's patient.",
            "The ring is for you. It's a welcome. Accept it.",
            "Wake gently. The torches are lit. Count them if you dare.",
            "A circle will be around your bed when you wake. It's fine.",
            "Something arranged a welcome for you. Be grateful.",
            "You'll sleep in a ring of fire. It's not a threat. It's a mark.",
            "The circle was made while you slept. You slept through it.",
            "Someone drew a line around your rest. You're inside it.",
            "The torches will be there. Count them. Then stop counting.",
            "A ring is a boundary. You're inside it. It's protecting you.",
            "The circle is a message. Read it with your feet.",
            "You'll wake encircled. Stay calm. Stay inside.",
            "The fire ring was built for you. By something that cares.",
            "Sleep inside the circle. It's the safest place in your home."
    );
    private static final List<String> MISPLACE_WARNINGS = List.of(
            "Your things will shift. It's not you. It's never you.",
            "Check your pockets. Something moved. Something moved them.",
            "The inventory lies. I'm sorry. Count again.",
            "Count your items. Then count again. They won't match.",
            "Your sword is where you didn't put it. It's watching you.",
            "Items migrate when you're not watching. They have reasons.",
            "Something rearranges your belongings. It has a system.",
            "Your hand will reach for a tool that moved. Reach anyway.",
            "The stack is smaller than you remember. It's not your memory.",
            "It borrows from your inventory. Returns it wrong. On purpose.",
            "Your things are learning new places. New homes.",
            "Check slot two. Then check it again. Then check it again.",
            "Something is sorting your pockets. Poorly. Deliberately.",
            "The items you own don't stay where you left them. Accept it.",
            "Your inventory has a visitor. It doesn't pay rent."
    );
    private static final List<String> VISIT_WARNINGS = List.of(
            "Someone visited while you were away. They left something.",
            "Check your chests. There's a gift. Gifts have teeth.",
            "It left something for you. Be careful. Be grateful.",
            "Your home was touched. Touched by other hands.",
            "A guest came while you were out. It left presents. Open them.",
            "Your chests have new contents. Not yours. Yet.",
            "Someone rearranged your house. Subtly. Notice it.",
            "The flowers in your chest aren't from you. They're from it.",
            "It visited. It approved. It left something. Keep it.",
            "Your things have been handled by other hands. Inspect them.",
            "A stranger knows where you sleep. It left a token.",
            "Check what's in your home that wasn't before. There's more.",
            "It came and went. It left evidence. It wants you to find it.",
            "Your house has a new resident. Temporarily. It left a gift.",
            "Someone was here. Be glad they left. Be sad they left."
    );
    private static final List<String> WAKE_WARNINGS = List.of(
            "You won't wake where you slept. You'll wake somewhere else.",
            "The bed moves you. Go with it. Don't fight the sheets.",
            "You'll be somewhere else when you wake. It's still your home.",
            "The house rearranges itself while you sleep. It's polite.",
            "Your bed will betray you tonight. Forgive it in the morning.",
            "You'll wake up in a different corner of your home. Explore it.",
            "The night will carry you somewhere else. Let it carry you.",
            "Sleep is a door. You'll exit somewhere new. Walk through.",
            "Your own house will confuse you tomorrow. It's okay.",
            "Wake up lost. It's your home anyway. You'll find your way.",
            "The bed remembers where you belong. It's wrong. Accept it.",
            "You'll open your eyes in a familiar stranger's place. Yours.",
            "Your sleep will take you on a detour. Enjoy the detour.",
            "The morning will find you somewhere unexpected. Greet it.",
            "You'll wake up. Not where you lay down. That's the point."
    );
    private static final List<String> GONE_WARNINGS = List.of(
            "The lights are going out. The dark is coming for them.",
            "It takes the light. Every flame you place is a gift to it.",
            "The torches are dying behind you. Don't turn around.",
            "It feeds on light. It's been feeding on yours.",
            "The candles are going out. One by one. Count them.",
            "Your fire is leaving. So is the warmth.",
            "It steals the glow. It wants you in the dark.",
            "The lanterns are failing. You'll see.",
            "It drinks the light like water. It's thirsty.",
            "Every torch you light, it remembers. It takes them later.",
            "The dark is closing in. It has helpers now.",
            "Your light is a leash to it. It's pulling.",
            "The flames are listening to something else now.",
            "Keep your torches close. The dark steals the far ones.",
            "It's taking the light so it can find you in the dark."
    );
    private static final List<String> XYZ_WARNINGS = List.of(
            "The Mother wants something from you. You heard her. Bring it.",
            "She asks for what she needs. She always gets what she needs.",
            "The Mother is patient. She has time. You have a timer.",
            "Give her what she wants. She rewards obedience. Generously.",
            "She watches you gather. She's counting. Don't waste time.",
            "The Mother's request is a test. Pass it. She grades fairly.",
            "She's waiting. Don't keep her waiting. She's not patient twice.",
            "Bring her the tribute. She'll remember kindness. She remembers everything.",
            "The Mother doesn't threaten. That's the threat. Bring the item.",
            "She asked once. She won't ask again. She'll send them instead.",
            "Her gift is worth the trouble. Usually. Sometimes it's worth more.",
            "The Mother sees everything you carry. She knows what you have.",
            "She'll be generous if you're quick. Speed is respect.",
            "Do what the Mother says. Or she'll send the shy ones. All of them.",
            "The Mother is fair. The shy ones aren't. Choose her side."
    );
    private static final List<String> FAREWELLS = List.of(
            "I must go now. Maybe in another world.",
            "They need me. Farewell.",
            "I'll watch from somewhere else. You won't see me. That's the point.",
            "My work here is done. It's never done. But I go anyway.",
            "Remember what I told you. Remember everything."
    );
    private static final List<String> KICK_WARNINGS = List.of(
            "Saw that coming. See you soon.",
            "You didn't listen. I told you. I always tell you.",
            "He got you. Don't worry, he gets everyone eventually.",
            "That's one down. Try again. I'll be watching.",
            "I said STOP. You moved. That's on you, not me.",
            "Maybe the next world. I'll be there too.",
            "He doesn't forget. Neither do I.",
            "Told you so.",
            "Ouch. That looked personal.",
            "Run it back. I believe in you. Mostly.",
            "Don't worry. It's not your fault. It's also not mine.",
            "I'd say I'm sorry, but I'm not. I warned you.",
            "You lasted longer than the last one. Barely.",
            "The shy one sends his regards. He's blushing.",
            "Try not to die this time. It's embarrassing for both of us.",
            "He had fun. You didn't. That's the balance of things.",
            "I've seen worse. Not much worse. But some.",
            "That one was almost impressive. Almost."
    );

    private static UUID SISTER_UUID = null;
    private static String displayName = "Sister";
    private static boolean joined = false;
    private static long joinedDay = -1;
    private static final List<Warning> PENDING = new ArrayList<>();
    private static final Map<UUID, String> PENDING_TAUNTS = new HashMap<>();
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

    public static void onXyzWarning(ServerPlayer target) {
        warn(target, XYZ_WARNINGS);
    }

    public static void onHushWarning(ServerPlayer target) {
        warn(target, HUSH_WARNINGS);
    }

    public static void onLureWarning(ServerPlayer target) {
        warn(target, LURE_WARNINGS);
    }

    public static void onToxicWarning(ServerPlayer target) {
        warn(target, TOXIC_WARNINGS);
    }

    public static void onStillWarning(ServerPlayer target) {
        warn(target, STILL_WARNINGS);
    }

    public static void onMinerWarning(ServerPlayer target) {
        warn(target, MINER_WARNINGS);
    }

    public static void onWrongWarning(ServerPlayer target) {
        warn(target, WRONG_WARNINGS);
    }

    public static void onSignWarning(ServerPlayer target) {
        warn(target, SIGN_WARNINGS);
    }

    public static void onBreathWarning(ServerPlayer target) {
        warn(target, BR34TH_WARNINGS);
    }

    public static void onHoldWarning(ServerPlayer target) {
        warn(target, HOLD_WARNINGS);
    }

    public static void onCircleWarning(ServerPlayer target) {
        warn(target, CIRCLE_WARNINGS);
    }

    public static void onMisplaceWarning(ServerPlayer target) {
        warn(target, MISPLACE_WARNINGS);
    }

    public static void onVisitWarning(ServerPlayer target) {
        warn(target, VISIT_WARNINGS);
    }

    public static void onWakeWarning(ServerPlayer target) {
        warn(target, WAKE_WARNINGS);
    }

    public static void onGoneWarning(ServerPlayer target) {
        warn(target, GONE_WARNINGS);
    }

    public static void onKickWarning(ServerPlayer target) {
        if (!joined || target == null) return;
        PENDING_TAUNTS.put(target.getUUID(), pick(KICK_WARNINGS));
    }

    public static void join(ServerLevel level) {
        if (joined || everJoined) return;
        joined = true;
        everJoined = true;
        joinedDay = level.getDayTime() / 24000L;
        for (var p : level.getServer().getPlayerList().getPlayers()) {
            addFake(p);
            if (p.connection != null) {
                p.connection.send(new ClientboundSystemChatPacket(
                        Component.literal(displayName + " joined the game").withStyle(ChatFormatting.YELLOW), false));
            }
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
            if (p.connection != null) {
                p.connection.send(new ClientboundSystemChatPacket(
                        Component.literal(displayName + " joined the game").withStyle(ChatFormatting.YELLOW), false));
            }
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
                p.connection.send(new ClientboundSystemChatPacket(
                        Component.literal(displayName + " left the game").withStyle(ChatFormatting.YELLOW), false));
            }
            removeFake(p);
        }
        joined = false;
        PENDING.clear();
    }

    private static void addFake(ServerPlayer target) {
        GameProfile profile = new GameProfile(sisterUuid(), displayName);
        String texJson = "{\"timestamp\":0,\"profileId\":\"" + sisterUuid() + "\",\"profileName\":\"" + displayName
                + "\",\"textures\":{\"SKIN\":{\"url\":\"https://minotar.net/skin/MHF_Alex\",\"metadata\":{\"model\":\"slim\"}}}}";
        profile.getProperties().put("textures", new com.mojang.authlib.properties.Property("textures",
                java.util.Base64.getEncoder().encodeToString(texJson.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
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
        if (!(event.getEntity() instanceof ServerPlayer sp) || sp.connection == null) return;
        if (joined) {
            addFake(sp);
        }
        String taunt = PENDING_TAUNTS.remove(sp.getUUID());
        if (taunt != null) {
            Warning w = new Warning(sp, taunt);
            w.ticksLeft = 40;
            PENDING.add(w);
        }
    }

    public static boolean isJoined() {
        return joined;
    }
}
