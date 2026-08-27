package com.lucas.atomicadditions.multiblock;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasTank;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.lib.multiblock.IMultiblockEjector;
import mekanism.common.tile.base.SubstanceType;
import mekanism.common.util.ChemicalUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

public class AtomicPortBlockEntity
        extends AtomicCasingBlockEntity
        implements IMultiblockEjector {

    private Set<Direction> outputDirections =
            Collections.emptySet();

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

    /**
     * Expõe os tanques do AMR para o sistema químico
     * do Mekanism.
     *
     * INPUT:
     *   inputTank1 + inputTank2
     *
     * OUTPUT:
     *   outputTank
     *
     * A assinatura e o IChemicalTankHolder são exatamente
     * os utilizados pelo TileEntitySPSPort do Mekanism
     * 10.4.16.80.
     */
    @Override
    public IChemicalTankHolder<Gas, GasStack, IGasTank> getInitialGasTanks(
            IContentsListener listener
    ) {
        return side -> {

            if (!getMultiblock().isFormed()) {
                return Collections.emptyList();
            }

            PortMode mode =
                    getBlockState().getValue(
                            AtomicPortBlock.MODE
                    );

            if (mode == PortMode.INPUT) {
                return List.of(
                        getMultiblock().inputTank1,
                        getMultiblock().inputTank2
                );
            }

            return List.of(
                    getMultiblock().outputTank
            );
        };
    }

    /**
     * O gás pertence ao multiblock.
     *
     * O Port apenas expõe o capability.
     */
    @Override
    public boolean persists(SubstanceType type) {
        if (type == SubstanceType.GAS) {
            return false;
        }

        return super.persists(type);
    }

    /**
     * Ejeção ativa de gás.
     *
     * Exatamente o mesmo mecanismo utilizado pelo
     * TileEntitySPSPort do Mekanism 10.4.16.80.
     */
    @Override
    protected boolean onUpdateServer(
            AtomicMultiblockData multiblock
    ) {
        boolean needsPacket =
                super.onUpdateServer(multiblock);

        if (multiblock.isFormed()) {

            PortMode mode =
                    getBlockState().getValue(
                            AtomicPortBlock.MODE
                    );

            if (mode == PortMode.OUTPUT
                    && !multiblock.outputTank.isEmpty()) {

                ChemicalUtil.emit(
                        outputDirections,
                        multiblock.outputTank,
                        this
                );
            }
        }

        return needsPacket;
    }

    /**
     * Recebe os lados externos determinados pelo
     * sistema de multiblock do Mekanism.
     */
    @Override
    public void setEjectSides(
            Set<Direction> sides
    ) {
        outputDirections = sides;
    }

    /**
     * Alterna:
     *
     * INPUT -> OUTPUT
     * OUTPUT -> INPUT
     */
    @Override
    public InteractionResult onSneakRightClick(
            Player player
    ) {
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

        /*
         * O TileEntityMultiblock já invalida capabilities
         * quando a estrutura muda. Aqui o holder consulta
         * o BlockState atual quando o capability é usado,
         * então não precisamos criar um sistema paralelo.
         */
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
                        : net.minecraft.ChatFormatting.RED
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