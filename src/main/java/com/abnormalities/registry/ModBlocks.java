package com.abnormalities.registry;

import com.abnormalities.AbnormalitiesMod;
import com.abnormalities.thewind.CorruptionBlock;
import com.abnormalities.thewind.DestructiveCorruptionBlock;
import com.abnormalities.thewind.WindPillarBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, AbnormalitiesMod.MODID);

    public static final RegistryObject<Block> CORRUPTION_BLOCK = BLOCKS.register("corruption_block",
            () -> new CorruptionBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(2.0f, 5.0f)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> 3)
                    .noOcclusion()));

    public static final RegistryObject<Block> DESTRUCTIVE_CORRUPTION_BLOCK = BLOCKS.register("destructive_corruption_block",
            () -> new DestructiveCorruptionBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(2.0f, 10.0f)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> 1)
                    .noOcclusion()));

    public static final RegistryObject<Block> WIND_PILLAR_BLOCK = BLOCKS.register("wind_pillar_block",
            () -> new WindPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0f, 3600000.0f)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> 2)
                    .noOcclusion()));
}
