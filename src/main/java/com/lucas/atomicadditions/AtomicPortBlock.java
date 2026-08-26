package com.lucas.atomicadditions;

import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class AtomicPortBlock extends AtomicCasingBlock {

    public static final EnumProperty<PortMode> MODE =
            EnumProperty.create("mode", PortMode.class);

    private static final TileEntityTypeRegistryObject<AtomicPortBlockEntity> TILE_TYPE =
            new TileEntityTypeRegistryObject<>(
                    AtomicAdditions.ATOMIC_PORT_BLOCK_ENTITY
            );

    public AtomicPortBlock(Properties properties) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(MODE, PortMode.INPUT)
        );
    }

    @Override
    public TileEntityTypeRegistryObject<AtomicPortBlockEntity> getTileType() {
        return TILE_TYPE;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(MODE);
    }
}