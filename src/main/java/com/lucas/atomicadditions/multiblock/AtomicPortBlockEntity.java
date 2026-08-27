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
import org.jetbrains.annotations.NotNull;

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

    @NotNull
    @Override
    public IChemicalTankHolder<Gas, GasStack, IGasTank>
    getInitialGasTanks(
            IContentsListener listener
    ) {
        return side -> {
            AtomicMultiblockData multiblock =
                    getMultiblock();

            if (!multiblock.isFormed()) {
                return Collections.emptyList();
            }

            /*
             * INPUT PORT
             *
             * Expõe SOMENTE os dois tanques de entrada.
             */
            if (!getActive()) {
                return List.of(
                        multiblock.inputTank1,
                        multiblock.inputTank2
                );
            }

            /*
             * OUTPUT PORT
             *
             * Expõe SOMENTE o tanque de saída.
             */
            return List.of(
                    multiblock.outputTank
            );
        };
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
         * Energia recebida pelo Port.
         */
        if (!energyContainer.isEmpty()) {

            var available =
                    energyContainer.getEnergy();

            var needed =
                    multiblock.energyContainer.getNeeded();

            if (!needed.isZero()) {

                var transfer =
                        available.min(needed);

                if (!transfer.isZero()) {

                    var extracted =
                            energyContainer.extract(
                                    transfer,
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
         * OUTPUT
         *
         * Somente Port em modo OUTPUT.
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

    @Override
    public void setEjectSides(
            Set<Direction> sides
    ) {
        outputDirections = sides;
    }

    @Override
    public InteractionResult onSneakRightClick(
            Player player
    ) {
        if (!isRemote()) {

            boolean oldMode =
                    getActive();

            setActive(!oldMode);
        }

        return InteractionResult.SUCCESS;
    }
}