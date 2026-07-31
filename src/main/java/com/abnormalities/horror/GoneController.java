package com.abnormalities.horror;

import com.abnormalities.ReputationManager;
import com.abnormalities.config.AbnormalitiesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

public class GoneController {
    private static final Map<UUID, Long> LAST_EXTINGUISH = new HashMap<>();
    private static final Map<UUID, Long> LAST_ITEM_STEAL = new HashMap<>();

    private static boolean isLight(BlockState state) {
        return state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)
                || state.is(Blocks.LANTERN) || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH)
                || state.is(Blocks.SOUL_LANTERN) || state.is(Blocks.SOUL_CAMPFIRE);
    }

    private static boolean isLightItem(ItemStack stack) {
        return stack.is(Items.TORCH) || stack.is(Items.LANTERN) || stack.is(Items.CAMPFIRE)
                || stack.is(Items.SOUL_TORCH) || stack.is(Items.SOUL_LANTERN) || stack.is(Items.SOUL_CAMPFIRE);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        var overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        if (!AbnormalitiesConfig.GONE_ENABLED.get()) return;
        long currentDay = overworld.getDayTime() / 24000L;
        if (currentDay < AbnormalitiesConfig.GRACE_PERIOD_DAYS.get()) return;
        if (overworld.getGameTime() % 40 != 0) return;

        for (var sp : new java.util.ArrayList<>(overworld.getServer().getPlayerList().getPlayers())) {
            if (sp.isSleeping()) continue;
            int rep = ReputationManager.getRep(sp);
            long now = overworld.getGameTime();

            int interval = AbnormalitiesConfig.GONE_INTERVAL.get();
            if (rep < 900) interval = (int) (interval * 0.5);
            else if (rep > 1900) interval = (int) (interval * 2.0);

            long last = LAST_EXTINGUISH.getOrDefault(sp.getUUID(), 0L);
            if (now - last >= interval) {
                if (extinguishOne(sp, overworld)) {
                    LAST_EXTINGUISH.put(sp.getUUID(), now);
                    if (overworld.random.nextInt(5) == 0) SisterController.onGoneWarning(sp);
                }
            }

            long lastItem = LAST_ITEM_STEAL.getOrDefault(sp.getUUID(), 0L);
            if (now - lastItem >= interval * 4L) {
                if (stealLightItem(sp)) {
                    LAST_ITEM_STEAL.put(sp.getUUID(), now);
                }
            }
        }
    }

    private static boolean extinguishOne(ServerPlayer player, ServerLevel level) {
        List<BlockPos> candidates = new ArrayList<>();
        int radius = AbnormalitiesConfig.GONE_RADIUS.get();
        Vec3 look = player.getLookAngle();
        BlockPos.betweenClosedStream(player.blockPosition().offset(-radius, -radius, -radius),
                player.blockPosition().offset(radius, radius, radius)).forEach(p -> {
            BlockState state = level.getBlockState(p);
            if (!isLight(state)) return;
            Vec3 toBlock = new Vec3(p.getX() + 0.5 - player.getX(), p.getY() + 0.5 - player.getEyeY(), p.getZ() + 0.5 - player.getZ());
            double dot = toBlock.normalize().dot(look);
            if (dot < 0.1) candidates.add(p.immutable());
        });
        if (candidates.isEmpty()) return false;
        BlockPos pick = candidates.get(level.random.nextInt(candidates.size()));
        BlockState state = level.getBlockState(pick);
        if (state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)) {
            level.setBlock(pick, state.setValue(net.minecraft.world.level.block.CampfireBlock.LIT, false), 3);
        } else {
            level.setBlock(pick, Blocks.AIR.defaultBlockState(), 3);
        }
        level.playSound(null, pick.getX(), pick.getY(), pick.getZ(),
                SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 0.6f, 1.0f);
        return true;
    }

    private static boolean stealLightItem(ServerPlayer player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || !isLightItem(stack)) continue;
            stack.shrink(1);
            inv.setItem(i, stack);
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                net.minecraft.core.Holder.direct(com.abnormalities.registry.ModSounds.WHISPER_SOUND.get()),
                SoundSource.MASTER, player.getX(), player.getY(), player.getZ(), 1.5f, 0.6f, 0));
            return true;
        }
        return false;
    }

    public static void forceSteal(ServerPlayer player) {
        extinguishOne(player, (ServerLevel) player.level());
        stealLightItem(player);
    }
}
