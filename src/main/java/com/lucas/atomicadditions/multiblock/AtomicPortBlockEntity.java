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
     * EXATAMENTE como o SPS do Mekanism 10.4.16:
     *
     * o Port apenas expõe os tanques do Multiblock.
     *
     * As permissões INPUT/OUTPUT são determinadas pelos
     * próprios tanques criados no AtomicMultiblockData.
     */

    @Override
    public IChemicalTankHolder<Gas, GasStack, IGasTank>
    getInitialGasTanks(
            IContentsListener listener
    ) {
        return side ->
                getMultiblock().getGasTanks(side);
    }

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
         */

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

        /*
         * -----------------------------------------------------
         * GAS DE SAÍDA
         * -----------------------------------------------------
         *
         * Igual ao SPS:
         *
         * somente Port em modo OUTPUT ejeta.
         */

        if (getActive()) {

            ChemicalUtil.emit(
                    outputDirections,
                    multiblock.outputTank,
                    this
            );
        }

        return needsPacket;
    }

    /*
     * =========================================================
     * EJECTOR
     * =========================================================
     */

    @Override
    public void setEjectSides(
            Set<Direction> sides
    ) {
        outputDirections = sides;
    }

    /*
     * =========================================================
     * INPUT / OUTPUT
     * =========================================================
     *
     * Igual ao SPS:
     *
     * false = INPUT
     * true  = OUTPUT
     */

    @Override
    public InteractionResult onSneakRightClick(
            Player player
    ) {
        if (level == null) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            setActive(!getActive());
        }

        return InteractionResult.SUCCESS;
    }
}