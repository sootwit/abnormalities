package com.abnormalities.thewind;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class CorruptionBlockEntity extends BlockEntity {
    private BlockState originalState;

    public CorruptionBlockEntity(BlockPos pos, BlockState state) {
        super(com.abnormalities.registry.ModBlockEntities.CORRUPTION.get(), pos, state);
    }

    public void setOriginalState(BlockState state) {
        this.originalState = state;
        setChanged();
    }

    public BlockState getOriginalState() {
        return originalState;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (originalState != null) {
            tag.putString("OriginalBlock", BuiltInRegistries.BLOCK.getKey(originalState.getBlock()).toString());
            CompoundTag props = new CompoundTag();
            for (Property<?> prop : originalState.getProperties()) {
                props.putString(prop.getName(), getNameForProperty(originalState, prop));
            }
            tag.put("OriginalProps", props);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Comparable<T>> String getNameForProperty(BlockState state, Property<T> prop) {
        return prop.getName(state.getValue(prop));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("OriginalBlock")) {
            ResourceLocation rl = new ResourceLocation(tag.getString("OriginalBlock"));
            Block block = BuiltInRegistries.BLOCK.get(rl);
            if (block != Blocks.AIR) {
                BlockState state = block.defaultBlockState();
                if (tag.contains("OriginalProps")) {
                    CompoundTag props = tag.getCompound("OriginalProps");
                    for (String key : props.getAllKeys()) {
                        state = applyProperty(state, key, props.getString(key));
                    }
                }
                originalState = state;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Comparable<T>> BlockState applyProperty(BlockState state, String key, String value) {
        for (Property<?> prop : state.getProperties()) {
            if (prop.getName().equals(key)) {
                Property<T> typed = (Property<T>) prop;
                for (T possible : typed.getPossibleValues()) {
                    if (typed.getName(possible).equals(value)) {
                        return state.setValue(typed, possible);
                    }
                }
                break;
            }
        }
        return state;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
