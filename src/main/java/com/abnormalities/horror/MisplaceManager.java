package com.abnormalities.horror;

import com.abnormalities.config.AbnormalitiesConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Random;

public class MisplaceManager {
    private static final Random RNG = new Random();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        var overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        if (!AbnormalitiesConfig.M1SL4Y_ENABLED.get()) return;
        long currentDay = overworld.getDayTime() / 24000L;
        if (currentDay < AbnormalitiesConfig.GRACE_PERIOD_DAYS.get()) return;
        if (overworld.getGameTime() % 200 != 0) return;

        for (var sp : overworld.getServer().getPlayerList().getPlayers()) {
            if (overworld.random.nextInt(AbnormalitiesConfig.M1SL4Y_WEIGHT.get()) != 0) continue;
            if (Vr9pController.isActive(sp.getUUID())) continue;
            if (sp.isSleeping()) continue;
            misplace(sp);
        }
    }

    private static void misplace(ServerPlayer player) {
        Inventory inv = player.getInventory();
        if (RNG.nextBoolean()) {
            int a = 1 + RNG.nextInt(35);
            int b = 1 + RNG.nextInt(35);
            while (b == a) b = 1 + RNG.nextInt(35);
            ItemStack sa = inv.getItem(a);
            ItemStack sb = inv.getItem(b);
            inv.setItem(a, sb);
            inv.setItem(b, sa);
        } else {
            int slot = 1 + RNG.nextInt(35);
            ItemStack stack = inv.getItem(slot);
            if (!stack.isEmpty() && stack.getCount() > 1) {
                stack.shrink(1 + RNG.nextInt(Math.min(2, stack.getCount() - 1)));
                inv.setItem(slot, stack);
            }
        }
    }

    public static void forceMisplace(ServerPlayer player) {
        misplace(player);
    }
}
