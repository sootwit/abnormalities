package com.abnormalities.horror;

import com.abnormalities.ReputationManager;
import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.registry.ModEntities;
import com.abnormalities.entity.NurEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

public class V1s1tManager {
    private static final Map<UUID, V1s1tData> DATA = new HashMap<>();
    private static final Random RNG = new Random();
    private static final List<Block> FLOWERS = List.of(
            Blocks.POPPY, Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY,
            Blocks.LILY_OF_THE_VALLEY, Blocks.CORNFLOWER, Blocks.DANDELION,
            Blocks.ALLIUM, Blocks.BLUE_ORCHID
    );
    private static final List<ItemStack> STAGE1_GIFTS = List.of(
            new ItemStack(Items.BREAD, 1), new ItemStack(Items.COOKIE, 3),
            new ItemStack(Items.SWEET_BERRIES, 2), new ItemStack(Items.GLOW_BERRIES, 1)
    );
    private static final List<ItemStack> STAGE2_GIFTS = List.of(
            new ItemStack(Items.APPLE, 1), new ItemStack(Items.GOLDEN_CARROT, 1),
            new ItemStack(Items.CAKE, 1), new ItemStack(Items.PUMPKIN_PIE, 1)
    );

    private static class V1s1tData {
        int stage = 0;
        int totalVisits = 0;
        long lastVisitDay = -1;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var srv = ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        var overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        if (!AbnormalitiesConfig.V1S1T_ENABLED.get()) return;
        long currentDay = overworld.getDayTime() / 24000L;
        if (currentDay < AbnormalitiesConfig.GRACE_PERIOD_DAYS.get()) return;
        if (overworld.getGameTime() % AbnormalitiesConfig.V1S1T_INTERVAL.get() != 0) return;

        for (ServerPlayer sp : new java.util.ArrayList<>(overworld.getServer().getPlayerList().getPlayers())) {
            UUID uuid = sp.getUUID();
            var d = DATA.computeIfAbsent(uuid, k -> new V1s1tData());
            if (d.lastVisitDay < 0) {
                d.lastVisitDay = currentDay;
                continue;
            }
            int interval = Math.max(1, AbnormalitiesConfig.V1S1T_VISIT_DAYS.get());
            if (currentDay - d.lastVisitDay < interval) continue;
            performVisit(sp, overworld, d);
            d.totalVisits++;
            d.lastVisitDay = currentDay;
            int newStage = Math.min(3, d.totalVisits / AbnormalitiesConfig.V1S1T_STAGE_UP.get());
            d.stage = newStage;
        }
    }

    private static void performVisit(ServerPlayer player, ServerLevel level, V1s1tData d) {
        SisterController.onVisitWarning(player);
        BlockPos anchor = player.getSleepingPos().orElse(player.blockPosition());
        if (!player.level().dimension().equals(Level.OVERWORLD)) return;

        int rep = ReputationManager.getRep(player);
        boolean hostile = rep < 900;

        switch (d.stage) {
            case 0 -> doStage0(player, level, anchor, hostile);
            case 1 -> doStage1(player, level, anchor, hostile);
            case 2 -> doStage2(player, level, anchor, hostile);
            case 3 -> doStage3(player, level, anchor, hostile);
        }
    }

    private static void doStage0(ServerPlayer player, ServerLevel level, BlockPos anchor, boolean hostile) {
        BlockPos chest = findChest(level, anchor, 32);
        if (chest != null) {
            int idx = RNG.nextInt(FLOWERS.size());
            Block flower = FLOWERS.get(idx);
            injectIntoChest(level, chest, new ItemStack(flower, 1));
        } else {
            BlockPos ground = findGround(level, anchor, 16);
            if (ground != null) {
                int idx = RNG.nextInt(FLOWERS.size());
                Block flower = FLOWERS.get(idx);
                BlockState below = level.getBlockState(ground.below());
                if (below.canOcclude() && level.getBlockState(ground).canBeReplaced()) {
                    level.setBlock(ground, flower.defaultBlockState(), 3);
                }
            }
        }
    }

    private static void doStage1(ServerPlayer player, ServerLevel level, BlockPos anchor, boolean hostile) {
        BlockPos chest = findChest(level, anchor, 48);
        if (chest != null) {
            ItemStack gift = STAGE1_GIFTS.get(RNG.nextInt(STAGE1_GIFTS.size()));
            injectIntoChest(level, chest, gift.copy());
        }
        if (RNG.nextInt(3) == 0) {
            BlockPos c2 = findChest(level, anchor, 48);
            if (c2 != null && !c2.equals(chest)) {
                int idx = RNG.nextInt(FLOWERS.size());
                injectIntoChest(level, c2, new ItemStack(FLOWERS.get(idx), 1));
            }
        }
    }

    private static void doStage2(ServerPlayer player, ServerLevel level, BlockPos anchor, boolean hostile) {
        BlockPos chest = findChest(level, anchor, 48);
        if (chest != null) {
            ItemStack gift = STAGE2_GIFTS.get(RNG.nextInt(STAGE2_GIFTS.size()));
            injectIntoChest(level, chest, gift.copy());
        }
        BlockPos stair = findStair(level, anchor, 32);
        if (stair != null && RNG.nextBoolean()) {
            BlockState st = level.getBlockState(stair);
            if (st.getBlock() instanceof StairBlock) {
                Direction facing = st.getValue(StairBlock.FACING);
                Direction newFacing = Direction.from2DDataValue((facing.get2DDataValue() + 1) % 4);
                level.setBlock(stair, st.setValue(StairBlock.FACING, newFacing), 3);
            }
        }
        if (RNG.nextInt(3) == 0) {
            BlockPos torch = findTorch(level, anchor, 24);
            if (torch != null) {
                Direction dir = Direction.from2DDataValue(RNG.nextInt(4));
                BlockPos newPos = torch.relative(dir);
                if (level.getBlockState(newPos).canBeReplaced()) {
                    BlockState ts = level.getBlockState(torch);
                    level.setBlock(newPos, ts, 3);
                    level.setBlock(torch, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private static void doStage3(ServerPlayer player, ServerLevel level, BlockPos anchor, boolean hostile) {
        BlockPos chest = findChest(level, anchor, 48);
        if (chest != null && RNG.nextBoolean()) {
            ItemStack gift = STAGE2_GIFTS.get(RNG.nextInt(STAGE2_GIFTS.size()));
            injectIntoChest(level, chest, gift.copy());
        }
        if (RNG.nextInt(4) == 0) {
            BlockPos torch = findTorch(level, anchor, 32);
            if (torch != null) {
                level.setBlock(torch, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        if (hostile && RNG.nextInt(3) == 0) {
            double angle = RNG.nextDouble() * Math.PI * 2;
            double dist = 8.0 + RNG.nextDouble() * 10.0;
            double sx = anchor.getX() + Math.cos(angle) * dist;
            double sz = anchor.getZ() + Math.sin(angle) * dist;
            int sy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) sx, (int) sz);
            NurEntity nur = ModEntities.NUR.get().create(level);
            if (nur != null) {
                nur.moveTo(sx + 0.5, sy + 1, sz + 0.5, 0, 0);
                var states = NurEntity.State.values();
                NurEntity.State picked;
                do { picked = states[level.random.nextInt(states.length)]; } while (picked == NurEntity.State.CHASING);
                nur.currentState = picked;
                level.addFreshEntity(nur);
            }
        }
    }

    private static BlockPos findChest(ServerLevel level, BlockPos anchor, int radius) {
        List<BlockPos> chests = new ArrayList<>();
        BlockPos.betweenClosedStream(anchor.offset(-radius, -radius, -radius), anchor.offset(radius, radius, radius))
                .forEach(pos -> {
                    if (level.getBlockEntity(pos) instanceof ChestBlockEntity) chests.add(pos.immutable());
                });
        if (chests.isEmpty()) return null;
        return chests.get(RNG.nextInt(chests.size()));
    }

    private static BlockPos findGround(ServerLevel level, BlockPos anchor, int radius) {
        List<BlockPos> spots = new ArrayList<>();
        BlockPos.betweenClosedStream(anchor.offset(-radius, 0, -radius), anchor.offset(radius, 0, radius))
                .forEach(pos -> {
                    int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
                    BlockPos g = new BlockPos(pos.getX(), top, pos.getZ());
                    if (g.getY() >= level.getMinBuildHeight() + 1 && g.getY() <= level.getMaxBuildHeight()) {
                        BlockState below = level.getBlockState(g.below());
                        if (below.canOcclude() && level.getBlockState(g).canBeReplaced()) {
                            spots.add(g.immutable());
                        }
                    }
                });
        if (spots.isEmpty()) return null;
        return spots.get(RNG.nextInt(spots.size()));
    }

    private static BlockPos findStair(ServerLevel level, BlockPos anchor, int radius) {
        List<BlockPos> stairs = new ArrayList<>();
        BlockPos.betweenClosedStream(anchor.offset(-radius, -radius, -radius), anchor.offset(radius, radius, radius))
                .forEach(pos -> {
                    if (level.getBlockState(pos).getBlock() instanceof StairBlock) stairs.add(pos.immutable());
                });
        if (stairs.isEmpty()) return null;
        return stairs.get(RNG.nextInt(stairs.size()));
    }

    private static BlockPos findTorch(ServerLevel level, BlockPos anchor, int radius) {
        List<BlockPos> torches = new ArrayList<>();
        BlockPos.betweenClosedStream(anchor.offset(-radius, -radius, -radius), anchor.offset(radius, radius, radius))
                .forEach(pos -> {
                    BlockState st = level.getBlockState(pos);
                    if (st.is(Blocks.TORCH) || st.is(Blocks.WALL_TORCH) || st.is(Blocks.REDSTONE_TORCH) || st.is(Blocks.REDSTONE_WALL_TORCH))
                        torches.add(pos.immutable());
                });
        if (torches.isEmpty()) return null;
        return torches.get(RNG.nextInt(torches.size()));
    }

    private static void injectIntoChest(ServerLevel level, BlockPos pos, ItemStack stack) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ChestBlockEntity chest)) return;
        for (int i = 0; i < chest.getContainerSize(); i++) {
            ItemStack existing = chest.getItem(i);
            if (existing.isEmpty()) {
                chest.setItem(i, stack);
                return;
            }
            if (ItemStack.isSameItemSameTags(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
                int space = existing.getMaxStackSize() - existing.getCount();
                int add = Math.min(space, stack.getCount());
                existing.grow(add);
                stack.shrink(add);
                if (stack.isEmpty()) return;
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        DATA.remove(event.getEntity().getUUID());
    }

    public static void cleanup(UUID uuid) {
        DATA.remove(uuid);
    }

    public static void forceVisit(ServerPlayer player) {
        var d = DATA.computeIfAbsent(player.getUUID(), k -> new V1s1tData());
        var level = (ServerLevel) player.level();
        d.lastVisitDay = level.getDayTime() / 24000L;
        performVisit(player, level, d);
        d.totalVisits++;
        int newStage = Math.min(3, d.totalVisits / AbnormalitiesConfig.V1S1T_STAGE_UP.get());
        d.stage = newStage;
    }
}
