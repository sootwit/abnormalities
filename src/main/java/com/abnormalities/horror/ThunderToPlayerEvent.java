package com.abnormalities.horror;

import com.abnormalities.config.AbnormalitiesConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThunderToPlayerEvent extends AbstractHorrorEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|ThunderToPlayer");

    public ThunderToPlayerEvent() {
        super("thunder_to_player", 40, 0.9);
    }

    @Override
    public boolean canTrigger(ServerPlayer player, long currentTick) {
        return !player.level().isClientSide && AbnormalitiesConfig.THUNDER_TO_PLAYER_ENABLED.get();
    }

    @Override
    public void execute(ServerPlayer player) {
        strike(player);
    }

    public static void forceThunder(ServerPlayer player) {
        strike(player);
    }

    private static void strike(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
        bolt.moveTo(player.getX(), player.getY(), player.getZ());
        bolt.setCause(player);
        level.addFreshEntity(bolt);
        LOGGER.info("[ThunderToPlayer] {} struck by lightning", player.getName().getString());
    }
}
