package com.abnormalities.horror;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VoidEvent extends AbstractHorrorEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|Void");
    public VoidEvent() {
        super("void", 80, 1.0, 0, 2500, 36000, false);
    }

    @Override
    public void execute(ServerPlayer player) {
        if (player.level().isClientSide) return;
        ServerLevel level = (ServerLevel) player.level();
        BlockPos pos = player.blockPosition();
        LOGGER.info("[Void] {} void event at ({}, {}, {})", player.getName().getString(), pos.getX(), pos.getY(), pos.getZ());
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = -1; y >= -3; y--) {
                    BlockPos below = pos.offset(x, y, z);
                    if (!level.getBlockState(below).isAir() && !level.getBlockState(below).is(Blocks.BEDROCK)) {
                        level.destroyBlock(below, false);
                    }
                }
            }
        }
        level.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                net.minecraft.sounds.SoundEvents.AMBIENT_CAVE.get(), net.minecraft.sounds.SoundSource.AMBIENT, 2.0f, 0.3f);
    }
}
