package com.abnormalities.entity;

import com.abnormalities.config.AbnormalitiesConfig;
import com.abnormalities.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.RegistryObject;

import java.util.*;

public class K3wEntity extends Mob {
    private static final int CHAT_DELAY = 600;
    private static final EntityDataAccessor<Long> DATA_TARGET_UUID_MOST = SynchedEntityData.defineId(K3wEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DATA_TARGET_UUID_LEAST = SynchedEntityData.defineId(K3wEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> DATA_CRASHING = SynchedEntityData.defineId(K3wEntity.class, EntityDataSerializers.BOOLEAN);

    private final List<K3wAction> pendingActions = new ArrayList<>();
    private final Set<BlockPos> undonePositions = new HashSet<>();

    private Player targetPlayer;
    private int spawnTimer = 0;
    private boolean messageSent = false;
    private boolean waitingForDay = false;
    private final List<double[]> pathPoints = new ArrayList<>();
    private int currentPathIndex = 0;
    private boolean isMoving = false;
    private int hitCooldown = 0;

    public K3wEntity(EntityType<? extends K3wEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setPersistenceRequired();
        this.noCulling = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 9999.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 128.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_TARGET_UUID_MOST, 0L);
        this.entityData.define(DATA_TARGET_UUID_LEAST, 0L);
        this.entityData.define(DATA_CRASHING, false);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    public UUID getTargetUUID() {
        long most = this.entityData.get(DATA_TARGET_UUID_MOST);
        long least = this.entityData.get(DATA_TARGET_UUID_LEAST);
        if (most == 0L && least == 0L) {
            return targetPlayer != null ? targetPlayer.getUUID() : null;
        }
        return new UUID(most, least);
    }

    public boolean isCrashing() {
        return this.entityData.get(DATA_CRASHING);
    }

    public Player getTargetPlayer() {
        return targetPlayer;
    }

    public void setTargetPlayer(Player player) {
        if (player != null && player == this.targetPlayer) {
            return;
        }
        this.targetPlayer = player;
        if (player != null) {
            UUID uuid = player.getUUID();
            this.entityData.set(DATA_TARGET_UUID_MOST, uuid.getMostSignificantBits());
            this.entityData.set(DATA_TARGET_UUID_LEAST, uuid.getLeastSignificantBits());
            this.setCustomName(net.minecraft.network.chat.Component.literal(player.getName().getString()));
            this.setCustomNameVisible(true);
        }
        this.messageSent = false;
        this.spawnTimer = 0;
        this.pendingActions.clear();
        this.undonePositions.clear();
        this.pathPoints.clear();
        this.currentPathIndex = 0;
        this.isMoving = false;
    }

    public void setInitialPath(List<double[]> path) {
        this.pathPoints.clear();
        this.pathPoints.addAll(path);
        if (!pathPoints.isEmpty()) {
            double[] first = pathPoints.get(0);
            this.moveTo(first[0], first[1], first[2], (float) first[3], (float) first[4]);
            isMoving = true;
        }
        spawnTimer = CHAT_DELAY;
        messageSent = false;
    }

    public void setInitialActions(List<K3wAction> actions) {
        this.pendingActions.clear();
        this.pendingActions.addAll(actions);
    }

    public void recordBlockBreak(Player player, BlockPos pos, BlockState state) {
        if (targetPlayer == null || player != targetPlayer) return;
        if (!level().isClientSide) {
            pendingActions.add(new K3wAction(
                    K3wAction.ActionType.BREAK,
                    pos.getX(), pos.getY(), pos.getZ(),
                    state.getBlock()
            ));
        }
    }

    public void recordBlockPlace(Player player, BlockPos pos, BlockState state) {
        if (targetPlayer == null || player != targetPlayer) return;
        if (!level().isClientSide) {
            pendingActions.add(new K3wAction(
                    K3wAction.ActionType.PLACE,
                    pos.getX(), pos.getY(), pos.getZ(),
                    state.getBlock()
            ));
        }
    }

    public void recordMobKill(Player player, double x, double y, double z, EntityType<?> type) {
        if (targetPlayer == null || player != targetPlayer) return;
        if (!level().isClientSide) {
            pendingActions.add(new K3wAction(
                    K3wAction.ActionType.KILL,
                    x, y, z,
                    type
            ));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        if (targetPlayer == null || targetPlayer.isRemoved() || !targetPlayer.isAlive()) {
            var nearest = level().getNearestPlayer(this, 64.0D);
            if (nearest != null) {
                setTargetPlayer(nearest);
                int followTicks = AbnormalitiesConfig.K3W_FOLLOW_TIME.get() * 20;
                spawnTimer = CHAT_DELAY + followTicks;
                messageSent = true;
                List<double[]> dummyPath = new ArrayList<>();
                for (int i = 0; i < 20; i++) {
                    dummyPath.add(new double[]{nearest.getX(), nearest.getY(), nearest.getZ(), nearest.getYRot(), nearest.getXRot()});
                }
                setInitialPath(dummyPath);
            } else {
                discard();
                return;
            }
        }

        if (targetPlayer.isSleeping()) {
            waitingForDay = true;
        }
        if (waitingForDay && targetPlayer.level().getDayTime() % 24000L < 2000) {
            discard();
            return;
        }
        if (!targetPlayer.isSleeping() && waitingForDay && level().getDayTime() % 24000L >= 2000) {
            waitingForDay = false;
        }

        if (hitCooldown > 0) hitCooldown--;

        double dist = this.distanceTo(targetPlayer);
        if (dist < 2.0D && hitCooldown <= 0 && !isCrashing() && targetPlayer.isAlive()) {
            targetPlayer.hurt(targetPlayer.damageSources().mobAttack(this), 5.0F);
            hitCooldown = 20;

            if (!level().isClientSide && targetPlayer instanceof ServerPlayer sp) {
                RegistryObject<SoundEvent>[] crashSounds = new RegistryObject[]{
                        ModSounds.K3W_CRASH1, ModSounds.K3W_CRASH2, ModSounds.K3W_CRASH3, ModSounds.K3W_CRASH4
                };
                RegistryObject<SoundEvent> chosen = crashSounds[this.random.nextInt(crashSounds.length)];
                level().playSound(null, targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ(),
                        chosen.get(), SoundSource.MASTER, 10.0f, 1.0f);

                if (AbnormalitiesConfig.K3W_CRASH_ON_CATCH.get()) {
                    this.entityData.set(DATA_CRASHING, true);
                    SoundEvent nurScare = ModSounds.NUR_SOUND.get();
                    level().playSound(null, targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ(),
                            nurScare, SoundSource.MASTER, 10.0f, 1.0f);
                    net.minecraft.server.MinecraftServer srv = level().getServer();
                    srv.tell(new net.minecraft.server.TickTask(srv.getTickCount() + 30, () -> {
                        K3wEntity.this.discard();
                        sp.connection.disconnect(Component.literal("k3w got you."));
                    }));
                }
            }
            return;
        }

        spawnTimer++;

        int followTicks = AbnormalitiesConfig.K3W_FOLLOW_TIME.get() * 20;

        if (!messageSent && spawnTimer >= CHAT_DELAY) {
            if (targetPlayer instanceof ServerPlayer sp) {
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSystemChatPacket(
                        Component.literal("<k3w> you have " + AbnormalitiesConfig.K3W_FOLLOW_TIME.get() + " seconds to run").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), false));
            }
            targetPlayer.level().playSound(null, targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ(),
                    net.minecraft.sounds.SoundEvents.AMBIENT_CAVE.get(), SoundSource.MASTER, 4.0f, 0.5f);
            messageSent = true;
        }

        if (spawnTimer < CHAT_DELAY + followTicks) return;

        if (!isMoving || pathPoints.isEmpty()) {
            if (spawnTimer > CHAT_DELAY + followTicks + 200) {
                discard();
            }
            return;
        }

        double[] target = K3wActionTracker.getDelayedPosition(targetPlayer);
        if (target != null) {
            boolean shouldFly = target[1] - this.getY() > 2.0;
            this.setNoGravity(shouldFly);
            double dx = target[0] - this.getX();
            double dy = target[1] - this.getY();
            double dz = target[2] - this.getZ();
            double horizDist = Math.sqrt(dx * dx + dz * dz);
            if (horizDist > 0.5) {
                this.getMoveControl().setWantedPosition(target[0], target[1], target[2], 1.0D);
                this.getLookControl().setLookAt(target[0], target[1], target[2], 30, 30);
                if (shouldFly) {
                    double speed = 0.5D;
                    this.setDeltaMovement(dx / horizDist * speed, dy > 1 ? 0.3D : dy < -1 ? -0.3D : 0, dz / horizDist * speed);
                }
            }
            if (this.onGround() && horizDist > 1) {
                BlockPos inFront = this.blockPosition().relative(this.getDirection());
                if (level().getBlockState(inFront).canOcclude()) {
                    this.jumpFromGround();
                }
            }
            if (this.isInWater() && horizDist > 0.5) {
                this.setDeltaMovement(this.getDeltaMovement().x, 0.2D, this.getDeltaMovement().z);
            }
            BlockPos targetPos = new BlockPos((int) Math.floor(target[0]), (int) Math.floor(target[1]), (int) Math.floor(target[2]));
            Iterator<K3wAction> it = pendingActions.iterator();
            while (it.hasNext()) {
                K3wAction action = it.next();
                BlockPos actionPos = new BlockPos(action.x, action.y, action.z);
                if (Math.abs(actionPos.getX() - targetPos.getX()) <= 1 && Math.abs(actionPos.getY() - targetPos.getY()) <= 1 && Math.abs(actionPos.getZ() - targetPos.getZ()) <= 1 && !undonePositions.contains(actionPos)) {
                    executeUndo(action);
                    undonePositions.add(actionPos);
                    it.remove();
                }
            }
        }
    }

    private void executeUndo(K3wAction action) {
        BlockPos pos = new BlockPos(action.x, action.y, action.z);
        switch (action.type) {
            case BREAK -> {
                if (!AbnormalitiesConfig.K3W_BREAK_BLOCKS.get()) return;
                if (level().getBlockState(pos).isAir()) {
                    if (action.block != null) {
                        level().setBlockAndUpdate(pos, action.block.defaultBlockState());
                        level().playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                                net.minecraft.sounds.SoundEvents.STONE_PLACE, SoundSource.MASTER, 1.0f, 0.8f);
                    }
                }
            }
            case PLACE -> {
                if (!AbnormalitiesConfig.K3W_PLACE_BLOCKS.get()) return;
                if (!level().getBlockState(pos).isAir()) {
                    level().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    level().playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                            net.minecraft.sounds.SoundEvents.STONE_BREAK, SoundSource.MASTER, 1.0f, 0.8f);
                }
            }
            case KILL -> {
                if (!AbnormalitiesConfig.K3W_REVIVE_MOBS.get()) return;
                if (action.entityType != null) {
                    try {
                        var entity = action.entityType.create(level());
                        if (entity != null) {
                            entity.moveTo(action.x + 0.5, action.y, action.z + 0.5, 0, 0);
                            level().addFreshEntity(entity);
                            level().playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                                    net.minecraft.sounds.SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.MASTER, 1.0f, 1.2f);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        if (target instanceof Player p) {
            p.hurt(p.damageSources().mobAttack(this), 5.0F);
            return true;
        }
        return false;
    }

    @Override
    public void remove(net.minecraft.world.entity.Entity.RemovalReason reason) {
        super.remove(reason);
        if (level() != null && !level().isClientSide && targetPlayer != null) {
            level().playSound(null, targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ(),
                    net.minecraft.sounds.SoundEvents.AMBIENT_CAVE.get(), SoundSource.MASTER, 3.0f, 1.8f);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (targetPlayer != null) {
            tag.putUUID("TargetPlayer", targetPlayer.getUUID());
        }
        tag.putInt("SpawnTimer", spawnTimer);
        tag.putBoolean("MessageSent", messageSent);
        tag.putBoolean("IsMoving", isMoving);
        tag.putInt("CurrentPathIndex", currentPathIndex);

        net.minecraft.nbt.ListTag pathTag = new net.minecraft.nbt.ListTag();
        for (double[] pt : pathPoints) {
            CompoundTag ptTag = new CompoundTag();
            ptTag.putDouble("X", pt[0]);
            ptTag.putDouble("Y", pt[1]);
            ptTag.putDouble("Z", pt[2]);
            ptTag.putFloat("YRot", (float) pt[3]);
            ptTag.putFloat("XRot", (float) pt[4]);
            pathTag.add(ptTag);
        }
        tag.put("PathPoints", pathTag);

        net.minecraft.nbt.ListTag actionTag = new net.minecraft.nbt.ListTag();
        for (K3wAction action : pendingActions) {
            CompoundTag aTag = new CompoundTag();
            aTag.putString("Type", action.type.name());
            aTag.putInt("X", action.x);
            aTag.putInt("Y", action.y);
            aTag.putInt("Z", action.z);
            if (action.entityType != null) {
                var etKey = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(action.entityType);
                if (etKey != null) aTag.putString("EntityType", etKey.toString());
            }
            if (action.block != null) {
                var blKey = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(action.block);
                if (blKey != null) aTag.putString("Block", blKey.toString());
            }
            actionTag.add(aTag);
        }
        tag.put("PendingActions", actionTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("TargetPlayer")) {
            UUID uuid = tag.getUUID("TargetPlayer");
            if (level() != null && level().getServer() != null) {
                targetPlayer = level().getServer().getPlayerList().getPlayer(uuid);
            }
        }
        spawnTimer = tag.getInt("SpawnTimer");
        messageSent = tag.getBoolean("MessageSent");
        isMoving = tag.getBoolean("IsMoving");
        currentPathIndex = tag.getInt("CurrentPathIndex");

        pathPoints.clear();
        net.minecraft.nbt.ListTag pathTag = tag.getList("PathPoints", 10);
        for (int i = 0; i < pathTag.size(); i++) {
            CompoundTag ptTag = pathTag.getCompound(i);
            pathPoints.add(new double[]{ptTag.getDouble("X"), ptTag.getDouble("Y"), ptTag.getDouble("Z"), ptTag.getFloat("YRot"), ptTag.getFloat("XRot")});
        }

        pendingActions.clear();
        net.minecraft.nbt.ListTag actionTag = tag.getList("PendingActions", 10);
        for (int i = 0; i < actionTag.size(); i++) {
            CompoundTag aTag = actionTag.getCompound(i);
            K3wAction.ActionType type = K3wAction.ActionType.valueOf(aTag.getString("Type"));
            int ax = aTag.getInt("X"), ay = aTag.getInt("Y"), az = aTag.getInt("Z");
            if (aTag.contains("EntityType")) {
                EntityType<?> et = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(new net.minecraft.resources.ResourceLocation(aTag.getString("EntityType")));
                if (et != null) pendingActions.add(new K3wAction(type, (double)ax, (double)ay, (double)az, et));
            } else if (aTag.contains("Block")) {
                Block bl = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new net.minecraft.resources.ResourceLocation(aTag.getString("Block")));
                if (bl != null) pendingActions.add(new K3wAction(type, ax, ay, az, bl));
            } else {
                pendingActions.add(new K3wAction(type, ax, ay, az, (Block) null));
            }
        }
    }

    public static class K3wAction {
        enum ActionType { BREAK, PLACE, KILL }

        final ActionType type;
        final int x, y, z;
        final Block block;
        final EntityType<?> entityType;

        K3wAction(ActionType type, int x, int y, int z, Block block) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.z = z;
            this.block = block;
            this.entityType = null;
        }

        K3wAction(ActionType type, double x, double y, double z, EntityType<?> entityType) {
            this.type = type;
            this.x = (int) Math.floor(x);
            this.y = (int) Math.floor(y);
            this.z = (int) Math.floor(z);
            this.block = null;
            this.entityType = entityType;
        }
    }
}
