package com.lucas.atomicadditions;

import mekanism.api.providers.IBlockProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

public class AtomicPortBlockEntity extends AtomicCasingBlockEntity {

    public AtomicPortBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                new AtomicBlockProvider(state.getBlock()),
                pos,
                state
        );
    }

    @Override
    public InteractionResult onSneakRightClick(Player player) {
        if (level == null) {
            return InteractionResult.PASS;
        }

        PortMode current =
                getBlockState().getValue(
                        AtomicPortBlock.MODE
                );

        PortMode next =
                current == PortMode.INPUT
                        ? PortMode.OUTPUT
                        : PortMode.INPUT;

        level.setBlock(
                worldPosition,
                getBlockState().setValue(
                        AtomicPortBlock.MODE,
                        next
                ),
                3
        );

        setChanged();

        player.displayClientMessage(
                net.minecraft.network.chat.Component.literal(
                        "Port: "
                                + next.getSerializedName().toUpperCase()
                ),
                true
        );

        return InteractionResult.SUCCESS;
    }
}