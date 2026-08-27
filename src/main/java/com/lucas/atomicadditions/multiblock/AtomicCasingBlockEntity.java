package com.lucas.atomicadditions.multiblock;

import com.lucas.atomicadditions.AtomicAdditions;
import mekanism.api.chemical.gas.IGasTank;
import mekanism.api.providers.IBlockProvider;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.chemical.SyncableGasStack;
import mekanism.common.lib.multiblock.MultiblockManager;
import mekanism.common.tile.prefab.TileEntityMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class AtomicCasingBlockEntity
        extends TileEntityMultiblock<AtomicMultiblockData> {

    public AtomicCasingBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
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

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);

        if (!(this instanceof AtomicPortBlockEntity)) {
            boolean isClient = isRemote();

            for (IGasTank gasTank : getMultiblock().getGasTanks(null)) {
                container.track(
                        SyncableGasStack.create(
                                gasTank,
                                isClient
                        )
                );
            }
        }
    }
}