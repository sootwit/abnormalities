package com.abnormalities.entity;

import com.abnormalities.ReputationManager;
import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class XyzEntity extends Mob {
    private static final EntityDataAccessor<Boolean> DATA_ACTIVE = SynchedEntityData.defineId(XyzEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SECONDS_LEFT = SynchedEntityData.defineId(XyzEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_AMOUNT = SynchedEntityData.defineId(XyzEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_REQUEST_NAME = SynchedEntityData.defineId(XyzEntity.class, EntityDataSerializers.STRING);

    public UUID targetUUID;
    public Player targetPlayer;
    private ResourceLocation requestedItemId;
    private Item requestedItem;
    private int timerTicks;
    private boolean messageSent;
    private boolean rewardGiven;
    private boolean hasFailed;
    private int fadeOutTick;

    private static final ResourceLocation XYZ_ITEMS_TAG = new ResourceLocation("abnormalities", "xyz_items");

    public static net.minecraft.world.item.Item pickNearbyItem(net.minecraft.server.level.ServerLevel level, double cx, double cz) {
        java.util.HashSet<net.minecraft.world.item.Item> found = new java.util.HashSet<>();
        java.util.Set<net.minecraft.world.level.block.Block> silkTouch = java.util.Set.of(
            net.minecraft.world.level.block.Blocks.GRASS_BLOCK, net.minecraft.world.level.block.Blocks.MYCELIUM,
            net.minecraft.world.level.block.Blocks.PODZOL, net.minecraft.world.level.block.Blocks.GRASS,
            net.minecraft.world.level.block.Blocks.TALL_GRASS, net.minecraft.world.level.block.Blocks.FERN,
            net.minecraft.world.level.block.Blocks.LARGE_FERN, net.minecraft.world.level.block.Blocks.DEAD_BUSH,
            net.minecraft.world.level.block.Blocks.VINE, net.minecraft.world.level.block.Blocks.GLOW_LICHEN,
            net.minecraft.world.level.block.Blocks.SEA_PICKLE,
            net.minecraft.world.level.block.Blocks.TUBE_CORAL_BLOCK, net.minecraft.world.level.block.Blocks.BRAIN_CORAL_BLOCK,
            net.minecraft.world.level.block.Blocks.BUBBLE_CORAL_BLOCK, net.minecraft.world.level.block.Blocks.FIRE_CORAL_BLOCK,
            net.minecraft.world.level.block.Blocks.HORN_CORAL_BLOCK,
            net.minecraft.world.level.block.Blocks.ICE, net.minecraft.world.level.block.Blocks.PACKED_ICE,
            net.minecraft.world.level.block.Blocks.BLUE_ICE, net.minecraft.world.level.block.Blocks.FROSTED_ICE,
            net.minecraft.world.level.block.Blocks.SPAWNER, net.minecraft.world.level.block.Blocks.GLASS,
            net.minecraft.world.level.block.Blocks.GLASS_PANE, net.minecraft.world.level.block.Blocks.SCULK,
            net.minecraft.world.level.block.Blocks.SCULK_CATALYST, net.minecraft.world.level.block.Blocks.SCULK_SHRIEKER,
            net.minecraft.world.level.block.Blocks.SCULK_SENSOR, net.minecraft.world.level.block.Blocks.CALIBRATED_SCULK_SENSOR,
            net.minecraft.world.level.block.Blocks.COAL_ORE, net.minecraft.world.level.block.Blocks.DEEPSLATE_COAL_ORE,
            net.minecraft.world.level.block.Blocks.IRON_ORE, net.minecraft.world.level.block.Blocks.DEEPSLATE_IRON_ORE,
            net.minecraft.world.level.block.Blocks.COPPER_ORE, net.minecraft.world.level.block.Blocks.DEEPSLATE_COPPER_ORE,
            net.minecraft.world.level.block.Blocks.GOLD_ORE, net.minecraft.world.level.block.Blocks.DEEPSLATE_GOLD_ORE,
            net.minecraft.world.level.block.Blocks.DIAMOND_ORE, net.minecraft.world.level.block.Blocks.DEEPSLATE_DIAMOND_ORE,
            net.minecraft.world.level.block.Blocks.EMERALD_ORE, net.minecraft.world.level.block.Blocks.DEEPSLATE_EMERALD_ORE,
            net.minecraft.world.level.block.Blocks.LAPIS_ORE, net.minecraft.world.level.block.Blocks.DEEPSLATE_LAPIS_ORE,
            net.minecraft.world.level.block.Blocks.REDSTONE_ORE, net.minecraft.world.level.block.Blocks.DEEPSLATE_REDSTONE_ORE,
            net.minecraft.world.level.block.Blocks.NETHER_GOLD_ORE, net.minecraft.world.level.block.Blocks.NETHER_QUARTZ_ORE,
            net.minecraft.world.level.block.Blocks.ANCIENT_DEBRIS,
            net.minecraft.world.level.block.Blocks.DEEPSLATE, net.minecraft.world.level.block.Blocks.TUFF,
            net.minecraft.world.level.block.Blocks.SPONGE, net.minecraft.world.level.block.Blocks.WET_SPONGE,
            net.minecraft.world.level.block.Blocks.CLAY, net.minecraft.world.level.block.Blocks.BOOKSHELF,
            net.minecraft.world.level.block.Blocks.CHISELED_BOOKSHELF, net.minecraft.world.level.block.Blocks.MELON,
            net.minecraft.world.level.block.Blocks.PUMPKIN, net.minecraft.world.level.block.Blocks.CARVED_PUMPKIN,
            net.minecraft.world.level.block.Blocks.JACK_O_LANTERN, net.minecraft.world.level.block.Blocks.TNT,
            net.minecraft.world.level.block.Blocks.ENDER_CHEST, net.minecraft.world.level.block.Blocks.CRYING_OBSIDIAN,
            net.minecraft.world.level.block.Blocks.CAMPFIRE, net.minecraft.world.level.block.Blocks.SOUL_CAMPFIRE,
            net.minecraft.world.level.block.Blocks.CRIMSON_NYLIUM,
            net.minecraft.world.level.block.Blocks.WARPED_NYLIUM, net.minecraft.world.level.block.Blocks.ROOTED_DIRT,
            net.minecraft.world.level.block.Blocks.INFESTED_STONE, net.minecraft.world.level.block.Blocks.INFESTED_COBBLESTONE,
            net.minecraft.world.level.block.Blocks.INFESTED_STONE_BRICKS,
            net.minecraft.world.level.block.Blocks.INFESTED_MOSSY_STONE_BRICKS,
            net.minecraft.world.level.block.Blocks.INFESTED_CRACKED_STONE_BRICKS,
            net.minecraft.world.level.block.Blocks.INFESTED_CHISELED_STONE_BRICKS,
            net.minecraft.world.level.block.Blocks.INFESTED_DEEPSLATE,
            net.minecraft.world.level.block.Blocks.FARMLAND, net.minecraft.world.level.block.Blocks.DIRT_PATH,
            net.minecraft.world.level.block.Blocks.POWDER_SNOW, net.minecraft.world.level.block.Blocks.SNOW,
            net.minecraft.world.level.block.Blocks.SNOW_BLOCK, net.minecraft.world.level.block.Blocks.SUGAR_CANE,
            net.minecraft.world.level.block.Blocks.PINK_PETALS, net.minecraft.world.level.block.Blocks.BUDDING_AMETHYST,
            net.minecraft.world.level.block.Blocks.AMETHYST_CLUSTER, net.minecraft.world.level.block.Blocks.SMALL_AMETHYST_BUD,
            net.minecraft.world.level.block.Blocks.MEDIUM_AMETHYST_BUD, net.minecraft.world.level.block.Blocks.LARGE_AMETHYST_BUD,
            net.minecraft.world.level.block.Blocks.BEE_NEST, net.minecraft.world.level.block.Blocks.BEEHIVE,
            net.minecraft.world.level.block.Blocks.COCOA, net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH,
            net.minecraft.world.level.block.Blocks.CAVE_VINES, net.minecraft.world.level.block.Blocks.CAVE_VINES_PLANT,
            net.minecraft.world.level.block.Blocks.TWISTING_VINES, net.minecraft.world.level.block.Blocks.TWISTING_VINES_PLANT,
            net.minecraft.world.level.block.Blocks.WEEPING_VINES, net.minecraft.world.level.block.Blocks.WEEPING_VINES_PLANT,
            net.minecraft.world.level.block.Blocks.BIG_DRIPLEAF, net.minecraft.world.level.block.Blocks.SMALL_DRIPLEAF,
            net.minecraft.world.level.block.Blocks.SPORE_BLOSSOM, net.minecraft.world.level.block.Blocks.HANGING_ROOTS,
            net.minecraft.world.level.block.Blocks.MOSS_BLOCK, net.minecraft.world.level.block.Blocks.MOSS_CARPET,
            net.minecraft.world.level.block.Blocks.AZALEA, net.minecraft.world.level.block.Blocks.FLOWERING_AZALEA,
            net.minecraft.world.level.block.Blocks.BAMBOO, net.minecraft.world.level.block.Blocks.BAMBOO_SAPLING,
            net.minecraft.world.level.block.Blocks.OAK_LEAVES, net.minecraft.world.level.block.Blocks.SPRUCE_LEAVES,
            net.minecraft.world.level.block.Blocks.BIRCH_LEAVES, net.minecraft.world.level.block.Blocks.JUNGLE_LEAVES,
            net.minecraft.world.level.block.Blocks.ACACIA_LEAVES, net.minecraft.world.level.block.Blocks.DARK_OAK_LEAVES,
            net.minecraft.world.level.block.Blocks.MANGROVE_LEAVES, net.minecraft.world.level.block.Blocks.AZALEA_LEAVES,
            net.minecraft.world.level.block.Blocks.FLOWERING_AZALEA_LEAVES, net.minecraft.world.level.block.Blocks.CHERRY_LEAVES,
            net.minecraft.world.level.block.Blocks.TUBE_CORAL, net.minecraft.world.level.block.Blocks.BRAIN_CORAL,
            net.minecraft.world.level.block.Blocks.BUBBLE_CORAL, net.minecraft.world.level.block.Blocks.FIRE_CORAL,
            net.minecraft.world.level.block.Blocks.HORN_CORAL,
            net.minecraft.world.level.block.Blocks.TUBE_CORAL_FAN, net.minecraft.world.level.block.Blocks.BRAIN_CORAL_FAN,
            net.minecraft.world.level.block.Blocks.BUBBLE_CORAL_FAN, net.minecraft.world.level.block.Blocks.FIRE_CORAL_FAN,
            net.minecraft.world.level.block.Blocks.HORN_CORAL_FAN,
            net.minecraft.world.level.block.Blocks.DEAD_TUBE_CORAL, net.minecraft.world.level.block.Blocks.DEAD_BRAIN_CORAL,
            net.minecraft.world.level.block.Blocks.DEAD_BUBBLE_CORAL, net.minecraft.world.level.block.Blocks.DEAD_FIRE_CORAL,
            net.minecraft.world.level.block.Blocks.DEAD_HORN_CORAL,
            net.minecraft.world.level.block.Blocks.DEAD_TUBE_CORAL_FAN, net.minecraft.world.level.block.Blocks.DEAD_BRAIN_CORAL_FAN,
            net.minecraft.world.level.block.Blocks.DEAD_BUBBLE_CORAL_FAN, net.minecraft.world.level.block.Blocks.DEAD_FIRE_CORAL_FAN,
            net.minecraft.world.level.block.Blocks.DEAD_HORN_CORAL_FAN,
            net.minecraft.world.level.block.Blocks.COBWEB, net.minecraft.world.level.block.Blocks.SCULK_VEIN,
            net.minecraft.world.level.block.Blocks.CONDUIT, net.minecraft.world.level.block.Blocks.POINTED_DRIPSTONE,
            net.minecraft.world.level.block.Blocks.TURTLE_EGG, net.minecraft.world.level.block.Blocks.FROGSPAWN,
            net.minecraft.world.level.block.Blocks.SLIME_BLOCK, net.minecraft.world.level.block.Blocks.HONEY_BLOCK
        );
        java.util.Set<net.minecraft.world.level.block.Block> unobtainable = java.util.Set.of(
            net.minecraft.world.level.block.Blocks.BEDROCK, net.minecraft.world.level.block.Blocks.BARRIER,
            net.minecraft.world.level.block.Blocks.COMMAND_BLOCK, net.minecraft.world.level.block.Blocks.CHAIN_COMMAND_BLOCK,
            net.minecraft.world.level.block.Blocks.REPEATING_COMMAND_BLOCK, net.minecraft.world.level.block.Blocks.STRUCTURE_BLOCK,
            net.minecraft.world.level.block.Blocks.JIGSAW, net.minecraft.world.level.block.Blocks.STRUCTURE_VOID,
            net.minecraft.world.level.block.Blocks.END_PORTAL, net.minecraft.world.level.block.Blocks.END_PORTAL_FRAME,
            net.minecraft.world.level.block.Blocks.END_GATEWAY, net.minecraft.world.level.block.Blocks.NETHER_PORTAL,
            net.minecraft.world.level.block.Blocks.AIR, net.minecraft.world.level.block.Blocks.CAVE_AIR,
            net.minecraft.world.level.block.Blocks.VOID_AIR, net.minecraft.world.level.block.Blocks.WATER,
            net.minecraft.world.level.block.Blocks.LAVA, net.minecraft.world.level.block.Blocks.MOVING_PISTON,
            net.minecraft.world.level.block.Blocks.PISTON_HEAD, net.minecraft.world.level.block.Blocks.PISTON,
            net.minecraft.world.level.block.Blocks.STICKY_PISTON, net.minecraft.world.level.block.Blocks.LIGHT,
            net.minecraft.world.level.block.Blocks.SPAWNER
        );
        int bx = net.minecraft.util.Mth.floor(cx) >> 4;
        int bz = net.minecraft.util.Mth.floor(cz) >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                var chunk = level.getChunk(bx + dx, bz + dz);
                for (var entry : chunk.getBlockEntities().entrySet()) {}
                int cx2 = (bx + dx) << 4;
                int cz2 = (bz + dz) << 4;
                for (int x = cx2; x < cx2 + 16; x++) {
                    for (int z = cz2; z < cz2 + 16; z++) {
                        int sy = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
                        for (int y = sy - 5; y <= sy + 5; y++) {
                            if (y < level.getMinBuildHeight() || y > level.getMaxBuildHeight()) continue;
                            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(new net.minecraft.core.BlockPos(x, y, z));
                            if (state.isAir()) continue;
                            var item = state.getBlock().asItem();
                            if (item == null || item == net.minecraft.world.item.Items.AIR) continue;
                            if (silkTouch.contains(state.getBlock())) continue;
                            if (unobtainable.contains(state.getBlock())) continue;
                            found.add(item);
                            if (found.size() >= 30) break;
                        }
                        if (found.size() >= 30) break;
                    }
                    if (found.size() >= 30) break;
                }
                if (found.size() >= 30) break;
            }
            if (found.size() >= 30) break;
        }
        if (found.isEmpty()) return net.minecraft.world.item.Items.COBBLESTONE;
        return new java.util.ArrayList<>(found).get(level.random.nextInt(found.size()));
    }

    public XyzEntity(EntityType<? extends XyzEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setPersistenceRequired();
        this.noCulling = true;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ACTIVE, false);
        this.entityData.define(DATA_SECONDS_LEFT, 0);
        this.entityData.define(DATA_AMOUNT, 1);
        this.entityData.define(DATA_REQUEST_NAME, "");
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 9999.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean isPushedByFluid() { return false; }

    @Override
    protected boolean canRide(net.minecraft.world.entity.Entity vehicle) { return false; }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        if (!hasFailed && !rewardGiven) {
            if (source.getEntity() instanceof Player attacker) {
                triggerFailure(attacker);
            } else if (targetPlayer != null) {
                triggerFailure(targetPlayer);
            }
        }
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        this.setDeltaMovement(0, 0, 0);

        if (hasFailed || rewardGiven) return;

        if (targetPlayer == null || targetPlayer.isRemoved() || !targetPlayer.isAlive()) {
            if (targetUUID != null && level().getServer() != null) {
                targetPlayer = level().getServer().getPlayerList().getPlayer(targetUUID);
            }
            if (targetPlayer == null) return;
        }

        if (!messageSent) return;

        timerTicks--;
        int secondsLeft = Math.max(0, (timerTicks + 19) / 20);
        this.entityData.set(DATA_SECONDS_LEFT, secondsLeft);

        int mins = secondsLeft / 60;
        int secs = secondsLeft % 60;
        String timeStr = String.format("%02d:%02d", mins, secs);
        Component hotbar = Component.literal(timeStr + " - " + entityData.get(DATA_AMOUNT) + "x " + getRequestedItemName())
                .withStyle(ChatFormatting.LIGHT_PURPLE);
        if (targetPlayer instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetActionBarTextPacket(hotbar));
        }

        if (timerTicks <= 0) {
            triggerFailure(targetPlayer);
            return;
        }

        for (ItemEntity itemEntity : level().getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(3.0D))) {
            if (!(itemEntity.getOwner() instanceof Player owner) || owner != targetPlayer) continue;
            ItemStack dropped = itemEntity.getItem();
            if (isRequestedItem(dropped)) {
                int amount = this.entityData.get(DATA_AMOUNT);
                if (dropped.getCount() >= amount) {
                    if (!level().isClientSide) {
                        deliverItem(targetPlayer);
                        dropped.shrink(amount);
                        if (dropped.isEmpty()) {
                            itemEntity.discard();
                        }
                    }
                    return;
                }
            }
        }
    }

    public void setTargetPlayer(Player player) {
        this.targetPlayer = player;
        this.targetUUID = player.getUUID();
    }

    public void startRequest(int amount, Item item, int seconds) {
        this.entityData.set(DATA_AMOUNT, amount);
        this.requestedItem = item;
        this.requestedItemId = BuiltInRegistries.ITEM.getKey(item);
        this.entityData.set(DATA_REQUEST_NAME, new ItemStack(item).getHoverName().getString());
        this.messageSent = false;
        this.rewardGiven = false;
        this.hasFailed = false;
        this.fadeOutTick = 0;

        this.timerTicks = seconds * 20;
        this.entityData.set(DATA_SECONDS_LEFT, seconds);
        this.entityData.set(DATA_ACTIVE, true);
    }

    private boolean isRequestedItem(ItemStack stack) {
        if (requestedItem == null) return false;
        return stack.is(requestedItem);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide || hasFailed || rewardGiven || !messageSent) return InteractionResult.PASS;
        if (player != targetPlayer) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);
        int amount = this.entityData.get(DATA_AMOUNT);

        if (isRequestedItem(held) && held.getCount() >= amount) {
            deliverItem(player);
            held.shrink(amount);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private void deliverItem(Player player) {
        if (rewardGiven) return;
        rewardGiven = true;
        this.entityData.set(DATA_ACTIVE, false);

        ReputationManager.addRep(player, 50);

        if (player instanceof ServerPlayer sp) {
            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSystemChatPacket(
                    Component.literal("thank you.").withStyle(ChatFormatting.LIGHT_PURPLE), false));
        }

        int cookieAmt = AbnormalitiesConfig.XYZ_REWARD_COOKIES.get();
        int carrotAmt = AbnormalitiesConfig.XYZ_REWARD_GOLDEN_CARROTS.get();
        int appleAmt = AbnormalitiesConfig.XYZ_REWARD_GOLDEN_APPLES.get();
        ItemStack reward = rollGoody(cookieAmt, carrotAmt, appleAmt);
        ItemStack reward2 = rollExtra();

        BlockPos chestPos = this.blockPosition();
        if (!level().getBlockState(chestPos).canBeReplaced()) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos adj = chestPos.offset(dx, 0, dz);
                    if (level().getBlockState(adj).canBeReplaced()) { chestPos = adj; break; }
                }
                if (chestPos != this.blockPosition()) break;
            }
        }
        level().setBlockAndUpdate(chestPos, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
        if (level().getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
            chest.setItem(0, reward);
            if (!reward2.isEmpty()) chest.setItem(1, reward2);
        }

        level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.MASTER, 3.0f, 1.5f);
        level().playSound(null, chestPos.getX(), chestPos.getY(), chestPos.getZ(),
                SoundEvents.CHEST_OPEN, SoundSource.MASTER, 1.0f, 1.0f);

        if (targetPlayer instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetActionBarTextPacket(Component.empty()));
        }
        discard();
    }

    private ItemStack rollGoody(int cookieAmt, int carrotAmt, int appleAmt) {
        int roll = level().random.nextInt(1000);
        if (roll < 120) return new ItemStack(Items.COOKIE, cookieAmt);
        if (roll < 230) return new ItemStack(Items.GOLDEN_CARROT, carrotAmt);
        if (roll < 330) return new ItemStack(Items.GOLDEN_APPLE, appleAmt);
        if (roll < 480) return new ItemStack(Items.IRON_INGOT, 12 + level().random.nextInt(9));
        if (roll < 610) return new ItemStack(Items.EMERALD, 8 + level().random.nextInt(7));
        if (roll < 720) return new ItemStack(Items.DIAMOND, 3 + level().random.nextInt(4));
        if (roll < 800) return new ItemStack(Items.DIAMOND_PICKAXE);
        if (roll < 860) return new ItemStack(Items.IRON_PICKAXE);
        if (roll < 920) return new ItemStack(Items.DIAMOND_SWORD);
        if (roll < 980) return new ItemStack(Items.HEART_OF_THE_SEA);
        if (level().random.nextInt(2) == 0) return new ItemStack(Items.NETHERITE_INGOT, 2);
        return new ItemStack(Items.ELYTRA);
    }

    private ItemStack rollExtra() {
        int roll = level().random.nextInt(1000);
        if (roll < 550) return ItemStack.EMPTY;
        if (roll < 700) return new ItemStack(Items.EMERALD, 4 + level().random.nextInt(4));
        if (roll < 820) return new ItemStack(Items.IRON_INGOT, 6 + level().random.nextInt(6));
        if (roll < 910) return new ItemStack(Items.DIAMOND, 1 + level().random.nextInt(3));
        if (roll < 970) return new ItemStack(Items.NETHERITE_SCRAP, 1 + level().random.nextInt(2));
        return new ItemStack(Items.ELYTRA);
    }

    private void triggerFailure(Player attacker) {
        if (hasFailed) return;
        hasFailed = true;
        this.entityData.set(DATA_ACTIVE, false);

        Player victim = attacker != null ? attacker : targetPlayer;
        if (victim == null || level().isClientSide) return;

        ReputationManager.addRep(victim, -100);

        ServerLevel serverLevel = (ServerLevel) level();
        long currentDayTime = serverLevel.getDayTime();
        long timeOfDay = currentDayTime % 24000L;

        if (timeOfDay < 13000L) {
            long jumpTo = currentDayTime + (13000L - timeOfDay) + 100;
            if (victim instanceof net.minecraft.server.level.ServerPlayer) {
                ((net.minecraft.server.level.ServerPlayer) victim).connection.send(
                    new net.minecraft.network.protocol.game.ClientboundSetTimePacket(serverLevel.getGameTime(), jumpTo, true));
            }
        } else {
            if (victim instanceof net.minecraft.server.level.ServerPlayer) {
                ((net.minecraft.server.level.ServerPlayer) victim).connection.send(
                    new net.minecraft.network.protocol.game.ClientboundSetTimePacket(serverLevel.getGameTime(), currentDayTime + 100, true));
            }
        }

        level().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                ModSounds.NUR_SOUND.get(), SoundSource.MASTER, 6.0f, 0.3f);

        String[] spawnMessages = {
                "PRAY.", "HOPE.", "LIFE.", "SOUL."
        };

        for (int i = 0; i < 4; i++) {
            String msg = spawnMessages[i];
            var srv = serverLevel.getServer();
            if (srv != null) {
                for (var p : srv.getPlayerList().getPlayers()) {
                    p.connection.send(new net.minecraft.network.protocol.game.ClientboundSystemChatPacket(
                            Component.literal(msg).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false));
                }
            }

            double angle = level().random.nextDouble() * Math.PI * 2;
            double dist = 4.0D + level().random.nextDouble() * 8.0D;
            double sx = this.getX() + Math.cos(angle) * dist;
            double sz = this.getZ() + Math.sin(angle) * dist;
            int sy = serverLevel.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) sx, (int) sz);

            NurEntity nur = com.abnormalities.registry.ModEntities.NUR.get().create(serverLevel);
            if (nur != null) {
                nur.moveTo(sx + 0.5, sy, sz + 0.5, 0, 0);
                serverLevel.addFreshEntity(nur);
                nur.startChasing(victim);
            }
        }

        var nearbyNurs = level().getEntitiesOfClass(NurEntity.class, this.getBoundingBox().inflate(128.0D));
        for (NurEntity nur : nearbyNurs) {
            if (nur.currentState == NurEntity.State.STALKING) {
                nur.startChasing(victim);
            }
        }

        if (victim instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetActionBarTextPacket(Component.empty()));
        }
        discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (targetUUID != null) {
            tag.putUUID("TargetPlayer", targetUUID);
        }
        if (requestedItemId != null) {
            tag.putString("RequestedItemId", requestedItemId.toString());
        }
        tag.putInt("TimerTicks", timerTicks);
        tag.putBoolean("MessageSent", messageSent);
        tag.putBoolean("RewardGiven", rewardGiven);
        tag.putBoolean("HasFailed", hasFailed);
        tag.putInt("Amount", this.entityData.get(DATA_AMOUNT));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("TargetPlayer")) {
            this.targetUUID = tag.getUUID("TargetPlayer");
            if (level() != null && level().getServer() != null) {
                this.targetPlayer = level().getServer().getPlayerList().getPlayer(this.targetUUID);
            }
        }
        if (tag.contains("RequestedItemId")) {
            this.requestedItemId = new ResourceLocation(tag.getString("RequestedItemId"));
            this.requestedItem = BuiltInRegistries.ITEM.get(this.requestedItemId);
            this.entityData.set(DATA_REQUEST_NAME, new ItemStack(this.requestedItem).getHoverName().getString());
        }
        this.timerTicks = tag.getInt("TimerTicks");
        this.messageSent = tag.getBoolean("MessageSent");
        this.rewardGiven = tag.getBoolean("RewardGiven");
        this.hasFailed = tag.getBoolean("HasFailed");
        this.entityData.set(DATA_AMOUNT, tag.getInt("Amount"));
        this.entityData.set(DATA_ACTIVE, !hasFailed && !rewardGiven && timerTicks > 0);
        this.setPersistenceRequired();
        this.noCulling = true;
    }

    public boolean isActive() {
        return this.entityData.get(DATA_ACTIVE);
    }

    public int getSecondsLeft() {
        return this.entityData.get(DATA_SECONDS_LEFT);
    }

    public int getAmount() {
        return this.entityData.get(DATA_AMOUNT);
    }

    public Item getRequestedItem() {
        return requestedItem;
    }

    public String getRequestedItemName() {
        if (requestedItem == null) return this.entityData.get(DATA_REQUEST_NAME).isEmpty() ? "something" : this.entityData.get(DATA_REQUEST_NAME);
        return new ItemStack(requestedItem).getHoverName().getString();
    }

    public void setMessageSent(boolean sent) {
        this.messageSent = sent;
    }

    public boolean hasMessageSent() {
        return messageSent;
    }

    public Player getTargetPlayer() {
        return targetPlayer;
    }
}
