package com.lucas.atomicadditions;

import mekanism.api.providers.IBlockProvider;
import mekanism.common.lib.multiblock.MultiblockManager;
import mekanism.common.tile.prefab.TileEntityMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class AtomicCasingBlockEntity
        extends TileEntityMultiblock<AtomicMultiblockData> {

    public AtomicCasingBlockEntity(BlockPos pos, BlockState state) {
        this(
                new AtomicBlockProvider(state.getBlock()),
                pos,
                state
        );
    }

    protected AtomicCasingBlockEntity(
            IBlockProvider provider,
            BlockPos pos,
            BlockState state
    ) {
        super(provider, pos, state);
    }

    @Override
    public AtomicMultiblockData createMultiblock() {
        return new AtomicMultiblockData(this);
    }

    @Override
    public MultiblockManager<AtomicMultiblockData> getManager() {
        return AtomicAdditions.ATOMIC_MANAGER;
    }

    @Override
    protected void structureChanged(
            AtomicMultiblockData multiblock
    ) {
        super.structureChanged(multiblock);
    }
}