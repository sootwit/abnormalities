package com.abnormalities.horror;

import com.abnormalities.WhisperManager;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class HotbarNurEvent extends AbstractHorrorEvent {
    private static final Map<UUID, Integer> T = new HashMap<>();

    public HotbarNurEvent() {
        super("hotbar_nur", 70, 0.6);
    }

    @Override
    public boolean allowsOngoing() { return true; }

    @Override
    public void execute(ServerPlayer player) {
        T.put(player.getUUID(), 0);
        WhisperManager.sendActionBar(player, "nur");
    }

    @Override
    public void onPlayerTick(ServerPlayer player) {
        int t = T.merge(player.getUUID(), 1, Integer::sum);
        if (t > 140) {
            T.remove(player.getUUID());
            HorrorEventPool.clearOngoing(player);
            return;
        }
        if (t % 20 == 0) {
            WhisperManager.sendActionBar(player, "nur");
        }
    }

    @Override
    public void onCleanup(ServerPlayer player) {
        T.remove(player.getUUID());
    }
}
