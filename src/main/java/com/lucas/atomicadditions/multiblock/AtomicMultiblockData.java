package com.lucas.atomicadditions.multiblock;

import com.lucas.atomicadditions.recipes.AtomicAMRRecipe;
import com.lucas.atomicadditions.recipes.AtomicRecipes;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasTank;
import mekanism.api.energy.IEnergyContainer;
import mekanism.common.capabilities.chemical.multiblock.MultiblockChemicalTankBuilder;
import mekanism.common.capabilities.energy.VariableCapacityEnergyContainer;
import mekanism.common.lib.multiblock.MultiblockData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashSet;
import java.util.Set;

public class AtomicMultiblockData extends MultiblockData {

    private static final long ENERGY_CAPACITY =
            1_000_000_000L;

    public final Set<BlockPos> coils =
            new HashSet<>();

    public final IGasTank inputTank1;
    public final IGasTank inputTank2;
    public final IGasTank outputTank;

    public final IEnergyContainer energyContainer;

    public double processProgress = 0;

    public AtomicMultiblockData(BlockEntity tile) {

        super(tile);

        inputTank1 =
                MultiblockChemicalTankBuilder.GAS.input(
                        this,
                        () -> 1_000_000L,
                        gas -> true,
                        mekanism.api.chemical.attribute.ChemicalAttributeValidator.ALWAYS_ALLOW,
                        createSaveAndComparator()
                );

        inputTank2 =
                MultiblockChemicalTankBuilder.GAS.input(
                        this,
                        () -> 1_000_000L,
                        gas -> true,
                        mekanism.api.chemical.attribute.ChemicalAttributeValidator.ALWAYS_ALLOW,
                        createSaveAndComparator()
                );

        outputTank =
                MultiblockChemicalTankBuilder.GAS.output(
                        this,
                        () -> 1_000_000L,
                        gas -> true,
                        mekanism.api.chemical.attribute.ChemicalAttributeValidator.ALWAYS_ALLOW,
                        this
                );

        gasTanks.add(inputTank1);
        gasTanks.add(inputTank2);
        gasTanks.add(outputTank);

        energyContainer =
                VariableCapacityEnergyContainer.create(
                        () -> isFormed()
                                ? ENERGY_CAPACITY
                                : 0,
                        automationType ->
                                automationType != AutomationType.EXTERNAL,
                        automationType ->
                                automationType != AutomationType.INTERNAL,
                        this
                );

        energyContainers.add(
                energyContainer
        );
    }

    public void addCoil(BlockPos pos) {
        coils.add(pos);
    }

    @Override
    public boolean tick(Level world) {

        boolean needsPacket =
                super.tick(world);

        if (!isFormed()) {
            return needsPacket;
        }

        GasStack stack1 =
                inputTank1.getStack();

        GasStack stack2 =
                inputTank2.getStack();

        if (stack1.isEmpty() || stack2.isEmpty()) {

            processProgress = 0;

            return needsPacket;
        }

        AtomicAMRRecipe recipe =
                AtomicRecipes.AMR_RECIPES.findRecipe(
                        stack1.getGas(),
                        stack1.getAmount(),
                        stack2.getGas(),
                        stack2.getAmount()
                );

        if (recipe == null) {

            processProgress = 0;

            return needsPacket;
        }

        GasStack outputStack =
                outputTank.getStack();

        if (!outputStack.isEmpty()
                && outputStack.getGas() != recipe.getOutput()) {

            processProgress = 0;

            return needsPacket;
        }

        long outputSpace =
                outputTank.getNeeded();

        if (outputSpace < recipe.getOutputAmount()) {

            processProgress = 0;

            return needsPacket;
        }

        long storedEnergy =
                energyContainer.getEnergy();

        if (storedEnergy <= 0) {
            return needsPacket;
        }

        long energyToUse =
                Math.min(
                        storedEnergy,
                        recipe.getEnergyPerTick()
                );

        long extracted =
                energyContainer.extract(
                        energyToUse,
                        Action.EXECUTE,
                        AutomationType.INTERNAL
                );

        if (extracted <= 0) {
            return needsPacket;
        }

        double progressThisTick =
                (double) extracted
                        / (double) recipe.getEnergyPerTick();

        processProgress +=
                progressThisTick;

        while (processProgress >= recipe.getDuration()) {

            processProgress -=
                    recipe.getDuration();

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

            needsPacket = true;

            GasStack remaining1 =
                    inputTank1.getStack();

            GasStack remaining2 =
                    inputTank2.getStack();

            AtomicAMRRecipe nextRecipe =
                    AtomicRecipes.AMR_RECIPES.findRecipe(
                            remaining1.isEmpty()
                                    ? null
                                    : remaining1.getGas(),
                            remaining1.isEmpty()
                                    ? 0
                                    : remaining1.getAmount(),
                            remaining2.isEmpty()
                                    ? null
                                    : remaining2.getGas(),
                            remaining2.isEmpty()
                                    ? 0
                                    : remaining2.getAmount()
                    );

            if (nextRecipe == null) {
                processProgress = 0;
                break;
            }

            if (outputTank.getNeeded()
                    < nextRecipe.getOutputAmount()) {

                processProgress = 0;
                break;
            }
        }

        if (processProgress > 0) {
            needsPacket = true;
        }

        return needsPacket;
    }
}