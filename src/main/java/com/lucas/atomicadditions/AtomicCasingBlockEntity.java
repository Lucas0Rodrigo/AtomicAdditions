package com.lucas.atomicadditions;

import mekanism.api.providers.IBlockProvider;
import mekanism.common.lib.multiblock.MultiblockManager;
import mekanism.common.tile.prefab.TileEntityMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
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
        if (player.isShiftKeyDown() ||
                !getMultiblock().isFormed()) {
            return InteractionResult.PASS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        NetworkHooks.openScreen(
                serverPlayer,
                new net.minecraft.world.MenuProvider() {

                    @Override
                    public Component getDisplayName() {
                        return Component.translatable(
                                "container.atomicadditions.atomic"
                        );
                    }

                    @Override
                    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                            int containerId,
                            Inventory inventory,
                            Player player
                    ) {
                        return new mekanism.common.inventory.container.tile.MekanismTileContainer<>(
                                AtomicContainerTypes.ATOMIC,
                                containerId,
                                inventory,
                                AtomicCasingBlockEntity.this
                        );
                    }
                },
                getBlockPos()
        );

        return InteractionResult.CONSUME;
    }
}