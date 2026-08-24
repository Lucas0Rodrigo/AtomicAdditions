package com.lucas.atomicadditions;

import mekanism.common.block.interfaces.IHasTileEntity;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class AtomicCasingBlock extends BaseEntityBlock
        implements IHasTileEntity<AtomicCasingBlockEntity> {

    public AtomicCasingBlock(Properties properties) {
        super(properties);
    }

    @Override
    public TileEntityTypeRegistryObject<AtomicCasingBlockEntity> getTileType() {
        return new TileEntityTypeRegistryObject<>(
                AtomicAdditions.ATOMIC_CASING_BLOCK_ENTITY
        );
    }

    @Override
    public AtomicCasingBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AtomicCasingBlockEntity(pos, state);
    }
}