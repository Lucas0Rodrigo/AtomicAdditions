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

        delaySupplier = NO_DELAY;
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

    @Override
    @NotNull
    public IChemicalTankHolder<Gas, GasStack, IGasTank> getInitialGasTanks(
            IContentsListener listener
    ) {
        return side -> {
            if (getBlockState().getValue(AtomicPortBlock.MODE)
                    == PortMode.OUTPUT) {

                return Collections.singletonList(
                        getMultiblock().outputTank
                );
            }

            return List.of(
                    getMultiblock().inputTank1,
                    getMultiblock().inputTank2
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
         * Somente o Port em modo OUTPUT ejeta
         * o gás pelo tubo conectado.
         */
        if (isOutputMode()) {
            ChemicalUtil.emit(
                    outputDirections,
                    multiblock.outputTank,
                    this
            );
        }

        if (!energyContainer.isEmpty()) {

            var energy =
                    energyContainer.getEnergy();

            if (!energy.isZero()) {

                var extracted =
                        energyContainer.extract(
                                energy,
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

        return needsPacket;
    }

    private boolean isOutputMode() {
        return getBlockState().getValue(
                AtomicPortBlock.MODE
        ) == PortMode.OUTPUT;
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

            BlockState state =
                    getBlockState();

            PortMode oldMode =
                    state.getValue(
                            AtomicPortBlock.MODE
                    );

            PortMode newMode =
                    oldMode == PortMode.INPUT
                            ? PortMode.OUTPUT
                            : PortMode.INPUT;

            level.setBlockAndUpdate(
                    worldPosition,
                    state.setValue(
                            AtomicPortBlock.MODE,
                            newMode
                    )
            );

            invalidateCachedCapabilities();
        }

        return InteractionResult.SUCCESS;
    }
}