package com.lucas.atomicadditions.multiblock;

import mekanism.api.providers.IBlockProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class AtomicBlockProvider implements IBlockProvider {

    private final Block block;

    public AtomicBlockProvider(Block block) {
        this.block = block;
    }

    @Override
    public Block getBlock() {
        return block;
    }

    @Override
    public Item asItem() {
        return block.asItem();
    }
}