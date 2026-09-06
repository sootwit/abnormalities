package com.abnormalities.horror;

import com.abnormalities.entity.NurEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.BedBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NurSleepBlocker {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|SleepBlocker");

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide) return;
        if (!(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof BedBlock)) return;
        Player player = event.getEntity();
        if (hasNurs(player)) {
            event.setCanceled(true);
            sendErrNur(player);
            LOGGER.info("[SleepBlocker] blocked bed click for {}, nurs found", player.getName().getString());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) return;
        if (event.player.tickCount % 10 != 0) return;
        Player player = event.player;
        if (!player.isSleeping()) return;
        if (hasNurs(player)) {
            player.stopSleepInBed(true, true);
            sendErrNur(player);
            LOGGER.info("[SleepBlocker] kicked {} out of bed, nur found", player.getName().getString());
        }
    }

    private static boolean hasNurs(Player player) {
        var level = player.level();
        var aabb = new net.minecraft.world.phys.AABB(
                player.getX() - 128, level.getMinBuildHeight(), player.getZ() - 128,
                player.getX() + 128, level.getMaxBuildHeight(), player.getZ() + 128);
        return !level.getEntitiesOfClass(NurEntity.class, aabb).isEmpty();
    }

    private static void sendErrNur(Player player) {
        if (player instanceof ServerPlayer sp) {
            String msg = player.getName().getString().equals("Auromancy") ? "0x0000.voidspore" : "0x0000.nur";
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSystemChatPacket(
                    Component.literal(msg).withStyle(ChatFormatting.DARK_RED), true));
        }
    }
}
