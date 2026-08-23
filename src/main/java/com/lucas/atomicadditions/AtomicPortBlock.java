package com.lucas.atomicadditions;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.core.BlockPos;
import net.minecraftforge.common.util.FakePlayer;
import mekanism.api.IConfigCardAccess;
import mekanism.common.item.ItemConfigurator;

public class AtomicPortBlock extends Block {

    public static final EnumProperty<PortMode> MODE =
            EnumProperty.create("mode", PortMode.class);

    public AtomicPortBlock(Properties properties) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(MODE, PortMode.INPUT)
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {

        builder.add(MODE);
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hit) {

        ItemStack stack = player.getItemInHand(hand);

        if (stack.getItem() instanceof ItemConfigurator) {

            if (!level.isClientSide && !(player instanceof FakePlayer)) {

                PortMode current = state.getValue(MODE);

                PortMode next =
                        current == PortMode.INPUT
                                ? PortMode.OUTPUT
                                : PortMode.INPUT;

                level.setBlock(
                        pos,
                        state.setValue(MODE, next),
                        3
                );
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.use(state, level, pos, player, hand, hit);
    }
}