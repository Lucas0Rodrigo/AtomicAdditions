package com.lucas.atomicadditions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class AtomicCasingBlock extends BaseEntityBlock {

    public AtomicCasingBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AtomicCasingBlockEntity(pos, state);
    }

    @Override
    public void onPlace(BlockState state, net.minecraft.world.level.Level level,
                        BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);

        if (!level.isClientSide) {
            boolean formed = AtomicValidator.validate(level, pos);

            AtomicCasingBlockEntity blockEntity =
                    (AtomicCasingBlockEntity) level.getBlockEntity(pos);

            if (blockEntity != null) {
                blockEntity.setFormed(formed);

                if (formed) {
                    level.players().forEach(player ->
                            player.sendSystemMessage(
                                    net.minecraft.network.chat.Component.literal(
                                            "§aAtomic Additions: ESTRUTURA FORMADA!"
                                    )
                            )
                    );
                } else {
                    level.players().forEach(player ->
                            player.sendSystemMessage(
                                    net.minecraft.network.chat.Component.literal(
                                            "§cAtomic Additions: estrutura inválida."
                                    )
                            )
                    );
                }
            }
        }
    }

    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.MODEL;
    }
}