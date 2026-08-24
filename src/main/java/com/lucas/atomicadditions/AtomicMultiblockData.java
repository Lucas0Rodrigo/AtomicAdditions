package com.lucas.atomicadditions;

import mekanism.common.lib.multiblock.MultiblockData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashSet;
import java.util.Set;

public class AtomicMultiblockData extends MultiblockData {

    public final Set<BlockPos> coils = new HashSet<>();

    public AtomicMultiblockData(BlockEntity tile) {
        super(tile);
    }

    public void addCoil(BlockPos pos, Direction side) {
        coils.add(pos.relative(side));
    }
}