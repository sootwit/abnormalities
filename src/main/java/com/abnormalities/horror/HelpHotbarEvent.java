package com.abnormalities.horror;

import com.abnormalities.WhisperManager;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class HelpHotbarEvent extends AbstractHorrorEvent {
    private static final Map<UUID, Integer> T = new HashMap<>();

    public HelpHotbarEvent() {
        super("hotbar_help", 60, 0.7);
    }

    @Override
    public boolean allowsOngoing() { return true; }

    @Override
    public void execute(ServerPlayer player) {
        T.put(player.getUUID(), 0);
        WhisperManager.sendActionBar(player, "HELP");
    }

    @Override
    public void onPlayerTick(ServerPlayer player) {
        int t = T.merge(player.getUUID(), 1, Integer::sum);
        if (t > 180) {
            T.remove(player.getUUID());
            HorrorEventPool.clearOngoing(player);
            return;
        }
        if (t % 30 == 0) {
            WhisperManager.sendActionBar(player, "HELP");
        }
    }

    @Override
    public void onCleanup(ServerPlayer player) {
        T.remove(player.getUUID());
    }
}
