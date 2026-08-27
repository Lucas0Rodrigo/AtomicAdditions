package com.lucas.atomicadditions.multiblock;

import mekanism.api.math.FloatingLong;
import mekanism.common.block.attribute.AttributeEnergy;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class AtomicPortBlock
        extends AtomicCasingBlock<AtomicPortBlockEntity> {

    public static final EnumProperty<PortMode> MODE =
            EnumProperty.create("mode", PortMode.class);

    public AtomicPortBlock(
            Properties properties,
            TileEntityTypeRegistryObject<AtomicPortBlockEntity> tileType
    ) {
        super(properties, tileType);

        getType().add(
                new AttributeEnergy(
                        () -> FloatingLong.ZERO,
                        () -> FloatingLong.create(1_000_000_000L)
                )
        );

        registerDefaultState(
                stateDefinition.any()
                        .setValue(MODE, PortMode.INPUT)
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(MODE);
    }
}