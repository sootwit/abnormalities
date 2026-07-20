package com.abnormalities;

import com.abnormalities.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

import java.util.List;
import java.util.Random;

public class WhisperManager {
    private static final Random RNG = new Random();

    private static final List<String> FRAGMENTS = List.of(
        "don't...", "help...", "behind...", "watching...", "alone...",
        "stay...", "close your...", "it knows...", "breathe...",
        "dreaming...", "awake...", "never...", "always...", "find...",
        "hide...", "run...", "stay still...", "listen...", "don't move..."
    );

    private static final List<String> LOW_REP_FRAGMENTS = List.of(
        "you're worthless...", "no one cares...", "why bother...",
        "they left you...", "you deserve this...", "pathetic...",
        "scream... no one hears...", "give up...", "it's over...",
        "you failed...", "lost... forgotten...", "nothing matters..."
    );

    private static final List<String> HIGH_REP_FRAGMENTS = List.of(
        "it trusts you...", "you are not alone...", "it watches over...",
        "keep going...", "almost there...", "it wants to help...",
        "remember... you matter...", "breathe... it's listening...",
        "it remembers your kindness..."
    );

    private static final List<String> COUNTDOWN = List.of(
        "ten...", "nine...", "eight...", "seven...", "six...",
        "five...", "four...", "three...", "two...", "one..."
    );

    public static void sendWhisper(ServerPlayer player, String text) {
        if (player.connection == null) return;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSystemChatPacket(
            Component.literal(text).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), false));
        playWhisperSfx(player);
    }

    private static void playWhisperSfx(ServerPlayer player) {
        if (player.connection == null) return;
        double ox = RNG.nextGaussian() * 2;
        double oy = RNG.nextGaussian();
        double oz = RNG.nextGaussian() * 2;
        float pitch = 0.8f + RNG.nextFloat() * 0.4f;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            net.minecraft.core.Holder.direct(ModSounds.WHISPER_SOUND.get()), SoundSource.MASTER,
            player.getX() + ox, player.getY() + oy + 1, player.getZ() + oz,
            0.4f, pitch, 0));
    }

    public static void sendActionBar(ServerPlayer player, String text) {
        if (player.connection == null) return;
        player.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.RED), true);
    }

    public static void sendPositionedSound(ServerPlayer player, net.minecraft.sounds.SoundEvent sound, double x, double y, double z, float vol, float pitch) {
        if (player.connection == null) return;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            net.minecraft.core.Holder.direct(sound), SoundSource.MASTER, x, y, z, vol, pitch, 0));
    }

    public static String randomFragment(int rep) {
        if (rep <= 299 && RNG.nextFloat() < 0.6f) {
            return LOW_REP_FRAGMENTS.get(RNG.nextInt(LOW_REP_FRAGMENTS.size()));
        }
        if (rep >= 2000 && RNG.nextFloat() < 0.6f) {
            return HIGH_REP_FRAGMENTS.get(RNG.nextInt(HIGH_REP_FRAGMENTS.size()));
        }
        return FRAGMENTS.get(RNG.nextInt(FRAGMENTS.size()));
    }

    public static String usernameWhisper(String username) {
        return RNG.nextBoolean() ? "..." + username + "..." : "..." + new StringBuilder(username).reverse() + "...";
    }

    public static String countdownNumber() {
        return COUNTDOWN.get(RNG.nextInt(COUNTDOWN.size()));
    }

    public static String locationWhisper(double x, double y, double z) {
        return String.format("you are at %.0f %.0f %.0f... we know...", x, y, z);
    }

    public static String chatEcho(String chatMessage) {
        if (chatMessage.length() > 30) chatMessage = chatMessage.substring(0, 30);
        return "\"" + chatMessage + "\"... you said that... why?";
    }

    public static void whisperSequence(ServerPlayer player, int rep, int count, int interval) {
        player.server.tell(new net.minecraft.server.TickTask(
            player.server.getTickCount() + interval,
            () -> {
                if (player.connection == null || !player.connection.isAcceptingMessages()) return;
                for (int i = 0; i < count; i++) {
                    int delay = interval * (i + 1);
                    player.server.tell(new net.minecraft.server.TickTask(
                        player.server.getTickCount() + delay + i * 20,
                        () -> {
                            if (player.connection == null || !player.connection.isAcceptingMessages()) return;
                            String w;
                            float roll = RNG.nextFloat();
                            if (roll < 0.3f) w = randomFragment(rep);
                            else if (roll < 0.5f) w = usernameWhisper(player.getName().getString());
                            else if (roll < 0.65f) w = locationWhisper(player.getX(), player.getY(), player.getZ());
                            else if (roll < 0.8f) w = countdownNumber();
                            else w = randomFragment(rep);
                            sendWhisper(player, w);
                        }
                    ));
                }
            }
        ));
    }
}
