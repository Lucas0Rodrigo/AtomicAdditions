package com.lucas.atomicadditions.multiblock;

import com.lucas.atomicadditions.chemical.AtomicGases;
import com.lucas.atomicadditions.recipes.AtomicAMRRecipe;
import com.lucas.atomicadditions.recipes.AtomicRecipes;
import java.util.HashSet;
import java.util.Set;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.attribute.ChemicalAttributeValidator;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasTank;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.math.FloatingLong;
import mekanism.common.capabilities.chemical.multiblock.MultiblockChemicalTankBuilder;
import mekanism.common.capabilities.energy.VariableCapacityEnergyContainer;
import mekanism.common.inventory.container.sync.dynamic.ContainerSync;
import mekanism.common.lib.multiblock.MultiblockData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class AtomicMultiblockData
        extends MultiblockData {

    private static final long GAS_TANK_CAPACITY =
            1_000_000L;

    private static final long ENERGY_CAPACITY =
            1_000_000_000L;

    public final Set<BlockPos> coils =
            new HashSet<>();

    @ContainerSync
    public final IGasTank inputTank1;

    @ContainerSync
    public final IGasTank inputTank2;

    @ContainerSync
    public final IGasTank outputTank;

    public final IEnergyContainer energyContainer;

    @ContainerSync
    public double processProgress = 0;

    public AtomicMultiblockData(
            BlockEntity tile
    ) {
        super(tile);

        inputTank1 =
                MultiblockChemicalTankBuilder.GAS.input(
                        this,
                        () -> GAS_TANK_CAPACITY,
                        gas ->
                                gas == AtomicGases.NIOBIUM.get()
                                        || gas == AtomicGases.PALLADIUM.get(),
                        ChemicalAttributeValidator.ALWAYS_ALLOW,
                        createSaveAndComparator()
                );

        inputTank2 =
                MultiblockChemicalTankBuilder.GAS.input(
                        this,
                        () -> GAS_TANK_CAPACITY,
                        gas ->
                                gas == AtomicGases.GERMANIUM.get()
                                        || gas == AtomicGases.COPPER.get(),
                        ChemicalAttributeValidator.ALWAYS_ALLOW,
                        createSaveAndComparator()
                );

        outputTank =
                MultiblockChemicalTankBuilder.GAS.output(
                        this,
                        () -> GAS_TANK_CAPACITY,
                        gas ->
                                gas == AtomicGases.TANTALUM.get()
                                        || gas == AtomicGases.RHENIUM.get(),
                        ChemicalAttributeValidator.ALWAYS_ALLOW,
                        createSaveAndComparator()
                );

        gasTanks.add(inputTank1);
        gasTanks.add(inputTank2);
        gasTanks.add(outputTank);

        energyContainer =
                VariableCapacityEnergyContainer.create(
                        () -> FloatingLong.create(
                                ENERGY_CAPACITY
                        ),
                        automationType ->
                                automationType
                                        != AutomationType.EXTERNAL,
                        automationType ->
                                automationType
                                        != AutomationType.INTERNAL,
                        createSaveAndComparator()
                );

        energyContainers.add(
                energyContainer
        );
    }

    public void addCoil(
            BlockPos pos
    ) {
        coils.add(pos);
    }

    @Override
    public boolean tick(
            Level world
    ) {
        boolean needsPacket =
                super.tick(world);

        if (!isFormed()) {
            processProgress = 0;
            return needsPacket;
        }

        GasStack stack1 =
                inputTank1.getStack();

        GasStack stack2 =
                inputTank2.getStack();

        if (stack1.isEmpty()
                || stack2.isEmpty()) {

            processProgress = 0;
            return needsPacket;
        }

        AtomicAMRRecipe recipe =
                AtomicRecipes.AMR_RECIPES.findRecipe(
                        stack1.getType(),
                        stack1.getAmount(),
                        stack2.getType(),
                        stack2.getAmount()
                );

        if (recipe == null) {
            processProgress = 0;
            return needsPacket;
        }

        if (stack1.getAmount()
                < recipe.getInput1Amount()
                || stack2.getAmount()
                < recipe.getInput2Amount()) {

            processProgress = 0;
            return needsPacket;
        }

        GasStack outputStack =
                outputTank.getStack();

        if (!outputStack.isEmpty()
                && outputStack.getType()
                != recipe.getOutput()) {

            processProgress = 0;
            return needsPacket;
        }

        if (outputTank.getNeeded()
                < recipe.getOutputAmount()) {

            processProgress = 0;
            return needsPacket;
        }

        FloatingLong storedEnergy =
                energyContainer.getEnergy();

        if (storedEnergy.isZero()) {
            return needsPacket;
        }

        FloatingLong maximumEnergyPerTick =
                FloatingLong.create(
                        recipe.getEnergyPerTick()
                );

        FloatingLong energyToUse =
                storedEnergy.min(
                        maximumEnergyPerTick
                );

        if (energyToUse.isZero()) {
            return needsPacket;
        }

        FloatingLong extracted =
                energyContainer.extract(
                        energyToUse,
                        Action.EXECUTE,
                        AutomationType.INTERNAL
                );

        if (extracted.isZero()) {
            return needsPacket;
        }

        processProgress +=
                extracted.doubleValue()
                        / recipe.getEnergyPerTick();

        while (
                processProgress
                        >= recipe.getDuration()
        ) {

            GasStack currentInput1 =
                    inputTank1.getStack();

            GasStack currentInput2 =
                    inputTank2.getStack();

            if (!recipe.matches(
                    currentInput1.isEmpty()
                            ? null
                            : currentInput1.getType(),
                    currentInput1.getAmount(),
                    currentInput2.isEmpty()
                            ? null
                            : currentInput2.getType(),
                    currentInput2.getAmount()
            )) {

                processProgress = 0;
                break;
            }

            if (currentInput1.getAmount()
                    < recipe.getInput1Amount()
                    || currentInput2.getAmount()
                    < recipe.getInput2Amount()) {

                processProgress = 0;
                break;
            }

            GasStack currentOutput =
                    outputTank.getStack();

            if (!currentOutput.isEmpty()
                    && currentOutput.getType()
                    != recipe.getOutput()) {

                processProgress = 0;
                break;
            }

            if (outputTank.getNeeded()
                    < recipe.getOutputAmount()) {

                processProgress = 0;
                break;
            }

            inputTank1.extract(
                    recipe.getInput1Amount(),
                    Action.EXECUTE,
                    AutomationType.INTERNAL
            );

            inputTank2.extract(
                    recipe.getInput2Amount(),
                    Action.EXECUTE,
                    AutomationType.INTERNAL
            );

            outputTank.insert(
                    new GasStack(
                            recipe.getOutput(),
                            recipe.getOutputAmount()
                    ),
                    Action.EXECUTE,
                    AutomationType.INTERNAL
            );

            processProgress -=
                    recipe.getDuration();

            needsPacket = true;
        }

        if (processProgress > 0) {
            needsPacket = true;
        }

        return needsPacket;
    }
}