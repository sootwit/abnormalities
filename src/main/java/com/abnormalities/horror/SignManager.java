package com.abnormalities.horror;

import com.abnormalities.config.AbnormalitiesConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

public class SignManager {
    private static final Map<UUID, Long> LAST_SIGN = new HashMap<>();
    private static final List<String> MESSAGES = List.of(
            "i saw you here.",
            "you were {deaths} deaths ago.",
            "you stood right there.",
            "{days} days. and you still dont see me.",
            "i counted your steps. {steps}.",
            "this is where you gave up.",
            "you sleep above me.",
            "your name was almost here.",
            "the {block} you mined? i kept it.",
            "you are closer than you think.",
            "dont come back tomorrow.",
            "you left something behind.",
            "i was here first.",
            "you dug too deep.",
            "the dark remembers."
    );

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        var overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        if (!AbnormalitiesConfig.S1GN_ENABLED.get()) return;
        long currentDay = overworld.getDayTime() / 24000L;
        if (currentDay < AbnormalitiesConfig.GRACE_PERIOD_DAYS.get()) return;
        if (overworld.getGameTime() % 200 != 0) return;

        for (var sp : new java.util.ArrayList<>(overworld.getServer().getPlayerList().getPlayers())) {
            if (sp.isSleeping()) continue;
            UUID uuid = sp.getUUID();
            long last = LAST_SIGN.getOrDefault(uuid, 0L);
            if (overworld.getGameTime() - last < AbnormalitiesConfig.S1GN_INTERVAL.get()) continue;
            if (overworld.random.nextInt(AbnormalitiesConfig.S1GN_CHANCE.get()) != 0) continue;
            if (placeSign(sp, overworld)) {
                LAST_SIGN.put(uuid, overworld.getGameTime());
            }
        }
    }

    private static boolean placeSign(ServerPlayer player, ServerLevel level) {
        SisterController.onSignWarning(player);
        BlockPos pos = findWallSpot(level, player.blockPosition(), AbnormalitiesConfig.S1GN_SEARCH_RADIUS.get());
        if (pos == null) return false;
        level.setBlock(pos, Blocks.OAK_WALL_SIGN.defaultBlockState(), 3);
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SignBlockEntity sign)) return false;
        String msg = MESSAGES.get(level.random.nextInt(MESSAGES.size()));
        int deaths = player.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.DEATHS));
        int days = (int) (level.getDayTime() / 24000L);
        int steps = player.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.WALK_ONE_CM)) / 100;
        msg = msg.replace("{deaths}", String.valueOf(deaths))
                 .replace("{days}", String.valueOf(days))
                 .replace("{steps}", String.valueOf(steps))
                 .replace("{block}", "stone");
        SignText text = new SignText()
                .setMessage(0, Component.literal(msg).withStyle(ChatFormatting.DARK_RED))
                .setColor(net.minecraft.world.item.DyeColor.BLACK);
        sign.setText(text, true);
        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
        return true;
    }

    private static BlockPos findWallSpot(ServerLevel level, BlockPos center, int radius) {
        List<BlockPos> spots = new ArrayList<>();
        BlockPos.betweenClosedStream(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))
                .forEach(p -> {
                    if (!level.getBlockState(p).isAir()) return;
                    BlockState below = level.getBlockState(p.below());
                    if (!below.isAir() && below.canOcclude()) {
                        for (Direction dir : Direction.Plane.HORIZONTAL) {
                            BlockState neighbor = level.getBlockState(p.relative(dir));
                            if (neighbor.canOcclude()) {
                                spots.add(p.immutable());
                                return;
                            }
                        }
                    }
                });
        if (spots.isEmpty()) return null;
        return spots.get(level.random.nextInt(spots.size()));
    }

    public static void recordMinedBlock(ServerPlayer player, String blockName) {
        player.getPersistentData().putString("abnormalities:last_mined", blockName);
    }

    public static void forceSign(ServerPlayer player) {
        placeSign(player, (ServerLevel) player.level());
    }
}
