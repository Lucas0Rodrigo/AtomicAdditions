package com.lucas.atomicadditions;

import mekanism.common.block.interfaces.IHasTileEntity;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class AtomicCasingBlock extends BaseEntityBlock
        implements IHasTileEntity<AtomicCasingBlockEntity> {

    private static final TileEntityTypeRegistryObject<AtomicCasingBlockEntity> TILE_TYPE =
            new TileEntityTypeRegistryObject<>(AtomicAdditions.ATOMIC_CASING_BLOCK_ENTITY);

    public AtomicCasingBlock(Properties properties) {
        super(properties);
    }

    @Override
    public TileEntityTypeRegistryObject<AtomicCasingBlockEntity> getTileType() {
        return TILE_TYPE;
    }

    @Override
    public AtomicCasingBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AtomicCasingBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (type != AtomicAdditions.ATOMIC_CASING_BLOCK_ENTITY.get()) {
            return null;
        }

        if (level.isClientSide) {
            return (lvl, pos, blockState, blockEntity) ->
                    TileEntityMekanism.tickClient(
                            lvl,
                            pos,
                            blockState,
                            (TileEntityMekanism) blockEntity
                    );
        }

        return (lvl, pos, blockState, blockEntity) ->
                TileEntityMekanism.tickServer(
                        lvl,
                        pos,
                        blockState,
                        (TileEntityMekanism) blockEntity
                );
    }
}
