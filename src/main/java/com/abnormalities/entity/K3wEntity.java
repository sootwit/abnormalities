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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
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
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.RegistryObject;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class K3wEntity extends Mob {
    private static final Logger LOGGER = LoggerFactory.getLogger("Abnormalities|K3w");
    private static final int CHAT_DELAY = 600;
    private static final TicketType<K3wEntity> K3W_TICKET = TicketType.create("abnormalities_k3w", Comparator.comparingInt(System::identityHashCode), 0);
    private static final EntityDataAccessor<Optional<UUID>> DATA_TARGET_UUID = SynchedEntityData.defineId(K3wEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> DATA_CRASHING = SynchedEntityData.defineId(K3wEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_SKIN = SynchedEntityData.defineId(K3wEntity.class, EntityDataSerializers.STRING);

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
    private int crashTimer = -1;
    private int lastHurtTick = -100;
    private int lifetimeTicks = 0;
    private final List<UUID> possessedPlayers = new ArrayList<>();
    private UUID possessingPlayer = null;
    private int possessionPhaseTicks = 0;
    private boolean possessionActive = false;
    private int mimicSprintTicks = 0;
    private int mimicJumpTicks = 0;
    private BlockPos forcedChunk = null;
    private boolean needsChunkForce = false;

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
        this.entityData.define(DATA_TARGET_UUID, Optional.empty());
        this.entityData.define(DATA_CRASHING, false);
        this.entityData.define(DATA_SKIN, "");
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        if (this.isCrashing()) return false;
        this.lastHurtTick = this.tickCount;
        if (source.getEntity() instanceof Player p && p == targetPlayer) {
            p.hurt(p.damageSources().mobAttack(this), amount);
        }
        return super.hurt(source, amount);
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean isPushedByFluid() { return false; }

    @Override
    protected boolean canRide(net.minecraft.world.entity.Entity vehicle) { return false; }

    public UUID getTargetUUID() {
        Optional<UUID> opt = this.entityData.get(DATA_TARGET_UUID);
        return opt.orElse(targetPlayer != null ? targetPlayer.getUUID() : null);
    }

    public boolean isCrashing() {
        return this.entityData.get(DATA_CRASHING);
    }

    public String getSkinLocation() {
        return this.entityData.get(DATA_SKIN);
    }

    public void setSkinLocation(String skin) {
        this.entityData.set(DATA_SKIN, skin != null ? skin : "");
    }

    private String extractSkinFromProfile(Player player) {
        try {
            com.mojang.authlib.GameProfile profile = player.getGameProfile();
            var textures = profile.getProperties().get("textures");
            if (textures == null || textures.isEmpty()) return null;
            String encoded = textures.iterator().next().getValue();
            String json = new String(java.util.Base64.getDecoder().decode(encoded), java.nio.charset.StandardCharsets.UTF_8);
            com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            com.google.gson.JsonObject texturesObj = obj.getAsJsonObject("textures");
            if (texturesObj == null) return null;
            com.google.gson.JsonObject skinObj = texturesObj.getAsJsonObject("SKIN");
            if (skinObj == null) return null;
            return skinObj.get("url").getAsString();
        } catch (Exception e) {
            LOGGER.debug("[K3w] failed to extract skin from profile: {}", e.getMessage());
            return null;
        }
    }

    public Player getTargetPlayer() {
        return targetPlayer;
    }

    public void setTargetPlayer(Player player) {
        if (player != null && this.targetPlayer != null && player.getUUID().equals(this.targetPlayer.getUUID())) {
            return;
        }
        LOGGER.info("[K3w] target set to {} (was {})", player != null ? player.getName().getString() : "null", this.targetPlayer != null ? this.targetPlayer.getName().getString() : "null");
        this.targetPlayer = player;
        if (player != null) {
            UUID uuid = player.getUUID();
            this.entityData.set(DATA_TARGET_UUID, Optional.of(uuid));
            this.setCustomName(net.minecraft.network.chat.Component.literal(player.getName().getString()));
            this.setCustomNameVisible(true);
            String skinUrl = extractSkinFromProfile(player);
            if (skinUrl != null) {
                this.entityData.set(DATA_SKIN, skinUrl);
                LOGGER.info("[K3w] populated skin from profile for {}: {}", player.getName().getString(), skinUrl);
            } else {
                this.entityData.set(DATA_SKIN, "");
            }
        }
        this.messageSent = false;
        this.spawnTimer = 0;
        this.pendingActions.clear();
        this.undonePositions.clear();
        this.pathPoints.clear();
        this.currentPathIndex = 0;
        this.isMoving = false;
        this.crashTimer = -1;
        if (player != null) {
            this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(player.getMaxHealth());
            this.setHealth(player.getHealth());
        }
    }

    public void setInitialPath(List<double[]> path) {
        this.pathPoints.clear();
        this.pathPoints.addAll(path);
        if (!pathPoints.isEmpty()) {
            double[] first = pathPoints.get(0);
            this.moveTo(first[0], first[1], first[2], (float) first[3], (float) first[4]);
            isMoving = true;
        }
    }

    public void initTimers() {
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
                    state
            ));
        }
    }

    public void recordBlockPlace(Player player, BlockPos pos, BlockState state) {
        if (targetPlayer == null || player != targetPlayer) return;
        if (!level().isClientSide) {
            pendingActions.add(new K3wAction(
                    K3wAction.ActionType.PLACE,
                    pos.getX(), pos.getY(), pos.getZ(),
                    state
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
        if (this.isPassenger() && this.getVehicle() instanceof net.minecraft.world.entity.vehicle.Boat) {
            this.stopRiding();
        }
        if (this.tickCount % 10 == 0) {
            AABB box = this.getBoundingBox().inflate(3.0D);
            level().getEntitiesOfClass(net.minecraft.world.entity.vehicle.Boat.class, box, b -> true).forEach(net.minecraft.world.entity.Entity::discard);
        }

        if (targetPlayer != null && targetPlayer.isAlive() && !targetPlayer.isRemoved()) {
            BlockPos targetChunk = targetPlayer.blockPosition();
            boolean chunkChanged = forcedChunk == null || (forcedChunk.getX() >> 4) != (targetChunk.getX() >> 4) || (forcedChunk.getZ() >> 4) != (targetChunk.getZ() >> 4);
            if (chunkChanged || needsChunkForce) {
                if (forcedChunk != null && level() instanceof ServerLevel sl) {
                    sl.getChunkSource().removeRegionTicket(K3W_TICKET, new ChunkPos(forcedChunk), 2, this);
                }
                forcedChunk = targetChunk;
                if (level() instanceof ServerLevel sl) {
                    sl.getChunkSource().addRegionTicket(K3W_TICKET, new ChunkPos(targetChunk), 2, this);
                }
                needsChunkForce = false;
            }
        }

        if (possessionActive) {
            tickPossession();
            return;
        }

        lifetimeTicks++;

        BlockPos bp = this.blockPosition();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = bp.offset(dx, dy, dz);
                    BlockState s = level().getBlockState(p);
                    if (s.getBlock() instanceof DoorBlock door) {
                        LOGGER.debug("[K3w] opening door at {}", p);
                        door.setOpen(this, level(), s, p, true);
                    } else if (s.getBlock() instanceof TrapDoorBlock) {
                        if (!s.getValue(TrapDoorBlock.OPEN)) {
                            LOGGER.debug("[K3w] opening trapdoor at {}", p);
                            level().setBlock(p, s.setValue(TrapDoorBlock.OPEN, true), 2);
                        }
                    }
                }
            }
        }

        if (targetPlayer == null || targetPlayer.isRemoved() || !targetPlayer.isAlive()) {
            isMoving = false;
            pathPoints.clear();
            currentPathIndex = 0;
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
                this.pathPoints.clear();
                this.pathPoints.addAll(dummyPath);
                this.isMoving = true;
            } else {
                discard();
                return;
            }
        }

        if (targetPlayer != null && targetPlayer.isAlive() && !targetPlayer.isRemoved()) {
            if (this.tickCount - this.lastHurtTick > 40) {
                double[] hp = K3wActionTracker.getDelayedHealth(targetPlayer);
                if (hp != null) {
                    this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(hp[1]);
                    this.setHealth((float) hp[0]);
                }
            }
            if (targetPlayer.isSleeping()) waitingForDay = true;
            if (waitingForDay && targetPlayer.level().getDayTime() % 24000L < 2000) { discard(); return; }
            if (!targetPlayer.isSleeping() && waitingForDay && level().getDayTime() % 24000L >= 2000) waitingForDay = false;
        }

        if (hitCooldown > 0) hitCooldown--;

        if (crashTimer >= 0) {
            crashTimer++;
            if (targetPlayer instanceof ServerPlayer sp2) {
                if (crashTimer == 10) {
                    LOGGER.info("[K3w] crash sequence: nur sound playing");
                    level().playSound(null, targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ(),
                            ModSounds.NUR_SOUND.get(), SoundSource.MASTER, 10.0f, 1.0f);
                }
                if (crashTimer >= 16) {
                    LOGGER.info("[K3w] crash sequence complete: punishing {}", targetPlayer.getName().getString());
                    K3wEntity.this.discard();
                    if (AbnormalitiesConfig.K3W_PUNISH.get() == AbnormalitiesConfig.PunishMode.CRASH) {
                        com.abnormalities.AbnormalitiesMod.CHANNEL.send(
                            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp2),
                            new com.abnormalities.network.CrashPacket());
                    } else {
                        sp2.connection.disconnect(Component.literal("got you!"));
                    }
                }
            } else if (crashTimer >= 16) {
                discard();
            }
            return;
        }

        if (targetPlayer == null || !targetPlayer.isAlive()) return;

        double dist = this.distanceTo(targetPlayer);
        if (dist < 2.0D && hitCooldown <= 0 && !isCrashing() && targetPlayer.isAlive()) {
            LOGGER.info("[K3w] caught player {} at distance {}", targetPlayer.getName().getString(), String.format("%.1f", dist));
            targetPlayer.hurt(targetPlayer.damageSources().mobAttack(this), 10.0F);
            hitCooldown = 20;

            if (!level().isClientSide && targetPlayer instanceof ServerPlayer sp) {
                if (possessionActive) {
                    LOGGER.info("[K3w] possession catch on {} - all possessed, punishing everyone", sp.getName().getString());
                    punishAllPossessed(sp);
                    return;
                }
                int onlinePlayers = level().getServer() != null ? level().getServer().getPlayerList().getPlayers().size() : 1;
                if (onlinePlayers >= 2) {
                    LOGGER.info("[K3w] starting possession chain on {} ({} players online)", sp.getName().getString(), onlinePlayers);
                    startPossession(sp);
                    return;
                }
                RegistryObject<SoundEvent>[] crashSounds = new RegistryObject[]{
                        ModSounds.K3W_CRASH1, ModSounds.K3W_CRASH2, ModSounds.K3W_CRASH3, ModSounds.K3W_CRASH4
                };
                RegistryObject<SoundEvent> chosen = crashSounds[this.random.nextInt(crashSounds.length)];
                level().playSound(null, targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ(),
                        chosen.get(), SoundSource.MASTER, 10.0f, 1.0f);

                if (AbnormalitiesConfig.K3W_PUNISH.get() != AbnormalitiesConfig.PunishMode.NONE) {
                    this.entityData.set(DATA_CRASHING, true);
                    crashTimer = 0;
                }
            }
            return;
        }

        spawnTimer++;

        int followTicks = AbnormalitiesConfig.K3W_FOLLOW_TIME.get() * 20;

        if (spawnTimer < CHAT_DELAY + followTicks) return;

        double pathDist = targetPlayer != null ? this.distanceTo(targetPlayer) : 0;

        if (!isMoving || pathPoints.isEmpty()) {
            if (targetPlayer != null && targetPlayer.isAlive() && !targetPlayer.isRemoved()) {
                if (pathDist > 32) {
                    this.teleportTo(targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ());
                    this.setNoGravity(true);
                    this.noPhysics = true;
                } else if (spawnTimer > CHAT_DELAY + followTicks + 200) {
                    discard();
                }
            } else if (spawnTimer > CHAT_DELAY + followTicks + 200) {
                discard();
            }
            return;
        }

        double[] target;
        if (currentPathIndex >= pathPoints.size()) {
            target = K3wActionTracker.getDelayedPosition(targetPlayer);
            if (target == null) {
                if (targetPlayer != null && targetPlayer.isAlive() && !targetPlayer.isRemoved() && pathDist > 32) {
                    this.teleportTo(targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ());
                    this.setNoGravity(true);
                    this.noPhysics = true;
                }
                return;
            }
            LOGGER.debug("[K3w] path complete, following delayed position");
            this.setPos(target[0], target[1], target[2]);
            this.setNoGravity(true);
            this.noPhysics = true;
            return;
        }
        target = pathPoints.get(currentPathIndex);
        double off = Math.sqrt(
            (target[0] - this.getX()) * (target[0] - this.getX()) +
            (target[1] - this.getY()) * (target[1] - this.getY()) +
            (target[2] - this.getZ()) * (target[2] - this.getZ())
        );

        if (off > 10) {
            this.teleportTo(target[0], target[1], target[2]);
        } else {
            this.setPos(target[0], target[1], target[2]);
        }
        this.setNoGravity(true);
        this.noPhysics = true;
        this.setYRot((float) target[3]);
        this.setXRot((float) target[4]);
        this.yHeadRot = this.getYRot();
        this.yBodyRot = this.getYRot();
        if (currentPathIndex < pathPoints.size()) currentPathIndex++;

        BlockPos targetPos = this.blockPosition();
        Iterator<K3wAction> it = pendingActions.iterator();
        while (it.hasNext()) {
            K3wAction action = it.next();
            BlockPos actionPos = new BlockPos(action.x, action.y, action.z);
            if (targetPos.distSqr(actionPos) <= 36 && !undonePositions.contains(actionPos)) {
                executeUndo(action);
                undonePositions.add(actionPos);
                it.remove();
            }
        }
    }

    private void executeUndo(K3wAction action) {
        BlockPos pos = new BlockPos(action.x, action.y, action.z);
        LOGGER.info("[K3w] undoing {} at ({}, {}, {})", action.type, action.x, action.y, action.z);
        switch (action.type) {
            case BREAK -> {
                if (!AbnormalitiesConfig.K3W_BREAK_BLOCKS.get()) return;
                if (level().getBlockState(pos).isAir()) {
                    if (action.blockState != null) {
                        level().setBlockAndUpdate(pos, action.blockState);
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
            p.hurt(p.damageSources().mobAttack(this), 10.0F);
            return true;
        }
        return false;
    }

    @Override
    public void remove(net.minecraft.world.entity.Entity.RemovalReason reason) {
        LOGGER.info("[K3w] removed, reason={}", reason);
        if (possessionActive) {
            for (UUID uuid : possessedPlayers) {
                ServerPlayer p = level().getServer() != null ? level().getServer().getPlayerList().getPlayer(uuid) : null;
                if (p != null) {
                    p.setInvisible(false);
                    com.abnormalities.AbnormalitiesMod.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> p),
                        new com.abnormalities.network.K3wPossessPacket(-1, this.getId()));
                }
            }
            possessionActive = false;
        }
        if (forcedChunk != null && level() instanceof ServerLevel sl) {
            sl.getChunkSource().removeRegionTicket(K3W_TICKET, new ChunkPos(forcedChunk), 2, this);
            forcedChunk = null;
        }
        super.remove(reason);
        if (level() != null && !level().isClientSide && targetPlayer != null) {
            level().playSound(null, targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ(),
                    net.minecraft.sounds.SoundEvents.AMBIENT_CAVE.get(), SoundSource.MASTER, 3.0f, 1.8f);
        }
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source) {
        if (forcedChunk != null && level() instanceof ServerLevel sl) {
            sl.getChunkSource().removeRegionTicket(K3W_TICKET, new ChunkPos(forcedChunk), 2, this);
            forcedChunk = null;
        }
        if (targetPlayer != null && targetPlayer.isAlive() && !level().isClientSide) {
            targetPlayer.hurt(targetPlayer.damageSources().genericKill(), Float.MAX_VALUE);
        }
        super.die(source);
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
        tag.putInt("CrashTimer", crashTimer);
        tag.putInt("LifetimeTicks", lifetimeTicks);
        tag.putString("StoredSkin", getSkinLocation());
        if (forcedChunk != null) {
            tag.putInt("ForcedChunkX", forcedChunk.getX());
            tag.putInt("ForcedChunkZ", forcedChunk.getZ());
        }

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
            if (action.blockState != null) {
                aTag.putInt("BlockState", net.minecraft.world.level.block.Block.getId(action.blockState));
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
        crashTimer = tag.getInt("CrashTimer");
        lifetimeTicks = tag.getInt("LifetimeTicks");
        if (tag.contains("StoredSkin")) setSkinLocation(tag.getString("StoredSkin"));
        if (tag.contains("ForcedChunkX") && tag.contains("ForcedChunkZ")) {
            forcedChunk = new BlockPos(tag.getInt("ForcedChunkX"), 0, tag.getInt("ForcedChunkZ"));
            needsChunkForce = true;
        }

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
            K3wAction.ActionType type;
            try { type = K3wAction.ActionType.valueOf(aTag.getString("Type")); } catch (Exception ignored) { continue; }
            int ax = aTag.getInt("X"), ay = aTag.getInt("Y"), az = aTag.getInt("Z");
            if (aTag.contains("EntityType")) {
                EntityType<?> et = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(new net.minecraft.resources.ResourceLocation(aTag.getString("EntityType")));
                if (et != null) pendingActions.add(new K3wAction(type, (double)ax, (double)ay, (double)az, et));
            } else if (aTag.contains("BlockState")) {
                net.minecraft.world.level.block.state.BlockState bs = net.minecraft.world.level.block.Block.stateById(aTag.getInt("BlockState"));
                if (bs != null) pendingActions.add(new K3wAction(type, ax, ay, az, bs));
            }
        }
    }

    private void tickPossession() {
        possessionPhaseTicks++;
        if (possessingPlayer != null) {
            ServerPlayer possessed = level().getServer() != null ? level().getServer().getPlayerList().getPlayer(possessingPlayer) : null;
            if (possessed != null) {
                if (possessionPhaseTicks == 5) {
                    com.abnormalities.AbnormalitiesMod.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> possessed),
                        new com.abnormalities.network.K3wPossessPacket(0, this.getId()));
                }
                if (possessionPhaseTicks == 105) {
                    com.abnormalities.AbnormalitiesMod.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> possessed),
                        new com.abnormalities.network.K3wPossessPacket(1, this.getId()));
                }
                possessed.setInvisible(true);
                possessed.connection.teleport(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
            }
        }
        mimicSprintTicks++;
        mimicJumpTicks++;
        if (mimicSprintTicks > 40) {
            this.setSprinting(true);
            if (mimicSprintTicks > 60) {
                this.setSprinting(false);
                mimicSprintTicks = 0;
            }
        }
        if (mimicJumpTicks > 30 && this.onGround()) {
            this.jumpFromGround();
            mimicJumpTicks = 0;
        }
        if (targetPlayer == null || !targetPlayer.isAlive() || possessedPlayers.contains(targetPlayer.getUUID())) {
            ServerPlayer next = findNextPossessionTarget();
            if (next == null) {
                punishAllPossessed(null);
                return;
            }
            setTargetPlayer(next);
        }
        if (targetPlayer != null && targetPlayer.isAlive()) {
            this.getNavigation().moveTo(targetPlayer, 1.0D);
            this.getLookControl().setLookAt(targetPlayer, 30, 30);
            double d = this.distanceTo(targetPlayer);
            if (d < 2.0D && targetPlayer instanceof ServerPlayer nextVictim) {
                LOGGER.info("[K3w] possession caught next player {}", nextVictim.getName().getString());
                possessedPlayers.add(nextVictim.getUUID());
                possessingPlayer = nextVictim.getUUID();
                possessionPhaseTicks = 0;
                nextVictim.setInvisible(true);
                com.abnormalities.AbnormalitiesMod.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> nextVictim),
                    new com.abnormalities.network.K3wPossessPacket(0, this.getId()));
                ServerPlayer next = findNextPossessionTarget();
                if (next == null) {
                    punishAllPossessed(nextVictim);
                    return;
                }
                setTargetPlayer(next);
            }
        }
    }

    private ServerPlayer findNextPossessionTarget() {
        if (level().getServer() == null) return null;
        for (ServerPlayer p : level().getServer().getPlayerList().getPlayers()) {
            if (p.level().dimension() != this.level().dimension()) continue;
            if (possessedPlayers.contains(p.getUUID())) continue;
            if (p.getUUID().equals(possessingPlayer)) continue;
            return p;
        }
        return null;
    }

    private void punishAllPossessed(ServerPlayer finalVictim) {
        LOGGER.info("[K3w] possession complete, punishing {} possessed players", possessedPlayers.size());
        if (level().getServer() == null) return;
        for (UUID uuid : possessedPlayers) {
            ServerPlayer p = level().getServer().getPlayerList().getPlayer(uuid);
            if (p == null) continue;
            p.setInvisible(false);
            com.abnormalities.AbnormalitiesMod.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> p),
                new com.abnormalities.network.K3wPossessPacket(-1, this.getId()));
            if (AbnormalitiesConfig.K3W_PUNISH.get() == AbnormalitiesConfig.PunishMode.CRASH) {
                com.abnormalities.AbnormalitiesMod.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> p),
                    new com.abnormalities.network.CrashPacket());
            } else if (AbnormalitiesConfig.K3W_PUNISH.get() == AbnormalitiesConfig.PunishMode.KICK) {
                p.connection.disconnect(Component.literal("k3w got you."));
            }
        }
        this.discard();
    }

    public void startPossession(ServerPlayer victim) {
        if (possessionActive) return;
        possessionActive = true;
        possessingPlayer = victim.getUUID();
        possessedPlayers.add(victim.getUUID());
        possessionPhaseTicks = 0;
        victim.setInvisible(true);
        this.setInvisible(true);
        this.setNoAi(true);
        this.getNavigation().stop();
        LOGGER.info("[K3w] possession started on {}", victim.getName().getString());
    }

    public static class K3wAction {
        enum ActionType { BREAK, PLACE, KILL }

        final ActionType type;
        final int x, y, z;
        final net.minecraft.world.level.block.state.BlockState blockState;
        final net.minecraft.world.entity.EntityType<?> entityType;

        K3wAction(ActionType type, int x, int y, int z, net.minecraft.world.level.block.state.BlockState blockState) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.z = z;
            this.blockState = blockState;
            this.entityType = null;
        }

        K3wAction(ActionType type, double x, double y, double z, net.minecraft.world.entity.EntityType<?> entityType) {
            this.type = type;
            this.x = (int) Math.floor(x);
            this.y = (int) Math.floor(y);
            this.z = (int) Math.floor(z);
            this.blockState = null;
            this.entityType = entityType;
        }
    }
}
