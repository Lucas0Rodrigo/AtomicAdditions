package com.lucas.atomicadditions;

import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.Set;

public class AtomicMultiblockData {

    private boolean formed;
    private final Set<BlockPos> positions = new HashSet<>();

    public boolean isFormed() {
        return formed;
    }

    public void setFormed(boolean formed) {
        this.formed = formed;
    }

    public Set<BlockPos> getPositions() {
        return positions;
    }
}