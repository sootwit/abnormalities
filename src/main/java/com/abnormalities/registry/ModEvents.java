package com.abnormalities.registry;

import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.entity.NurEntity;
import com.abnormalities.entity.XyzEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
        final double sx, sz;
        final int sy;
        final ServerLevel level;
        final java.util.UUID playerUUID;
        SpawnTask(int delay, double sx, int sy, double sz, ServerLevel level, java.util.UUID playerUUID) {
            this.ticksRemaining = delay;
            this.sx = sx;
            this.sy = sy;
            this.sz = sz;
            this.level = level;
            this.playerUUID = playerUUID;
        }
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
        double sx = player.getX() + Math.cos(angle) * dist;
        double sz = player.getZ() + Math.sin(angle) * dist;
        int sy = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) sx, (int) sz);
        PENDING_SPAWNS.add(new SpawnTask(100, sx, sy, sz, level, player.getUUID()));
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
                NurEntity nur = ModEntities.NUR.get().create(task.level);
                if (nur == null) continue;
                nur.moveTo(task.sx + 0.5, task.sy, task.sz + 0.5, 0, 0);
                nur.currentState = NurEntity.State.STALKING;
                task.level.addFreshEntity(nur);
                Player target = task.level.getServer().getPlayerList().getPlayer(task.playerUUID);
                if (target != null) {
                    task.level.playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.AMBIENT_CAVE.get(), SoundSource.MASTER, 6.0f, 0.3f);
                }
            }
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
                BlockPos spawnPos = new BlockPos((int) sx, sy, (int) sz);
                if (!overworld.getBlockState(spawnPos.below()).canOcclude()) continue;

                XyzEntity xyz = ModEntities.XYZ.get().create(overworld);
                if (xyz != null) {
                    xyz.moveTo(sx + 0.5, sy, sz + 0.5, 0, 0);
                    xyz.setTargetPlayer(player);
                    overworld.addFreshEntity(xyz);

                    var tag = net.minecraft.tags.ItemTags.create(new ResourceLocation("abnormalities", "xyz_items"));
                    var items = new java.util.ArrayList<net.minecraft.world.item.Item>();
                    for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                        items.add(holder.value());
                    }
                    if (items.isEmpty()) continue;

                    net.minecraft.world.item.Item chosenItem = items.get(overworld.random.nextInt(items.size()));
                    int amount = 1;
                    if (overworld.random.nextBoolean()) {
                        amount = 2 + overworld.random.nextInt(15);
                    }
                    int seconds = 60 + overworld.random.nextInt(181);
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

        if (time < 13000L || time > 23000L) return;
        for (Player player : overworld.players()) {
            if (player.tickCount % 20 != 0) continue;
            if (overworld.random.nextInt(AbnormalitiesConfig.NUR_SPAWN_WEIGHT.get()) != 0) continue;
            double angle = overworld.random.nextDouble() * Math.PI * 2;
            double dist = 35.0D + overworld.random.nextDouble() * 30.0D;
            double sx = player.getX() + Math.cos(angle) * dist;
            double sz = player.getZ() + Math.sin(angle) * dist;
            int sy = overworld.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) sx, (int) sz);
            BlockPos spawnPos = new BlockPos((int) sx, sy, (int) sz);
            if (!overworld.getBlockState(spawnPos.below()).canOcclude()) continue;
            String text = PRE_SPAWN_TEXTS[overworld.random.nextInt(PRE_SPAWN_TEXTS.length)];
            if (player instanceof ServerPlayer sp) {
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSystemChatPacket(
                        Component.literal(text).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false));
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMBIENT_CAVE.get(), SoundSource.MASTER, 6.0f, 0.3f);
            PENDING_SPAWNS.add(new SpawnTask(100, sx, sy, sz, overworld, player.getUUID()));
        }
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
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getSource().getEntity() instanceof NurEntity nur)) return;
        nur.discard();
        if (!player.level().isClientSide && AbnormalitiesConfig.CRASH_ON_DEATH.get()) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.MASTER, 3.0F, 0.5F);
            ServerLevel sl = (ServerLevel) nur.level();
            sl.getServer().tell(new net.minecraft.server.TickTask(
                sl.getServer().getTickCount() + 25,
                () -> {
                    nur.discard();
                    System.exit(1);
                }
            ));
        }
    }
}
