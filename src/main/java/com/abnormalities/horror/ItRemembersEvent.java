package com.abnormalities.horror;

import com.abnormalities.ActionLogger;
import com.abnormalities.ActionLogger.ActionEntry;
import com.abnormalities.WhisperManager;
import com.abnormalities.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.*;

public class ItRemembersEvent extends AbstractHorrorEvent {
    public ItRemembersEvent() {
        super("it_remembers", 60, 1.1);
    }

    @Override
    public boolean canTrigger(ServerPlayer player, long currentTick) {
        return ActionLogger.getSince(player, 20).size() >= 3;
    }

    @Override
    public void execute(ServerPlayer player) {
        ActionEntry old = ActionLogger.getOldestAction(player, 20);
        if (old == null) {
            WhisperManager.sendWhisper(player, "it remembers... something...");
            return;
        }

        WhisperManager.sendWhisper(player, "it remembers...");
        scheduleReference(player, old, 0);
    }

    private void scheduleReference(ServerPlayer player, ActionEntry entry, int index) {
        if (index >= 3) {
            WhisperManager.sendWhisper(player, "...it never forgets.");
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, false, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1, false, false, false));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.HEARTBEAT_SOUND.get(), SoundSource.MASTER, 0.8f, 0.6f);
            return;
        }
        player.server.tell(new net.minecraft.server.TickTask(
            player.server.getTickCount() + 30 * (index + 1),
            () -> {
                if (player.connection == null || !player.connection.isAcceptingMessages()) return;
                switch (entry.type) {
                    case "break" -> WhisperManager.sendWhisper(player, "you broke something. remember?");
                    case "place" -> WhisperManager.sendWhisper(player, "you placed something there. why?");
                    case "kill" -> WhisperManager.sendWhisper(player, "you took something's life. it remembers.");
                    case "chat" -> {
                        if (entry.detail != null && entry.detail.length() > 3)
                            WhisperManager.sendWhisper(player, "you said \"" + entry.detail + "\". " + (index == 1 ? "why did you say that?" : "it remembers everything."));
                        else
                            WhisperManager.sendWhisper(player, "you spoke. it heard.");
                    }
                    case "move" -> WhisperManager.sendWhisper(player, "you were somewhere else. it followed.");
                    default -> WhisperManager.sendWhisper(player, "you did something. it remembers.");
                }
                scheduleReference(player, entry, index + 1);
            }
        ));
    }
}
