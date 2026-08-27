package com.lucas.atomicadditions.multiblock;

import java.util.Collections;
import java.util.Set;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasTank;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.lib.multiblock.IMultiblockEjector;
import mekanism.common.tile.base.SubstanceType;
import mekanism.common.util.ChemicalUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

public class AtomicPortBlockEntity
        extends AtomicCasingBlockEntity
        implements IMultiblockEjector {

    private MachineEnergyContainer<AtomicPortBlockEntity> energyContainer;

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

    /*
     * =========================================================
     * ENERGIA
     * =========================================================
     *
     * Exatamente a arquitetura usada pelo SPS 10.4.16.80:
     *
     * EnergyContainerHelper.forSide(this::getDirection)
     * +
     * MachineEnergyContainer.input(...)
     *
     * Isso permite que Universal Cable reconheça o Port.
     */
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(
            IContentsListener listener
    ) {
        EnergyContainerHelper builder =
                EnergyContainerHelper.forSide(
                        this::getDirection
                );

        builder.addContainer(
                energyContainer =
                        MachineEnergyContainer.input(
                                this,
                                listener
                        )
        );

        return builder.build();
    }

    /*
     * =========================================================
     * GAS
     * =========================================================
     *
     * O SPS faz exatamente isso:
     *
     * return side -> getMultiblock().getGasTanks(side);
     *
     * Não devemos escolher manualmente inputTank1,
     * inputTank2 ou outputTank aqui.
     *
     * As próprias propriedades dos tanques determinam
     * se eles aceitam inserção/extração externa.
     */
    @Override
    public IChemicalTankHolder<Gas, GasStack, IGasTank>
    getInitialGasTanks(
            IContentsListener listener
    ) {
        return side ->
                getMultiblock().getGasTanks(side);
    }

    /*
     * O gás pertence ao multiblock.
     * O Port somente expõe os tanques.
     */
    @Override
    public boolean persists(
            SubstanceType type
    ) {
        if (type == SubstanceType.GAS) {
            return false;
        }

        return super.persists(type);
    }

    /*
     * =========================================================
     * SERVER UPDATE
     * =========================================================
     */
    @Override
    protected boolean onUpdateServer(
            AtomicMultiblockData multiblock
    ) {
        boolean needsPacket =
                super.onUpdateServer(multiblock);

        if (!multiblock.isFormed()) {
            return needsPacket;
        }

        /*
         * -----------------------------------------------------
         * ENERGIA
         * -----------------------------------------------------
         *
         * A energia recebida pelo Port é transferida para
         * o armazenamento interno do AMR.
         */
        if (!energyContainer.isEmpty()) {

            if (!energyContainer.isEmpty()) {

                var available =
                        energyContainer.getEnergy();

                var needed =
                        multiblock.energyContainer.getNeeded();

                if (!needed.isZero()) {

                    var toTransfer =
                            available.min(needed);

                    if (!toTransfer.isZero()) {

                        var extracted =
                                energyContainer.extract(
                                        toTransfer,
                                        Action.EXECUTE,
                                        AutomationType.INTERNAL
                                );

                        if (!extracted.isZero()) {

                            multiblock.energyContainer.insert(
                                    extracted,
                                    Action.EXECUTE,
                                    AutomationType.INTERNAL
                            );

                            needsPacket = true;
                        }
                    }
                }
            }
        }

        /*
         * -----------------------------------------------------
         * GAS DE SAÍDA
         * -----------------------------------------------------
         *
         * Somente Port OUTPUT ejeta gás.
         */
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

        return needsPacket;
    }

    /*
     * O sistema de ejector do Mekanism informa quais lados
     * devem ser utilizados para a saída.
     */
    @Override
    public void setEjectSides(
            Set<Direction> sides
    ) {
        outputDirections = sides;
    }

    /*
     * =========================================================
     * ALTERAÇÃO INPUT / OUTPUT
     * =========================================================
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

        setChanged();

        return InteractionResult.SUCCESS;
    }
}