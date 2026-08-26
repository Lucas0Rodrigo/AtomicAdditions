package com.lucas.atomicadditions;

import mekanism.api.providers.IBlockProvider;
import mekanism.common.lib.multiblock.MultiblockManager;
import mekanism.common.tile.prefab.TileEntityMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkHooks;

public class AtomicCasingBlockEntity
        extends TileEntityMultiblock<AtomicMultiblockData> {

    public AtomicCasingBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        this(
                new AtomicBlockProvider(state.getBlock()),
                pos,
                state
        );
    }

    protected AtomicCasingBlockEntity(
            IBlockProvider provider,
            BlockPos pos,
            BlockState state
    ) {
        super(provider, pos, state);
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
    protected void structureChanged(
            AtomicMultiblockData multiblock
    ) {
        super.structureChanged(multiblock);
    }

    @Override
    public InteractionResult onActivate(
            Player player,
            InteractionHand hand,
            ItemStack stack
    ) {
        if (player.isShiftKeyDown() || !getMultiblock().isFormed()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(
                    serverPlayer,
                    AtomicContainerTypes.ATOMIC.getProvider(
                            getDisplayName(),
                            this
                    )
            );
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}