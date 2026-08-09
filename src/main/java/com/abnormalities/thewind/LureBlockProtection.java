package com.abnormalities.thewind;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class LureBlockProtection {
    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (event.getPlayer().level().isClientSide) return;
        Player player = event.getPlayer();
        if (player instanceof ServerPlayer sp && TheWindLureManager.isInLure(sp)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!TheWindLureManager.isInLure(sp)) return;
        int min = com.abnormalities.config.AbnormalitiesConfig.TW_PILLARS_STEAL_MIN.get();
        int max = com.abnormalities.config.AbnormalitiesConfig.TW_PILLARS_STEAL_MAX.get();
        int count = min + sp.level().random.nextInt(Math.max(1, max - min + 1));
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            if (!sp.getInventory().getItem(i).isEmpty()) candidates.add(i);
        }
        Collections.shuffle(candidates, new Random());
        int stolen = 0;
        for (int slot : candidates) {
            if (stolen >= count) break;
            sp.getInventory().setItem(slot, ItemStack.EMPTY);
            stolen++;
        }
    }
}
