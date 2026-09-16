package com.abnormalities.sign;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.StructureManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class SignChunkGenerator extends NoiseBasedChunkGenerator {
    public static final Codec<SignChunkGenerator> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource),
            NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(g -> g.settings)
        ).apply(instance, instance.stable(SignChunkGenerator::new))
    );

    private final Holder<NoiseGeneratorSettings> settings;

    public SignChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings) {
        super(biomeSource, settings);
        this.settings = settings;
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState random, StructureManager structureManager, ChunkAccess chunk) {
        return super.fillFromNoise(executor, blender, random, structureManager, chunk).thenApply(c -> {
            corruptChunk(c);
            return c;
        });
    }

    private void corruptChunk(ChunkAccess chunk) {
        ChunkPos pos = chunk.getPos();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight();
        int floorY = 64;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = pos.getMinBlockX() + x;
                int wz = pos.getMinBlockZ() + z;
                long hash = ((long) wx * 73856093L) ^ ((long) wz * 19349663L);
                long absHash = Math.abs(hash);
                if (absHash % 37 == 0) {
                    for (int y = minY; y < maxY; y++) {
                        chunk.setBlockState(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), false);
                    }
                    continue;
                }
                double wx2 = (double) wx;
                double wz2 = (double) wz;
                double farlands = Math.sin(wx2 * 0.02) * Math.cos(wz2 * 0.02) * 12.0
                        + Math.sin(wx2 * 0.01 + wz2 * 0.015) * 18.0
                        + Math.cos(wx2 * 0.008 + wz2 * 0.01) * 10.0;
                int surfaceY = (int) (floorY + farlands);
                surfaceY = Math.max(floorY - 8, Math.min(floorY + 20, surfaceY));
                for (int y = minY; y < surfaceY - 4; y++) {
                    chunk.setBlockState(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState(), false);
                }
                for (int y = Math.max(minY, surfaceY - 4); y < surfaceY; y++) {
                    chunk.setBlockState(new BlockPos(x, y, z), Blocks.DIRT.defaultBlockState(), false);
                }
                if (surfaceY >= minY && surfaceY < maxY) {
                    chunk.setBlockState(new BlockPos(x, surfaceY, z), Blocks.GRASS_BLOCK.defaultBlockState(), false);
                }
                for (int y = surfaceY + 1; y < maxY; y++) {
                    chunk.setBlockState(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), false);
                }
            }
        }
    }
}
