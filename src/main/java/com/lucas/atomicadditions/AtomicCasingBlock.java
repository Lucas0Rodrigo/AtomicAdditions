package com.lucas.atomicadditions;

import mekanism.common.block.interfaces.IHasTileEntity;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class AtomicCasingBlock<BE extends AtomicCasingBlockEntity>
        extends BaseEntityBlock
        implements IHasTileEntity<BE> {

    private final TileEntityTypeRegistryObject<BE> tileType;

    public AtomicCasingBlock(
            Properties properties,
            TileEntityTypeRegistryObject<BE> tileType
    ) {
        super(properties);
        this.tileType = tileType;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public TileEntityTypeRegistryObject<BE> getTileType() {
        return tileType;
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (type != tileType.get()) {
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