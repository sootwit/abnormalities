package com.abnormalities.horror;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abnormalities.config.AbnormalitiesConfig;

public class SisterChatResponder {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|SisterChat");

    private static final int DELAY_MIN = 40;
    private static final int DELAY_MAX = 100;
    private static final int CREEPY_DELAY_MIN = 100;
    private static final int CREEPY_DELAY_MAX = 160;

    private static final List<String> GREETING_LINES = List.of(
        "Hello.",
        "You talk to me first. Most don't.",
        "Hi.",
        "Hey.",
        "Late for pleasantries. But hello."
    );

    private static final List<String> NAME_LINES = List.of(
        "I'm here.",
        "You called.",
        "Yes.",
        "Still here.",
        "I heard you the first time.",
        "That's my name.",
        "Sister. That's what they call me too.",
        "Right here.",
        "You sound unsure. Fair."
    );

    private static final List<String> IDENTITY_LINES = List.of(
        "I watch. That's most of it.",
        "Sister.",
        "Someone who's been here longer than you.",
        "Not one of them. Not exactly on your side either.",
        "I used to be simpler. Long story.",
        "I know things. Rest is useless.",
        "I'm the one who tells you things. Not the one who saves you.",
        "You'll figure out the rest.",
        "Doesn't matter what I am. What matters is I know."
    );

    private static final List<String> LOCATION_LINES = List.of(
        "Close.",
        "Behind the camera.",
        "Somewhere you already looked.",
        "Not far.",
        "Here. Vague, I know.",
        "Wherever the screen is.",
        "Same room as you. Different side of it.",
        "Watching.",
        "Closer than the walls.",
        "You won't find me. Stop looking."
    );

    private static final List<String> FEAR_LINES = List.of(
        "Good. Fear keeps you moving.",
        "I can't help. I can watch.",
        "Fear's doing its job. Keep it.",
        "That's the right feeling for this.",
        "Fear is expected.",
        "Nobody's coming. Move anyway.",
        "You should be.",
        "Fear's useful. Waste it and you're done.",
        "I hear you. Doesn't change anything.",
        "Stay afraid. Stay moving."
    );

    private static final Map<String, List<String>> ENTITY_LINES = new LinkedHashMap<>();
    static {
        ENTITY_LINES.put("nur", List.of(
            "Nur's shy. Don't stare at him. Wait for the sunlight.",
            "Nur doesn't like being seen by you. He's afraid of the day."
        ));
        ENTITY_LINES.put("k3w", List.of(
            "K3w's you. The worse version. Be careful of your actions and sleep.",
            "K3w learns fast. He's afraid of sleeping. So sleep."
        ));
        ENTITY_LINES.put("it", List.of(
            "It doesn't blink. Neither should you.",
            "It waits. Stare at it."
        ));
        ENTITY_LINES.put("him", List.of(
            "Him. Yeah. He will be an inconvenience to you. Fight him.",
            "Him. He never really leaves. Fight him until he's gone."
        ));
        ENTITY_LINES.put("xyz", List.of(
            "xYz tries to help you. Appreciate it.",
            "xYz requires items. Give them to her."
        ));
    }

    private static final List<String> HINT_LINES = List.of(
        "Don't run out of light.",
        "Quiet rooms stay quiet. Keep them that way.",
        "If it's staring, stare back. Don't blink first.",
        "Bridges don't outrun anything. Walk, don't build.",
        "Sleep in beds nobody's visited.",
        "Watch the ones that watch back. They freeze when you do.",
        "Torches buy time. Not much.",
        "Listen for music you didn't turn on.",
        "If the mobs stop moving, keep moving.",
        "Survive by noticing. That's most of it."
    );

    private static final List<String> RUN_LINES = List.of(
        "Go ahead.",
        "Running works.",
        "He likes it when you run. Gives him something to do.",
        "Run. I'll watch.",
        "Escape isn't a place. Just a direction.",
        "Fast doesn't mean far.",
        "You can run. He can wait.",
        "Run if it helps you feel better.",
        "Flee. Same result, more effort.",
        "That's the instinct. Not the plan."
    );

    private static final List<String> DEFIANT_LINES = List.of(
        "Sure.",
        "Everyone says that once.",
        "Confidence. Cute.",
        "I've heard that before. Before they arrived.",
        "Bring it. Not my line to deliver.",
        "You'll change your mind. Most do.",
        "Not scared yet.",
        "You're brave."
    );

    private static final List<String> META_LINES = List.of(
        "Abnormalities. Fitting name.",
        /* "There's a wiki. Won't save you either.", */
        "Mod. Sure. Call it that.",
        /* "You can read about it. Doesn't change what's in here.", */
        "Abnormalities. Someone documented all this.",
        /* "Wiki's got the facts. Not the feeling.", */
        "It's a mod. It's also real enough.",
        /* "Go read it. Come back scared anyway.", */
        /* "Names and numbers. The wiki has those.", */
        "Call it a mod if that helps you sleep."
    );

    private static final List<String> CREEPY_LINES = List.of(
        "Stop talking.",
        "They hear you.",
        "Quiet.",
        "That's enough.",
        "Shh."
    );

    private static final Random RNG = new Random();
    private static final Map<UUID, String> LAST_MSG = new HashMap<>();
    private static final Map<UUID, Integer> REPEAT_COUNT = new HashMap<>();

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player.connection == null) return;
        if (!SisterController.isJoined()) return;
        if (!AbnormalitiesConfig.SISTER_CHAT_ENABLED.get()) return;

        UUID uuid = player.getUUID();
        String msg = event.getMessage().getString().trim().toLowerCase(Locale.ROOT);
        if (msg.isEmpty()) return;

        String lastMsg = LAST_MSG.getOrDefault(uuid, "");
        if (msg.equals(lastMsg)) {
            REPEAT_COUNT.merge(uuid, 1, Integer::sum);
        } else {
            REPEAT_COUNT.put(uuid, 0);
        }
        LAST_MSG.put(uuid, msg);

        if (!SisterCooldownManager.canChatReply(uuid)) return;

        List<String> responsePool = matchTrigger(msg);
        if (responsePool != null) {
            scheduleChat(player, responsePool);
            return;
        }

        int repeats = REPEAT_COUNT.getOrDefault(uuid, 0);
        if (repeats >= 3 && RNG.nextInt(100) < AbnormalitiesConfig.SISTER_CHAT_CREEPY_CHANCE.get()) {
            scheduleChat(player, CREEPY_LINES);
            return;
        }

        if (RNG.nextInt(100) < AbnormalitiesConfig.SISTER_CHAT_SILENCE_CHANCE.get()) return;
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        LAST_MSG.remove(uuid);
        REPEAT_COUNT.remove(uuid);
    }

    private static List<String> matchTrigger(String msg) {
        if (msg.contains("hello") || msg.equals("hi") || msg.equals("hey")) return GREETING_LINES;
        if (msg.equals("sister") || msg.equals("sister?")) return NAME_LINES;
        if (msg.contains("who are you") || msg.contains("what are you") || msg.contains("what is sister")) return IDENTITY_LINES;
        if (msg.contains("where are you") || msg.contains("where is sister") || msg.equals("where")) return LOCATION_LINES;
        if (msg.contains("not scared") || msg.contains("not afraid") || msg.equals("come at me") || msg.equals("bring it")) return DEFIANT_LINES;
        if (msg.equals("help") || msg.contains("scared") || msg.contains("afraid") || msg.equals("help me") || msg.equals("i'm scared")) return FEAR_LINES;
        for (var entry : ENTITY_LINES.entrySet()) {
            if (msg.contains(entry.getKey())) return entry.getValue();
        }
        if (msg.equals("hint") || msg.equals("tip") || msg.contains("what should i do") || msg.contains("how do i survive")) return HINT_LINES;
        if (msg.equals("run") || msg.equals("run away") || msg.equals("flee") || msg.equals("escape")) return RUN_LINES;
        if (/* msg.contains("wiki") ||  */msg.equals("mod") || msg.contains("abnormalities") || msg.contains("what mod")) return META_LINES;
        return null;
    }

    private static void scheduleChat(ServerPlayer player, List<String> pool) {
        String msg = pool.get(RNG.nextInt(pool.size()));
        int delay = DELAY_MIN + RNG.nextInt(DELAY_MAX - DELAY_MIN);
        SisterCooldownManager.scheduleChat(player, msg, delay);
    }
}
