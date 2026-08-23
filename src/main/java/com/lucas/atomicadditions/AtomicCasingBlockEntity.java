package com.lucas.atomicadditions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class AtomicCasingBlockEntity extends BlockEntity {

    private boolean formed;

    public AtomicCasingBlockEntity(BlockPos pos, BlockState state) {
        super(AtomicAdditions.ATOMIC_CASING_BLOCK_ENTITY.get(), pos, state);
    }

    public boolean isFormed() {
        return formed;
    }

    public void setFormed(boolean formed) {
        this.formed = formed;
        setChanged();
    }
}