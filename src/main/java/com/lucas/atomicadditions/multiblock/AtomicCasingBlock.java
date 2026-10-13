package com.lucas.atomicadditions.multiblock;

import com.lucas.atomicadditions.AtomicAdditions;
import mekanism.common.block.attribute.AttributeGui;
import mekanism.common.block.attribute.AttributeSound;
import mekanism.common.block.interfaces.IHasTileEntity;
import mekanism.common.block.interfaces.ITypeBlock;
import mekanism.common.content.blocktype.BlockType;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.base.WrenchResult;
import mekanism.common.tile.prefab.TileEntityMultiblock;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AtomicCasingBlock<BE extends AtomicCasingBlockEntity>
        extends Block
        implements IHasTileEntity<BE>, ITypeBlock {

    private final TileEntityTypeRegistryObject<BE> tileType;
    private final BlockType type;

    public AtomicCasingBlock(
            Properties properties,
            TileEntityTypeRegistryObject<BE> tileType
    ) {
        super(properties);
        this.tileType = tileType;

        this.type = new BlockType(null);

        this.type.add(
                new AttributeGui(
                        () -> AtomicContainerTypes.ATOMIC,
                        null
                )
        );

        this.type.add(
                new AttributeSound(
                        AtomicAdditions.AMR_SOUND
                )
        );
    }

    @Override
    public BlockType getType() {
        return type;
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

    @Override
    protected boolean triggerEvent(
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            int id,
            int param
    ) {
        super.triggerEvent(
                state,
                level,
                pos,
                id,
                param
        );

        return triggerBlockEntityEvent(
                state,
                level,
                pos,
                id,
                param
        );
    }

    @Override
    @NotNull
    protected ItemInteractionResult useItemOn(
            @NotNull ItemStack stack,
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull Player player,
            @NotNull InteractionHand hand,
            @NotNull BlockHitResult hit
    ) {
        TileEntityMultiblock<?> tile =
                WorldUtils.getTileEntity(
                        TileEntityMultiblock.class,
                        level,
                        pos
                );

        if (tile == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (level.isClientSide) {
            if (!MekanismUtils.canUseAsWrench(
                    stack
            )) {
                if (!tile.hasGui()
                        || !tile.getMultiblock().isFormed()) {
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                }
            }

            return ItemInteractionResult.SUCCESS;
        }

        WrenchResult wrenchResult =
                tile.tryWrench(
                        state,
                        player,
                        stack
                );

        if (wrenchResult != WrenchResult.PASS) {
            return wrenchResult.getInteractionResult();
        }

        return tile.onActivate(
                player,
                hand,
                stack
        );
    }
}
