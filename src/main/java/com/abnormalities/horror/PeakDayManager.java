package com.abnormalities.horror;

import com.abnormalities.config.AbnormalitiesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PeakDayManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|PeakDay");
    private enum DayType { NORMAL, BAD, GOOD }

    private static final long SAVE_THROTTLE_MS = 5000;
    private static final Random RNG = new Random();
    private static DayType currentType = DayType.NORMAL;
    private static long ticksRemaining = 0;
    private static long lastSaveTime = 0;
    private static File dataFile = null;
    private static boolean loaded = false;
    private static int giftsSpawned = 0;

    private static final int[] ORE_Y_MIN = { -59, 15, -48, -24, -1, -16, 0, 232 };
    private static final int[] ORE_Y_MAX = { -53, 10, -16, -48, 64, 112, 80, 256 };
    private static final int[] ORE_AMOUNT_MIN = { 5, 8, 4, 10, 6, 8, 10, 2 };
    private static final int[] ORE_AMOUNT_MAX = { 15, 16, 10, 20, 12, 16, 24, 6 };
    private static final ItemStack[] ORE_ITEMS = {
        new ItemStack(Items.DIAMOND),
        new ItemStack(Items.RAW_IRON),
        new ItemStack(Items.RAW_GOLD),
        new ItemStack(Items.REDSTONE),
        new ItemStack(Items.LAPIS_LAZULI),
        new ItemStack(Items.RAW_COPPER),
        new ItemStack(Items.COAL),
        new ItemStack(Items.EMERALD)
    };

    public static boolean isBadDay() {
        return currentType == DayType.BAD;
    }

    public static boolean isGoodDay() {
        return currentType == DayType.GOOD;
    }

    public static DayType getCurrentType() {
        return currentType;
    }

    public static double getHorrorMultiplier() {
        if (currentType == DayType.BAD) return AbnormalitiesConfig.PEAK_DAY_BAD_MULTIPLIER.get();
        if (currentType == DayType.GOOD) return AbnormalitiesConfig.PEAK_DAY_GOOD_MULTIPLIER.get();
        return 1.0;
    }

    public static double getSpawnMultiplier() {
        if (currentType == DayType.BAD) return AbnormalitiesConfig.PEAK_DAY_BAD_SPAWN_MULT.get();
        if (currentType == DayType.GOOD) return AbnormalitiesConfig.PEAK_DAY_GOOD_SPAWN_MULT.get();
        return 1.0;
    }

    public static boolean isWindDisabled() {
        return currentType == DayType.GOOD && AbnormalitiesConfig.PEAK_DAY_WIND_DISABLED.get();
    }

    public static int getXyzItemMultiplier() {
        if (currentType == DayType.BAD) return AbnormalitiesConfig.PEAK_DAY_BAD_XYZ_MULT.get();
        if (currentType == DayType.GOOD) return AbnormalitiesConfig.PEAK_DAY_GOOD_XYZ_MULT.get();
        return 1;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!AbnormalitiesConfig.PEAK_DAY_ENABLED.get()) return;
        if (!loaded || dataFile == null) return;
        var srv = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (srv == null) return;
        ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        if (overworld.getServer().getTickCount() % 20 != 0) return;

        ticksRemaining--;
        if (ticksRemaining <= 0) {
            advanceDay(overworld);
        }

        if (currentType == DayType.GOOD) {
            long now = overworld.getGameTime();
            for (ServerPlayer player : srv.getPlayerList().getPlayers()) {
                if (player.level().dimension() != Level.OVERWORLD) continue;
                if (giftsSpawned >= AbnormalitiesConfig.PEAK_DAY_MAX_GIFTS.get()) continue;
                if (player.tickCount % AbnormalitiesConfig.PEAK_DAY_GIFT_INTERVAL.get() != 0) continue;
                spawnGiftChest(overworld, player);
            }
        }
    }

    private static void advanceDay(ServerLevel level) {
        if (currentType == DayType.BAD || currentType == DayType.GOOD) {
            LOGGER.info("[PeakDay] {} day ended", currentType);
            currentType = DayType.NORMAL;
            ticksRemaining = 24000L;
            return;
        }

        int roll = RNG.nextInt(100);
        int badChance = AbnormalitiesConfig.PEAK_DAY_BAD_CHANCE.get();
        int goodChance = AbnormalitiesConfig.PEAK_DAY_GOOD_CHANCE.get();
        if (roll < badChance) {
            currentType = DayType.BAD;
            ticksRemaining = 24000L * AbnormalitiesConfig.PEAK_DAY_DURATION.get();
            LOGGER.info("[PeakDay] bad day started (roll={})", roll);
        } else if (roll < badChance + goodChance) {
            currentType = DayType.GOOD;
            ticksRemaining = 24000L * AbnormalitiesConfig.PEAK_DAY_DURATION.get();
            giftsSpawned = 0;
            LOGGER.info("[PeakDay] good day started (roll={})", roll);
        } else {
            currentType = DayType.NORMAL;
            ticksRemaining = 24000L;
        }
        save();
    }

    private static void spawnGiftChest(ServerLevel level, ServerPlayer player) {
        int playerY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) player.getX(), (int) player.getZ());
        double angle = RNG.nextDouble() * Math.PI * 2;
        double dist = 4.0 + RNG.nextDouble() * 4.0;
        int cx = (int) (player.getX() + Math.cos(angle) * dist);
        int cz = (int) (player.getZ() + Math.sin(angle) * dist);
        int cy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, cx, cz);
        BlockPos chestPos = new BlockPos(cx, cy, cz);
        if (!level.getBlockState(chestPos).canBeReplaced()) return;
        if (!level.getBlockState(chestPos.below()).canOcclude()) return;

        level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            ItemStack gift = rollGift(player);
            chest.setItem(0, gift);
            level.playSound(null, chestPos.getX(), chestPos.getY(), chestPos.getZ(),
                SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1.0f, 1.0f);
            giftsSpawned++;
        }
    }

    private static ItemStack rollGift(ServerPlayer player) {
        int hunger = player.getFoodData().getFoodLevel();
        boolean hasBrokenTool = false;
        ItemStack brokenTool = ItemStack.EMPTY;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isDamageableItem() && stack.getDamageValue() > stack.getMaxDamage() * 0.8) {
                hasBrokenTool = true;
                brokenTool = stack;
                break;
            }
        }

        if (hunger < 10) {
            int roll = RNG.nextInt(3);
            if (roll == 0) return new ItemStack(Items.BREAD, 8 + RNG.nextInt(8));
            if (roll == 1) return new ItemStack(Items.COOKED_BEEF, 4 + RNG.nextInt(4));
            return new ItemStack(Items.GOLDEN_CARROT, 4 + RNG.nextInt(4));
        }

        if (hasBrokenTool) {
            Item tier = getReplacementTier(brokenTool);
            return tier.stack;
        }

        int y = (int) player.getY();
        for (int i = 0; i < ORE_Y_MIN.length; i++) {
            if (y >= ORE_Y_MIN[i] && y <= ORE_Y_MAX[i]) {
                int amount = ORE_AMOUNT_MIN[i] + RNG.nextInt(ORE_AMOUNT_MAX[i] - ORE_AMOUNT_MIN[i] + 1);
                return new ItemStack(ORE_ITEMS[i].getItem(), amount);
            }
        }

        int fallback = RNG.nextInt(4);
        if (fallback == 0) return new ItemStack(Items.IRON_INGOT, 8 + RNG.nextInt(8));
        if (fallback == 1) return new ItemStack(Items.DIAMOND, 2 + RNG.nextInt(4));
        if (fallback == 2) return new ItemStack(Items.EMERALD, 4 + RNG.nextInt(8));
        return new ItemStack(Items.GOLD_INGOT, 6 + RNG.nextInt(6));
    }

    private static record Item(ItemStack stack) {}

    private static final Item IRON_TOOL = new Item(new ItemStack(Items.IRON_PICKAXE));
    private static final Item DIAMOND_TOOL = new Item(new ItemStack(Items.DIAMOND_PICKAXE));
    private static final Item NETHERITE_TOOL = new Item(new ItemStack(Items.NETHERITE_PICKAXE));

    private static Item getReplacementTier(ItemStack broken) {
        if (broken.getItem() == Items.NETHERITE_PICKAXE || broken.getItem() == Items.NETHERITE_SWORD || broken.getItem() == Items.NETHERITE_AXE) return NETHERITE_TOOL;
        if (broken.getItem() == Items.DIAMOND_PICKAXE || broken.getItem() == Items.DIAMOND_SWORD || broken.getItem() == Items.DIAMOND_AXE) return NETHERITE_TOOL;
        if (broken.getItem() == Items.IRON_PICKAXE || broken.getItem() == Items.IRON_SWORD || broken.getItem() == Items.IRON_AXE) return DIAMOND_TOOL;
        return IRON_TOOL;
    }

    public static void forceBad(ServerPlayer player) {
        currentType = DayType.BAD;
        ticksRemaining = 24000L * AbnormalitiesConfig.PEAK_DAY_DURATION.get();
        LOGGER.info("[PeakDay] {} forced bad day", player.getName().getString());
        save();
    }

    public static void forceGood(ServerPlayer player) {
        currentType = DayType.GOOD;
        ticksRemaining = 24000L * AbnormalitiesConfig.PEAK_DAY_DURATION.get();
        giftsSpawned = 0;
        LOGGER.info("[PeakDay] {} forced good day", player.getName().getString());
        save();
    }

    private static void save() {
        save(false);
    }

    private static void save(boolean force) {
        if (dataFile == null) return;
        long now = System.currentTimeMillis();
        if (!force && now - lastSaveTime < SAVE_THROTTLE_MS) return;
        lastSaveTime = now;
        CompoundTag tag = new CompoundTag();
        tag.putString("type", currentType.name());
        tag.putLong("ticks", ticksRemaining);
        tag.putInt("gifts", giftsSpawned);
        try {
            dataFile.getParentFile().mkdirs();
            NbtIo.writeCompressed(tag, dataFile);
        } catch (IOException ignored) {}
    }

    private static void load() {
        if (dataFile == null || !dataFile.exists()) return;
        try {
            CompoundTag tag = NbtIo.readCompressed(dataFile);
            if (tag == null) return;
            try {
                currentType = DayType.valueOf(tag.getString("type"));
            } catch (IllegalArgumentException e) {
                currentType = DayType.NORMAL;
            }
            ticksRemaining = tag.getLong("ticks");
            giftsSpawned = tag.getInt("gifts");
            if (ticksRemaining <= 0) {
                ticksRemaining = 24000L;
                currentType = DayType.NORMAL;
            }
        } catch (IOException ignored) {}
    }

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (loaded) return;
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        if (sl.dimension() != Level.OVERWORLD) return;
        dataFile = sl.getServer().getWorldPath(LevelResource.ROOT).resolve("data/abnormalities_peakdays.nbt").toFile();
        load();
        loaded = true;
    }

    @SubscribeEvent
    public static void onWorldSave(LevelEvent.Save event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        if (sl.dimension() != Level.OVERWORLD) return;
        if (dataFile != null) save();
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        if (sl.dimension() != Level.OVERWORLD) return;
        if (dataFile != null) save(true);
        loaded = false;
        dataFile = null;
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (dataFile != null) save(true);
    }
}
