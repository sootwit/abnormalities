package com.abnormalities.registry;

import com.abnormalities.AbnormalitiesMod;
import com.abnormalities.hexnil.HexNilPillarBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, AbnormalitiesMod.MODID);

    public static final RegistryObject<Block> HN_PILLAR_BLOCK = BLOCKS.register("0x0000_pillar_block",
            () -> new HexNilPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0f, 3600000.0f)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> 2)
                    .noOcclusion()));
}
