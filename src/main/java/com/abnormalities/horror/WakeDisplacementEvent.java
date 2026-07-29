package com.abnormalities.horror;

import com.abnormalities.config.AbnormalitiesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.TickTask;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Random;

import java.util.Random;

public class WakeDisplacementEvent {
    private static final Random RNG = new Random();

    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (!(player instanceof ServerPlayer sp)) return;
        if (!AbnormalitiesConfig.W4K3_ENABLED.get()) return;
        if (event.wakeImmediately()) return;

        long currentDay = sp.level().getDayTime() / 24000L;
        if (currentDay < AbnormalitiesConfig.GRACE_PERIOD_DAYS.get()) return;

        int chance = AbnormalitiesConfig.W4K3_CHANCE.get();
        if (chance > 1 && RNG.nextInt(chance) != 0) return;

        BlockPos bedPos = sp.getSleepingPos().orElse(null);
        if (bedPos == null) return;

        var srv = sp.getServer();
        if (srv == null) return;

        srv.tell(new TickTask(srv.getTickCount() + 1, () -> {
            doDisplace(sp, bedPos);
        }));
    }

    public static void forceDisplace(ServerPlayer player) {
        if (player.level().isClientSide) return;
        BlockPos bedPos = player.getSleepingPos().orElse(player.blockPosition());
        doDisplace(player, bedPos);
    }

    private static void doDisplace(ServerPlayer sp, BlockPos bedPos) {
        if (!sp.isAlive() || sp.connection == null) return;
        int dist = AbnormalitiesConfig.W4K3_DISTANCE.get();
        double angle = RNG.nextDouble() * Math.PI * 2;
        double dx = Math.cos(angle) * (dist + RNG.nextDouble() * 10.0);
        double dz = Math.sin(angle) * (dist + RNG.nextDouble() * 10.0);
        int targetX = bedPos.getX() + (int) Math.round(dx);
        int targetZ = bedPos.getZ() + (int) Math.round(dz);
        ServerLevel level = (ServerLevel) sp.level();
        int targetY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetX, targetZ);
        if (targetY < level.getMinBuildHeight() + 1) return;
        BlockPos tp = new BlockPos(targetX, targetY + 1, targetZ);
        sp.teleportTo(level, tp.getX() + 0.5, tp.getY(), tp.getZ() + 0.5, sp.getYRot(), sp.getXRot());
    }
}
