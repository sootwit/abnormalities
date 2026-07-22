package com.abnormalities.registry;

import com.abnormalities.ReputationManager;
import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.entity.NurEntity;
import com.abnormalities.entity.XyzEntity;
import com.abnormalities.entity.skinwalker.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ModEvents {
    private static final String[] PRE_SPAWN_TEXTS = {"PRAY.", "HOPE.", "LIFE.", "SOUL."};
    private static final List<SpawnTask> PENDING_SPAWNS = new ArrayList<>();
    private static final Set<Block> SEE_THROUGH = Set.of(
            Blocks.AIR, Blocks.CAVE_AIR, Blocks.VOID_AIR,
            Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES,
            Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES,
            Blocks.MANGROVE_LEAVES, Blocks.AZALEA_LEAVES, Blocks.FLOWERING_AZALEA_LEAVES,
            Blocks.CHERRY_LEAVES,
            Blocks.GLASS, Blocks.GLASS_PANE,
            Blocks.WHITE_STAINED_GLASS, Blocks.ORANGE_STAINED_GLASS,
            Blocks.MAGENTA_STAINED_GLASS, Blocks.LIGHT_BLUE_STAINED_GLASS,
            Blocks.YELLOW_STAINED_GLASS, Blocks.LIME_STAINED_GLASS,
            Blocks.PINK_STAINED_GLASS, Blocks.GRAY_STAINED_GLASS,
            Blocks.LIGHT_GRAY_STAINED_GLASS, Blocks.CYAN_STAINED_GLASS,
            Blocks.PURPLE_STAINED_GLASS, Blocks.BLUE_STAINED_GLASS,
            Blocks.BROWN_STAINED_GLASS, Blocks.GREEN_STAINED_GLASS,
            Blocks.RED_STAINED_GLASS, Blocks.BLACK_STAINED_GLASS,
            Blocks.WHITE_STAINED_GLASS_PANE, Blocks.ORANGE_STAINED_GLASS_PANE,
            Blocks.MAGENTA_STAINED_GLASS_PANE, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE,
            Blocks.YELLOW_STAINED_GLASS_PANE, Blocks.LIME_STAINED_GLASS_PANE,
            Blocks.PINK_STAINED_GLASS_PANE, Blocks.GRAY_STAINED_GLASS_PANE,
            Blocks.LIGHT_GRAY_STAINED_GLASS_PANE, Blocks.CYAN_STAINED_GLASS_PANE,
            Blocks.PURPLE_STAINED_GLASS_PANE, Blocks.BLUE_STAINED_GLASS_PANE,
            Blocks.BROWN_STAINED_GLASS_PANE, Blocks.GREEN_STAINED_GLASS_PANE,
            Blocks.RED_STAINED_GLASS_PANE, Blocks.BLACK_STAINED_GLASS_PANE,
            Blocks.TINTED_GLASS, Blocks.IRON_BARS, Blocks.COBWEB,
            Blocks.GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN,
            Blocks.SWEET_BERRY_BUSH, Blocks.VINE,
            Blocks.TORCH, Blocks.WALL_TORCH, Blocks.REDSTONE_TORCH, Blocks.REDSTONE_WALL_TORCH,
            Blocks.WATER, Blocks.LAVA
    );

    private static class SpawnTask {
        int ticksRemaining;
        final ServerLevel level;
        final java.util.UUID playerUUID;
        final double angle;
        final double dist;
        SpawnTask(int delay, double angle, double dist, ServerLevel level, java.util.UUID playerUUID) {
            this.ticksRemaining = delay;
            this.angle = angle;
            this.dist = dist;
            this.level = level;
            this.playerUUID = playerUUID;
        }
    }

    private static class SkinwalkerSpawnTask {
        int ticksRemaining;
        final double x, y, z;
        final ServerLevel level;
        final java.util.UUID targetUUID;
        SkinwalkerSpawnTask(int delay, double x, double y, double z, ServerLevel level, java.util.UUID targetUUID) {
            this.ticksRemaining = delay;
            this.x = x;
            this.y = y;
            this.z = z;
            this.level = level;
            this.targetUUID = targetUUID;
        }
    }
    private static final List<SkinwalkerSpawnTask> PENDING_SKINWALKER_SPAWNS = new ArrayList<>();
    private static final Map<UUID, int[]> SW_CHUNKS = new HashMap<>();
    private static final Map<String, Integer> SW_RELEASE_QUEUE = new HashMap<>();
    private static final Map<UUID, Integer> REP_LOOK_TICKS = new HashMap<>();

    public static void scheduleSkinwalkerSpawn(int delay, double x, double y, double z, ServerLevel level, java.util.UUID targetUUID) {
        PENDING_SKINWALKER_SPAWNS.add(new SkinwalkerSpawnTask(delay, x, y, z, level, targetUUID));
    }

    public static void forceNurSpawn(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        String text = PRE_SPAWN_TEXTS[level.random.nextInt(PRE_SPAWN_TEXTS.length)];
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSystemChatPacket(
                Component.literal(text).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMBIENT_CAVE.get(), SoundSource.MASTER, 6.0f, 0.3f);
        double angle = level.random.nextDouble() * Math.PI * 2;
        double dist = 10.0D + level.random.nextDouble() * 15.0D;
        PENDING_SPAWNS.add(new SpawnTask(100, angle, dist, level, player.getUUID()));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ServerLevel overworld = ServerLifecycleHooks.getCurrentServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        Iterator<SpawnTask> it = PENDING_SPAWNS.iterator();
        while (it.hasNext()) {
            SpawnTask task = it.next();
            task.ticksRemaining--;
            if (task.ticksRemaining <= 0) {
                it.remove();
                Player target = task.level.getServer().getPlayerList().getPlayer(task.playerUUID);
                if (target == null) continue;
                double sx = target.getX() + Math.cos(task.angle) * task.dist;
                double sz = target.getZ() + Math.sin(task.angle) * task.dist;
                int sy = task.level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) sx, (int) sz);
                NurEntity nur = ModEntities.NUR.get().create(task.level);
                if (nur == null) continue;
                nur.moveTo(sx + 0.5, sy + 1, sz + 0.5, 0, 0);
                nur.currentState = NurEntity.State.STALKING;
                task.level.addFreshEntity(nur);
                task.level.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.AMBIENT_CAVE.get(), SoundSource.MASTER, 6.0f, 0.3f);
            }
        }
        Iterator<SkinwalkerSpawnTask> sit = PENDING_SKINWALKER_SPAWNS.iterator();
        while (sit.hasNext()) {
            SkinwalkerSpawnTask task = sit.next();
            task.ticksRemaining--;
            if (task.ticksRemaining > 0) continue;
            sit.remove();
            NurEntity nur = ModEntities.NUR.get().create(task.level);
            if (nur == null) continue;
            nur.moveTo(task.x, task.y, task.z, 0, 0);
            Player target = task.level.getServer().getPlayerList().getPlayer(task.targetUUID);
            if (target != null) nur.startChasing(target);
            task.level.addFreshEntity(nur);
        }
        long currentDay = overworld.getDayTime() / 24000L;
        if (currentDay < AbnormalitiesConfig.GRACE_PERIOD_DAYS.get()) return;
        long time = overworld.getDayTime() % 24000L;

        if (time < 13000L && time > 2000L) {
            for (Player player : overworld.players()) {
                if (player.tickCount % 40 != 0) continue;
                if (overworld.random.nextInt(AbnormalitiesConfig.XYZ_SPAWN_WEIGHT.get()) != 0) continue;
                boolean alreadyHasXyz = false;
                for (XyzEntity existing : overworld.getEntitiesOfClass(XyzEntity.class, player.getBoundingBox().inflate(256.0D))) {
                    if (existing.getTargetPlayer() == player && existing.isActive()) {
                        alreadyHasXyz = true;
                        break;
                    }
                }
                if (alreadyHasXyz) continue;

                double angle = overworld.random.nextDouble() * Math.PI * 2;
                double dist = 45.0D + overworld.random.nextDouble() * 35.0D;
                double sx = player.getX() + Math.cos(angle) * dist;
                double sz = player.getZ() + Math.sin(angle) * dist;
                int sy = overworld.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) sx, (int) sz);
                BlockPos spawnPos = BlockPos.containing(sx, sy + 1, sz);
                if (!overworld.getBlockState(spawnPos.below()).canOcclude()) continue;
                if (overworld.getBlockState(spawnPos).canOcclude()) continue;

                XyzEntity xyz = ModEntities.XYZ.get().create(overworld);
                if (xyz != null) {
                    xyz.moveTo(sx + 0.5, sy + 1, sz + 0.5, 0, 0);
                    xyz.setTargetPlayer(player);
                    overworld.addFreshEntity(xyz);

                    var tag = net.minecraft.tags.ItemTags.create(new ResourceLocation("abnormalities", "xyz_items"));
                    var items = new java.util.ArrayList<net.minecraft.world.item.Item>();
                    for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                        items.add(holder.value());
                    }
                    if (items.isEmpty()) continue;

                    if (player instanceof ServerPlayer sp && !hasEndAccess(sp)) {
                        var endTag = net.minecraft.tags.ItemTags.create(new ResourceLocation("abnormalities", "xyz_end_items"));
                        var endItems = new java.util.HashSet<net.minecraft.world.item.Item>();
                        for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(endTag)) {
                            endItems.add(holder.value());
                        }
                        items.removeIf(endItems::contains);
                        if (items.isEmpty()) continue;
                    }

                    net.minecraft.world.item.Item chosenItem = items.get(overworld.random.nextInt(items.size()));
                    int maxStack = chosenItem.getMaxStackSize();
                    int amount;
                    if (AbnormalitiesConfig.XYZ_STATIC_AMOUNT.get()) {
                        amount = Math.min(maxStack, AbnormalitiesConfig.XYZ_STATIC_ITEM_COUNT.get());
                    } else {
                        int min = Math.min(maxStack, AbnormalitiesConfig.XYZ_MIN_ITEMS.get());
                        int max = Math.min(maxStack, AbnormalitiesConfig.XYZ_MAX_ITEMS.get());
                        amount = max > min ? min + overworld.random.nextInt(max - min + 1) : min;
                    }
                    int seconds;
                    if (AbnormalitiesConfig.XYZ_STATIC_WAIT.get()) {
                        seconds = AbnormalitiesConfig.XYZ_STATIC_WAIT_SECONDS.get();
                    } else {
                        int min = AbnormalitiesConfig.XYZ_MIN_WAIT.get();
                        int max = AbnormalitiesConfig.XYZ_MAX_WAIT.get();
                        seconds = min + (max > min ? overworld.random.nextInt(max - min + 1) : 0);
                    }
                    xyz.startRequest(amount, chosenItem, seconds);

                    String itemName = new net.minecraft.world.item.ItemStack(chosenItem).getHoverName().getString();
                    String msg;
                    if (amount == 1) {
                        String prefix = "aeiou".indexOf(Character.toLowerCase(itemName.charAt(0))) >= 0 ? "an" : "a";
                        msg = player.getName().getString() + ", bring me " + prefix + " " + itemName + " in " + seconds + "s";
                    } else {
                        msg = player.getName().getString() + ", bring me " + amount + " " + itemName + "s in " + seconds + "s";
                    }
                    if (player instanceof ServerPlayer sp) {
                        sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSystemChatPacket(
                                Component.literal(msg).withStyle(ChatFormatting.LIGHT_PURPLE), false));
                        xyz.setMessageSent(true);
                    }
                }
            }
        }

        if (time >= 2000L && time <= 23000L) {
        for (Player player : overworld.players()) {
            if (player.tickCount % 40 != 0) continue;
            if (overworld.random.nextInt(AbnormalitiesConfig.SW_SPAWN_WEIGHT.get()) != 0) continue;
            double angle = overworld.random.nextDouble() * Math.PI * 2;
            double dist = 35.0D + overworld.random.nextDouble() * 30.0D;
            double sx = player.getX() + Math.cos(angle) * dist;
            double sz = player.getZ() + Math.sin(angle) * dist;
            int sy = overworld.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) sx, (int) sz);
            BlockPos spawnPos = BlockPos.containing(sx, sy + 1, sz);
            if (!overworld.getBlockState(spawnPos.below()).canOcclude()) continue;
            if (overworld.getBlockState(spawnPos).canOcclude()) continue;
            EntityType<?> disguise = pickRandomDisguise(overworld.random);
            if (disguise == null) continue;
            var nearby = overworld.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(64.0D),
                    e -> e.getPersistentData().getBoolean("abnormalities:skinwalker"));
            if (nearby.size() >= 3) continue;
            Entity raw = disguise.create(overworld);
            if (raw instanceof Mob skinwalker) {
                skinwalker.setPersistenceRequired();
                skinwalker.getPersistentData().putBoolean("abnormalities:skinwalker", true);
                skinwalker.goalSelector.addGoal(1, new NurSkinwalkerApproachGoal(skinwalker));
                skinwalker.moveTo(sx + 0.5, sy + 1, sz + 0.5, overworld.random.nextFloat() * 360.0F, 0);
                overworld.addFreshEntity(skinwalker);
                int cx = ((int)Math.floor(sx)) >> 4;
                int cz = ((int)Math.floor(sz)) >> 4;
                overworld.setChunkForced(cx, cz, true);
                SW_CHUNKS.put(skinwalker.getUUID(), new int[]{cx, cz});
            }
        }
        }

        if (time < 13000L || time > 23000L) return;
        for (Player player : overworld.players()) {
            if (player.tickCount % 20 != 0) continue;
            if (overworld.random.nextInt(AbnormalitiesConfig.NUR_SPAWN_WEIGHT.get()) != 0) continue;
            double angle = overworld.random.nextDouble() * Math.PI * 2;
            double dist = 35.0D + overworld.random.nextDouble() * 30.0D;
            String text = PRE_SPAWN_TEXTS[overworld.random.nextInt(PRE_SPAWN_TEXTS.length)];
            if (player instanceof ServerPlayer sp) {
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSystemChatPacket(
                        Component.literal(text).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false));
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMBIENT_CAVE.get(), SoundSource.MASTER, 6.0f, 0.3f);
            PENDING_SPAWNS.add(new SpawnTask(100, angle, dist, overworld, player.getUUID()));
        }
        tickSkinwalkerChunks(overworld);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) return;
        Level level = event.player.level();
        Player player = event.player;
        if (player.tickCount % 2 != 0) return;
        var entities = level.getEntitiesOfClass(NurEntity.class, player.getBoundingBox().inflate(64.0D));
        for (NurEntity nur : entities) {
            if (nur.currentTarget != null && nur.currentTarget != player) continue;
            if ((isPlayerLookingAtEntity(player, nur) || isCursorCloseToHitbox(player, nur))
                    && nur.currentState == NurEntity.State.STALKING) {
                nur.startChasing(player);
                return;
            }
        }
        var nurRep = level.getEntitiesOfClass(NurEntity.class, player.getBoundingBox().inflate(64.0D));
        boolean lookingAtStalking = false;
        boolean hasStalking = false;
        for (NurEntity nur : nurRep) {
            if (nur.currentState == NurEntity.State.STALKING) {
                hasStalking = true;
                if (isPlayerLookingAtEntity(player, nur) || isCursorCloseToHitbox(player, nur)) {
                    lookingAtStalking = true;
                    break;
                }
            }
        }
        UUID puid = player.getUUID();
        int lt = REP_LOOK_TICKS.getOrDefault(puid, 0);
        if (lookingAtStalking) {
            REP_LOOK_TICKS.put(puid, lt + 1);
            if (lt >= 9) {
                ReputationManager.addRep(player, -5);
                REP_LOOK_TICKS.put(puid, 0);
            }
        } else if (hasStalking) {
            REP_LOOK_TICKS.put(puid, lt + 1);
            if (lt >= 9) {
                ReputationManager.addRep(player, 1);
                REP_LOOK_TICKS.put(puid, 0);
            }
        } else if (lt != 0) {
            REP_LOOK_TICKS.put(puid, 0);
        }
    }

    private static boolean isPlayerLookingAtEntity(Player player, NurEntity entity) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getViewVector(1.0F);
        AABB box = entity.getBoundingBox().inflate(0.3D);
        double entityDist = rayToAABB(eyePos, lookVec, box);
        if (entityDist < 0) return false;
        Vec3 current = eyePos;
        double walked = 0;
        while (walked < entityDist) {
            Vec3 rayEnd = eyePos.add(lookVec.scale(entityDist));
            BlockHitResult hit = player.level().clip(new ClipContext(
                    current, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.MISS) return true;
            double hitDist = current.distanceTo(hit.getLocation());
            if (walked + hitDist >= entityDist) return true;
            BlockState state = player.level().getBlockState(hit.getBlockPos());
            if (!SEE_THROUGH.contains(state.getBlock())) return false;
            walked += hitDist + 0.05;
            current = hit.getLocation().add(lookVec.scale(0.05));
        }
        return true;
    }

    private static double rayToAABB(Vec3 origin, Vec3 dir, AABB box) {
        double tmin = Double.NEGATIVE_INFINITY;
        double tmax = Double.POSITIVE_INFINITY;
        double[] originArr = {origin.x, origin.y, origin.z};
        double[] dirArr = {dir.x, dir.y, dir.z};
        double[] boxMin = {box.minX, box.minY, box.minZ};
        double[] boxMax = {box.maxX, box.maxY, box.maxZ};
        for (int i = 0; i < 3; i++) {
            if (Math.abs(dirArr[i]) < 1.0E-10D) {
                if (originArr[i] < boxMin[i] || originArr[i] > boxMax[i]) return -1;
            } else {
                double inv = 1.0D / dirArr[i];
                double t1 = (boxMin[i] - originArr[i]) * inv;
                double t2 = (boxMax[i] - originArr[i]) * inv;
                if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
                tmin = Math.max(tmin, t1);
                tmax = Math.min(tmax, t2);
                if (tmin > tmax) return -1;
            }
        }
        if (tmax < 0) return -1;
        return tmin >= 0 ? tmin : tmax;
    }

    private static boolean isCursorCloseToHitbox(Player player, NurEntity entity) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getViewVector(1.0F);
        AABB box = entity.getBoundingBox();
        double entityDist = rayToAABB(eyePos, lookVec, box);
        if (entityDist >= 0 && entityDist < 64.0D) {
            if (!isLineOfSightClear(player, eyePos, eyePos.add(lookVec.scale(entityDist)))) return false;
            return true;
        }
        double threshold = AbnormalitiesConfig.NUR_CURSOR_TRIGGER_DISTANCE.get();
        Vec3 entityCenter = box.getCenter();
        if (!isLineOfSightClear(player, eyePos, entityCenter)) return false;
        Vec3[] corners = {
                new Vec3(box.minX, box.minY, box.minZ), new Vec3(box.maxX, box.minY, box.minZ),
                new Vec3(box.minX, box.maxY, box.minZ), new Vec3(box.maxX, box.maxY, box.minZ),
                new Vec3(box.minX, box.minY, box.maxZ), new Vec3(box.maxX, box.minY, box.maxZ),
                new Vec3(box.minX, box.maxY, box.maxZ), new Vec3(box.maxX, box.maxY, box.maxZ)
        };
        for (Vec3 corner : corners) {
            Vec3 toCorner = corner.subtract(eyePos);
            double t = toCorner.dot(lookVec) / lookVec.dot(lookVec);
            if (t < 0) continue;
            Vec3 projection = eyePos.add(lookVec.scale(t));
            if (projection.distanceTo(corner) < threshold) return true;
        }
        return false;
    }

    private static boolean isLineOfSightClear(Player player, Vec3 from, Vec3 to) {
        BlockHitResult hit = player.level().clip(new ClipContext(
                from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS;
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getSource().getEntity() instanceof NurEntity)) return;
        event.setAmount(999.0F);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (!(event.getSource().getEntity() instanceof NurEntity)) return;
            if (!player.level().isClientSide && AbnormalitiesConfig.KICK_ON_DEATH.get()) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.GENERIC_EXPLODE, SoundSource.MASTER, 3.0F, 0.5F);
                ((ServerPlayer) player).connection.disconnect(Component.literal("Unknown error"));
            }
            return;
        }
        if (event.getEntity().level().isClientSide) return;
        if (!event.getEntity().getPersistentData().getBoolean("abnormalities:skinwalker")) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (event.getEntity().level().random.nextInt(100) >= AbnormalitiesConfig.SW_KILL_SPAWN_CHANCE.get()) return;
        scheduleSkinwalkerSpawn(40, event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(),
                (ServerLevel) event.getEntity().level(), player.getUUID());
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide) return;
        Entity target = event.getTarget();
        if (!target.getPersistentData().getBoolean("abnormalities:skinwalker")) return;
        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(event.getHand());
        if (target instanceof Animal animal && animal.isFood(held)) {
            target.getPersistentData().remove("abnormalities:skinwalker");
            ReputationManager.addRep(player, 15);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
            return;
        }
        if (target instanceof AbstractVillager && held.is(Items.BREAD)) {
            target.getPersistentData().remove("abnormalities:skinwalker");
            ReputationManager.addRep(player, 15);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
            if (!player.getAbilities().instabuild) held.shrink(1);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onTradeWithVillager(TradeWithVillagerEvent event) {
        AbstractVillager villager = event.getAbstractVillager();
        if (!villager.getPersistentData().getBoolean("abnormalities:skinwalker")) return;
        villager.getPersistentData().remove("abnormalities:skinwalker");
        ReputationManager.addRep(event.getEntity(), 15);
        event.getEntity().level().playSound(null, event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (mob.getPersistentData().getBoolean("abnormalities:skinwalker")) {
            boolean hasGoal = mob.goalSelector.getAvailableGoals().stream()
                    .anyMatch(g -> g.getGoal() instanceof NurSkinwalkerApproachGoal);
            if (!hasGoal) {
                mob.goalSelector.addGoal(1, new NurSkinwalkerApproachGoal(mob));
            }
        }
        if (mob instanceof AgeableMob baby && baby.isBaby()) {
            var adults = mob.level().getEntitiesOfClass(mob.getClass(), mob.getBoundingBox().inflate(8.0D),
                    e -> e != mob && !e.isBaby() && e.getPersistentData().getBoolean("abnormalities:skinwalker"));
            for (var adult : adults) {
                adult.getPersistentData().remove("abnormalities:skinwalker");
            }
        }
    }

    public static boolean hasEndAccess(net.minecraft.server.level.ServerPlayer player) {
        var endRoot = player.getServer().getAdvancements().getAdvancement(new ResourceLocation("minecraft", "end/root"));
        return endRoot != null && player.getAdvancements().getOrStartProgress(endRoot).isDone();
    }

    private static final java.util.Set<String> EXCLUDED_DISGUISES = java.util.Set.of(
        "armor_stand", "shulker", "ender_dragon", "wither", "giant", "illusioner"
    );
    private static java.util.List<net.minecraft.world.entity.EntityType<?>> cachedDisguiseTypes = null;

    private static net.minecraft.world.entity.EntityType<?> pickRandomDisguise(net.minecraft.util.RandomSource random) {
        if (cachedDisguiseTypes == null) {
            cachedDisguiseTypes = new java.util.ArrayList<>();
            for (java.util.Map.Entry<net.minecraft.resources.ResourceKey<net.minecraft.world.entity.EntityType<?>>, net.minecraft.world.entity.EntityType<?>> entry : net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getEntries()) {
                net.minecraft.world.entity.EntityType<?> type = entry.getValue();
                if (type.getCategory() == net.minecraft.world.entity.MobCategory.MISC) continue;
                net.minecraft.resources.ResourceLocation key = entry.getKey().location();
                if (key.getNamespace().equals(com.abnormalities.AbnormalitiesMod.MODID)) continue;
                String path = key.getPath();
                if (EXCLUDED_DISGUISES.contains(path)) continue;
                Class<? extends net.minecraft.world.entity.Entity> baseClass = type.getBaseClass();
                if (baseClass != null && net.minecraft.world.entity.Mob.class.isAssignableFrom(baseClass)) {
                    cachedDisguiseTypes.add(type);
                }
            }
        }
        if (cachedDisguiseTypes.isEmpty()) return null;
        return cachedDisguiseTypes.get(random.nextInt(cachedDisguiseTypes.size()));
    }

    private static void tickSkinwalkerChunks(ServerLevel level) {
        Set<String> active = new HashSet<>();
        Iterator<Map.Entry<UUID, int[]>> it = SW_CHUNKS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, int[]> entry = it.next();
            net.minecraft.world.entity.Entity e = level.getEntity(entry.getKey());
            if (e == null || !e.isAlive()) {
                int[] p = entry.getValue();
                SW_RELEASE_QUEUE.put(p[0] + "," + p[1], 100);
                it.remove();
                continue;
            }
            int cx = e.blockPosition().getX() >> 4;
            int cz = e.blockPosition().getZ() >> 4;
            String key = cx + "," + cz;
            active.add(key);
            if (cx != entry.getValue()[0] || cz != entry.getValue()[1]) {
                SW_RELEASE_QUEUE.put(entry.getValue()[0] + "," + entry.getValue()[1], 100);
                entry.getValue()[0] = cx;
                entry.getValue()[1] = cz;
            }
            level.setChunkForced(cx, cz, true);
        }
        SW_RELEASE_QUEUE.keySet().removeAll(active);
        Iterator<Map.Entry<String, Integer>> rit = SW_RELEASE_QUEUE.entrySet().iterator();
        while (rit.hasNext()) {
            Map.Entry<String, Integer> r = rit.next();
            int t = r.getValue() - 1;
            if (t <= 0) {
                String[] parts = r.getKey().split(",");
                level.setChunkForced(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), false);
                rit.remove();
            } else {
                r.setValue(t);
            }
        }
    }
}
