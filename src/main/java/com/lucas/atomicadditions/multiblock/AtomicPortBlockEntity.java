package com.lucas.atomicadditions.multiblock;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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

        MutableComponent modeComponent =
                Component.translatable(
                        next == PortMode.INPUT
                                ? "message.atomicadditions.port_input"
                                : "message.atomicadditions.port_output"
                );

        modeComponent.withStyle(
                next == PortMode.INPUT
                        ? net.minecraft.ChatFormatting.GREEN
                        : ChatFormatting.RED
        );

        MutableComponent message =
                Component.translatable(
                        "message.atomicadditions.port_changed"
                );

        message.append(modeComponent);

        player.displayClientMessage(
                message,
                true
        );

        return InteractionResult.SUCCESS;
    }
}