package com.abnormalities.registry;

import com.abnormalities.AbnormalitiesMod;
import com.abnormalities.thewind.CorruptionBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AbnormalitiesMod.MODID);

    public static final RegistryObject<BlockEntityType<CorruptionBlockEntity>> CORRUPTION = BLOCK_ENTITIES.register("corruption_block_entity",
            () -> BlockEntityType.Builder.of(CorruptionBlockEntity::new,
                    ModBlocks.CORRUPTION_BLOCK.get(),
                    ModBlocks.DESTRUCTIVE_CORRUPTION_BLOCK.get()).build(null));
}
