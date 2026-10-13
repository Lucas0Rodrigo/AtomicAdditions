package com.lucas.atomicadditions.multiblock;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.chemical.IChemicalTankHolder;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.lib.multiblock.IValveHandler.ValveData;
import mekanism.common.lib.multiblock.MultiblockData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import org.jetbrains.annotations.NotNull;

public class AtomicPortBlockEntity
        extends AtomicCasingBlockEntity {

    private MachineEnergyContainer<AtomicPortBlockEntity> energyContainer;

    private final Map<Direction, BlockCapabilityCache<IChemicalHandler, Direction>>
            chemicalCapabilityCaches =
            new EnumMap<>(Direction.class);

    public AtomicPortBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                state.getBlockHolder(),
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

        if (getBlockState().getValue(
                AtomicPortBlock.MODE
        ) == PortMode.INPUT) {

            builder.addContainer(
                    energyContainer =
                            MachineEnergyContainer.input(
                                    this,
                                    listener
                            )
            );

        } else {
            energyContainer = null;
        }

        return builder.build();
    }

    @Override
    @NotNull
    public IChemicalTankHolder getInitialChemicalTanks(
            IContentsListener listener
    ) {
        return side -> {
            AtomicMultiblockData mb =
                    getMultiblock();

            if (getBlockState().getValue(
                    AtomicPortBlock.MODE
            ) == PortMode.OUTPUT) {

                return List.of(
                        mb.outputTank
                );
            }

            return List.of(
                    mb.inputTank1,
                    mb.inputTank2
            );
        };
    }

    @Override
    public boolean persists(
            ContainerType<?, ?, ?> type
    ) {
        if (type == ContainerType.CHEMICAL) {
            return false;
        }

        return super.persists(type);
    }

    public void addChemicalTargetCapability(
            List<MultiblockData.CapabilityOutputTarget<IChemicalHandler>> outputTargets,
            Direction side
    ) {
        BlockCapabilityCache<IChemicalHandler, Direction> cache =
                chemicalCapabilityCaches.get(
                        side
                );

        if (cache == null) {
            cache =
                    Capabilities.CHEMICAL.createCache(
                            (ServerLevel) level,
                            worldPosition.relative(side),
                            side.getOpposite()
                    );

            chemicalCapabilityCaches.put(
                    side,
                    cache
            );
        }

        outputTargets.add(
                new MultiblockData.CapabilityOutputTarget<>(
                        cache,
                        this::isOutputMode
                )
        );
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

        if (isInputMode()
                && energyContainer != null
                && hasAttachedCoil()
                && !energyContainer.isEmpty()) {

            long energy =
                    energyContainer.getEnergy();

            if (energy > 0L) {

                long extracted =
                        energyContainer.extract(
                                energy,
                                Action.EXECUTE,
                                AutomationType.INTERNAL
                        );

                if (extracted > 0L) {

                    multiblock.energyContainer.insert(
                            extracted,
                            Action.EXECUTE,
                            AutomationType.EXTERNAL
                    );

                    needsPacket = true;
                }
            }
        }

        return needsPacket;
    }

    private boolean hasAttachedCoil() {
        for (ValveData valve :
                getMultiblock().valves) {

            if (!valve.location.equals(
                    worldPosition
            )) {
                continue;
            }

            BlockPos coilPos =
                    valve.location.relative(
                            valve.side.getOpposite()
                    );

            return getMultiblock()
                    .coils
                    .contains(coilPos);
        }

        return false;
    }

    private boolean isInputMode() {
        return getBlockState().getValue(
                AtomicPortBlock.MODE
        ) == PortMode.INPUT;
    }

    private boolean isOutputMode() {
        return getBlockState().getValue(
                AtomicPortBlock.MODE
        ) == PortMode.OUTPUT;
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

            invalidateCapabilitiesFull();

            boolean input =
                    newMode == PortMode.INPUT;

            Component message =
                    Component.translatable(
                            "message.atomicadditions.port_changed"
                    ).append(
                            Component.translatable(
                                    input
                                            ? "message.atomicadditions.port_input"
                                            : "message.atomicadditions.port_output"
                            ).withStyle(
                                    input
                                            ? ChatFormatting.GREEN
                                            : ChatFormatting.RED
                            )
                    );

            player.displayClientMessage(
                    message,
                    true
            );
        }

        return InteractionResult.SUCCESS;
    }
}
