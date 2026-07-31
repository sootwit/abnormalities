package com.abnormalities.horror;

import com.abnormalities.config.AbnormalitiesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Random;

public class CircleManager {
    private static final Random RNG = new Random();

    @SubscribeEvent
    public static void onWake(PlayerWakeUpEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!AbnormalitiesConfig.C1RCL_ENABLED.get()) return;
        if (event.wakeImmediately()) return;
        long currentDay = sp.level().getDayTime() / 24000L;
        if (currentDay < AbnormalitiesConfig.GRACE_PERIOD_DAYS.get()) return;
        if (RNG.nextInt(AbnormalitiesConfig.C1RCL_CHANCE.get()) != 0) return;
        BlockPos bed = sp.getSleepingPos().orElse(sp.blockPosition());
        spawnRing((ServerLevel) sp.level(), bed);
    }

    private static void spawnRing(ServerLevel level, BlockPos center) {
        int radius = 4 + RNG.nextInt(3);
        int count = radius * 6;
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2 * i) / count;
            int x = center.getX() + (int) Math.round(Math.cos(angle) * radius);
            int z = center.getZ() + (int) Math.round(Math.sin(angle) * radius);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            BlockState ground = level.getBlockState(pos.below());
            if (!ground.canOcclude()) continue;
            if (!level.getBlockState(pos).isAir()) continue;
            if (level.random.nextBoolean()) {
                level.setBlock(pos, Blocks.TORCH.defaultBlockState(), 3);
            } else {
                level.setBlock(pos, Blocks.CAMPFIRE.defaultBlockState(), 3);
            }
        }
    }

    public static void forceRing(ServerPlayer player) {
        spawnRing((ServerLevel) player.level(), player.blockPosition());
    }
}
