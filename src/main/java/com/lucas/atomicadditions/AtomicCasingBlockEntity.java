package com.lucas.atomicadditions;

import mekanism.common.lib.multiblock.MultiblockManager;
import mekanism.common.tile.prefab.TileEntityMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

public class AtomicCasingBlockEntity
        extends TileEntityMultiblock<AtomicMultiblockData> {

    public AtomicCasingBlockEntity(BlockPos pos, BlockState state) {
        super(AtomicAdditions.CASING.get(), pos, state);
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
    public InteractionResult onSneakRightClick(Player player) {

        if (!(getBlockState().getBlock() instanceof AtomicPortBlock)) {
            return InteractionResult.PASS;
        }

        if (!getMultiblock().isFormed()) {
            return InteractionResult.PASS;
        }

        PortMode current =
                getBlockState().getValue(AtomicPortBlock.MODE);

        PortMode next =
                current == PortMode.INPUT
                        ? PortMode.OUTPUT
                        : PortMode.INPUT;

        level.setBlock(
                worldPosition,
                getBlockState().setValue(
                        AtomicPortBlock.MODE,
                        next
                ),
                3
        );

        setChanged();

        player.displayClientMessage(
                Component.literal(
                        "Port: " +
                                next.getSerializedName().toUpperCase()
                ),
                true
        );

        return InteractionResult.SUCCESS;
    }
}