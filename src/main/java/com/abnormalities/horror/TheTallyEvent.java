package com.abnormalities.horror;

import com.abnormalities.WhisperManager;
import com.abnormalities.registry.ModEvents;
import com.abnormalities.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

public class TheTallyEvent extends AbstractHorrorEvent {
    private static final Map<UUID, Integer> TALLY = new HashMap<>();
    private static final Map<UUID, String> TARGET_ACTION = new HashMap<>();
    private static final Map<UUID, Double> LAST_Y = new HashMap<>();
    private static final List<String> ACTIONS = List.of("jump", "move", "mine");

    public TheTallyEvent() {
        super("the_tally", 100, 0.8);
    }

    @Override
    public boolean allowsOngoing() { return true; }

    @Override
    public void execute(ServerPlayer player) {
        String action = ACTIONS.get(player.getRandom().nextInt(ACTIONS.size()));
        TARGET_ACTION.put(player.getUUID(), action);
        TALLY.put(player.getUUID(), 0);
        LAST_Y.put(player.getUUID(), player.getY());
        WhisperManager.sendWhisper(player, "...");
    }

    @Override
    public void onPlayerTick(ServerPlayer player) {
        if (!TALLY.containsKey(player.getUUID())) return;
        String action = TARGET_ACTION.get(player.getUUID());
        int count = TALLY.get(player.getUUID());
        boolean triggered = false;

        if ("jump".equals(action)) {
            double ly = LAST_Y.getOrDefault(player.getUUID(), player.getY());
            if (player.getY() > ly + 0.25 && player.getDeltaMovement().y > 0) {
                count++;
                triggered = true;
            }
            LAST_Y.put(player.getUUID(), player.getY());
        } else if ("move".equals(action)) {
            if (player.xxa != 0 || player.zza != 0) {
                count++;
                triggered = true;
            }
        }

        if (triggered) {
            TALLY.put(player.getUUID(), count);
            WhisperManager.sendActionBar(player, count + "");
        }

        if (count >= 50) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, false, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3, false, false, false));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.HEARTBEAT_SOUND.get(), SoundSource.MASTER, 1.5f, 0.5f);
            WhisperManager.sendWhisper(player, "...enough.");
            ModEvents.forceNurSpawn(player);
            tallyDone(player);
        }
    }

    private static void tallyDone(ServerPlayer player) {
        TALLY.remove(player.getUUID());
        TARGET_ACTION.remove(player.getUUID());
        LAST_Y.remove(player.getUUID());
        HorrorEventPool.clearOngoing(player);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer sp)) return;
        if (!TALLY.containsKey(sp.getUUID())) return;
        if (!"mine".equals(TARGET_ACTION.get(sp.getUUID()))) return;
        int count = TALLY.getOrDefault(sp.getUUID(), 0) + 1;
        TALLY.put(sp.getUUID(), count);
        WhisperManager.sendActionBar(sp, count + "");
        if (count >= 50) {
            sp.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, false, false, false));
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3, false, false, false));
            sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                ModSounds.HEARTBEAT_SOUND.get(), SoundSource.MASTER, 1.5f, 0.5f);
            WhisperManager.sendWhisper(sp, "...enough.");
            ModEvents.forceNurSpawn(sp);
            tallyDone(sp);
        }
    }

    @Override
    public void onCleanup(ServerPlayer player) {
        TALLY.remove(player.getUUID());
        TARGET_ACTION.remove(player.getUUID());
        LAST_Y.remove(player.getUUID());
    }
}
