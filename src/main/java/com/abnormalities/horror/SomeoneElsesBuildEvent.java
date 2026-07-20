package com.abnormalities.horror;

import com.abnormalities.WhisperManager;
import com.abnormalities.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

public class SomeoneElsesBuildEvent extends AbstractHorrorEvent {
    private static final Map<Block, Block> SUBSTITUTES = new HashMap<>();
    static {
        SUBSTITUTES.put(Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS);
        SUBSTITUTES.put(Blocks.SPRUCE_PLANKS, Blocks.BIRCH_PLANKS);
        SUBSTITUTES.put(Blocks.BIRCH_PLANKS, Blocks.JUNGLE_PLANKS);
        SUBSTITUTES.put(Blocks.STONE, Blocks.COBBLESTONE);
        SUBSTITUTES.put(Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE);
        SUBSTITUTES.put(Blocks.DIRT, Blocks.COARSE_DIRT);
        SUBSTITUTES.put(Blocks.GRASS_BLOCK, Blocks.PODZOL);
        SUBSTITUTES.put(Blocks.OAK_LOG, Blocks.DARK_OAK_LOG);
        SUBSTITUTES.put(Blocks.DARK_OAK_LOG, Blocks.OAK_LOG);
        SUBSTITUTES.put(Blocks.GLASS, Blocks.GLASS_PANE);
        SUBSTITUTES.put(Blocks.GLASS_PANE, Blocks.TINTED_GLASS);
        SUBSTITUTES.put(Blocks.WHITE_WOOL, Blocks.LIGHT_GRAY_WOOL);
        SUBSTITUTES.put(Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS);
        SUBSTITUTES.put(Blocks.COBBLESTONE_STAIRS, Blocks.STONE_STAIRS);
        SUBSTITUTES.put(Blocks.OAK_STAIRS, Blocks.SPRUCE_STAIRS);
    }

    private static final Map<UUID, List<BlockPlace>> RECENT_PLACES = new HashMap<>();
    private static final Map<UUID, BlockPos> MIRROR_CENTER = new HashMap<>();

    private static class BlockPlace {
        final int x, y, z;
        final Block block;
        BlockPlace(int x, int y, int z, Block b) { this.x = x; this.y = y; this.z = z; this.block = b; }
    }

    public SomeoneElsesBuildEvent() {
        super("someone_elses_build", 80, 0.7);
    }

    @Override
    public boolean allowsOngoing() { return true; }

    @Override
    public boolean canTrigger(ServerPlayer player, long currentTick) {
        List<BlockPlace> places = RECENT_PLACES.get(player.getUUID());
        return places != null && places.size() >= 5;
    }

    @Override
    public void execute(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        List<BlockPlace> places = RECENT_PLACES.getOrDefault(player.getUUID(), List.of());
        if (places.size() < 5) return;

        double angle = level.random.nextDouble() * Math.PI * 2;
        int ox = player.getBlockX() + (int)(Math.cos(angle) * 35);
        int oz = player.getBlockZ() + (int)(Math.sin(angle) * 35);
        int oy = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, ox, oz);

        int placed = 0;
        int cx = 0, cz = 0;
        for (BlockPlace p : places) {
            int dx = p.x - player.getBlockX();
            int dz = p.z - player.getBlockZ();
            int mx = ox - dx;
            int mz = oz - dz;
            cx += mx; cz += mz;
            BlockPos target = new BlockPos(mx, oy + (p.y - player.getBlockY()), mz);
            if (target.getY() < level.getMinBuildHeight() || target.getY() > level.getMaxBuildHeight()) continue;

            Block block = p.block;
            if (level.random.nextFloat() < 0.3f) block = SUBSTITUTES.getOrDefault(block, block);
            if (level.random.nextFloat() < 0.15f) block = Blocks.AIR;

            level.setBlock(target, block.defaultBlockState(), 2);
            placed++;
        }

        if (placed > 0) {
            cx /= placed; cz /= placed;
            MIRROR_CENTER.put(player.getUUID(), new BlockPos(cx, oy, cz));
            WhisperManager.sendWhisper(player, "someone else built this...");
            double sx = cx + 0.5;
            double sz = cz + 0.5;
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                SoundEvents.AMBIENT_CAVE, SoundSource.MASTER,
                sx, oy + 1, sz, 0.5f, 1.2f, 0));
        }
    }

    @Override
    public void onPlayerTick(ServerPlayer player) {
        BlockPos mirror = MIRROR_CENTER.get(player.getUUID());
        if (mirror == null) return;
        double dist = player.distanceToSqr(mirror.getX() + 0.5, mirror.getY() + 0.5, mirror.getZ() + 0.5);
        if (dist < 400) {
            WhisperManager.sendWhisper(player, "you found it...");
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                Holder.direct(ModSounds.HEARTBEAT_SOUND.get()), SoundSource.MASTER,
                player.getX(), player.getY() + 1, player.getZ(),
                0.8f, 0.6f, 0));
            MIRROR_CENTER.remove(player.getUUID());
            HorrorEventPool.clearOngoing(player);
        }
    }

    @Override
    public void onCleanup(ServerPlayer player) {
        RECENT_PLACES.remove(player.getUUID());
        MIRROR_CENTER.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        BlockPos pos = event.getPos();
        RECENT_PLACES.computeIfAbsent(sp.getUUID(), k -> new ArrayList<>())
            .add(new BlockPlace(pos.getX(), pos.getY(), pos.getZ(), event.getState().getBlock()));
        List<BlockPlace> list = RECENT_PLACES.get(sp.getUUID());
        if (list.size() > 30) list.remove(0);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        RECENT_PLACES.remove(event.getEntity().getUUID());
        MIRROR_CENTER.remove(event.getEntity().getUUID());
    }
}
