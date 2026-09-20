package com.lucas.atomicadditions.multiblock;

import com.lucas.atomicadditions.chemical.AtomicGases;
import com.lucas.atomicadditions.recipes.AtomicAMRRecipe;
import com.lucas.atomicadditions.recipes.AtomicRecipes;
import java.util.HashSet;
import java.util.Set;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.attribute.ChemicalAttributeValidator;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasTank;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.math.FloatingLong;
import mekanism.common.capabilities.chemical.multiblock.MultiblockChemicalTankBuilder;
import mekanism.common.capabilities.energy.VariableCapacityEnergyContainer;
import mekanism.common.inventory.container.sync.dynamic.ContainerSync;
import mekanism.common.lib.multiblock.MultiblockData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class AtomicMultiblockData
        extends MultiblockData {

    private static final long GAS_TANK_CAPACITY =
            8_000L;

    private static final long ENERGY_CAPACITY =
            80_000_000L;

    public int renderInput1Color = -1;
    public int renderInput2Color = -1;

    public String renderInput1Name = "";
    public String renderInput2Name = "";

    public double renderEnergy = 0;
    public double renderProcessed = 0;
    public double renderProgress = 0;
    public double renderProcessRate = 0;

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

    @ContainerSync
    public double lastProcessed = 0;

    @ContainerSync
    public FloatingLong lastReceivedEnergy =
            FloatingLong.ZERO;

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

        lastProcessed = 0;

        lastReceivedEnergy =
                FloatingLong.ZERO;

        if (!isFormed()) {
            processProgress = 0;

            updateRenderData();

            return true;
        }

        GasStack stack1 =
                inputTank1.getStack();

        GasStack stack2 =
                inputTank2.getStack();

        if (stack1.isEmpty()
                || stack2.isEmpty()) {

            processProgress = 0;

            updateRenderData();

            return true;
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

            updateRenderData();

            return true;
        }

        GasStack outputStack =
                outputTank.getStack();

        if (!outputStack.isEmpty()
                && outputStack.getType()
                != recipe.getOutput()) {

            processProgress = 0;

            updateRenderData();

            return true;
        }

        double totalEnergyPerRecipe =
                (double) recipe.getEnergyPerTick()
                        * recipe.getDuration();

        if (totalEnergyPerRecipe <= 0) {
            processProgress = 0;

            updateRenderData();

            return true;
        }

        double processableByEnergy =
                energyContainer.getEnergy().doubleValue()
                        / totalEnergyPerRecipe;

        double processableByInput1 =
                (double) stack1.getAmount()
                        / recipe.getInput1Amount();

        double processableByInput2 =
                (double) stack2.getAmount()
                        / recipe.getInput2Amount();

        double processableByOutput =
                (double) outputTank.getNeeded()
                        / recipe.getOutputAmount();

        double processable =
                Math.min(
                        processableByEnergy,
                        Math.min(
                                processableByInput1,
                                Math.min(
                                        processableByInput2,
                                        processableByOutput
                                )
                        )
                );

        if (processable <= 0) {
            updateRenderData();
            return true;
        }

        double energyToUseDouble =
                processable
                        * totalEnergyPerRecipe;

        FloatingLong energyToUse =
                FloatingLong.create(
                        Math.min(
                                energyToUseDouble,
                                energyContainer
                                        .getEnergy()
                                        .doubleValue()
                        )
                );

        if (energyToUse.isZero()) {
            updateRenderData();

            return true;
        }

        FloatingLong extractedEnergy =
                energyContainer.extract(
                        energyToUse,
                        Action.EXECUTE,
                        AutomationType.INTERNAL
                );

        if (extractedEnergy.isZero()) {
            updateRenderData();

            return true;
        }

        lastReceivedEnergy =
                extractedEnergy;

        double actualProcessable =
                extractedEnergy.doubleValue()
                        / totalEnergyPerRecipe;

        if (actualProcessable <= 0) {
            updateRenderData();

            return true;
        }

        processProgress +=
                actualProcessable;

        lastProcessed =
                actualProcessable;

        while (
                processProgress >= 1.0
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
                break;
            }

            if (currentInput1.getAmount()
                    < recipe.getInput1Amount()
                    || currentInput2.getAmount()
                    < recipe.getInput2Amount()) {

                break;
            }

            GasStack currentOutput =
                    outputTank.getStack();

            if (!currentOutput.isEmpty()
                    && currentOutput.getType()
                    != recipe.getOutput()) {

                break;
            }

            if (outputTank.getNeeded()
                    < recipe.getOutputAmount()) {

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
                    1.0;

            needsPacket = true;
        }

        if (processProgress > 0) {
            needsPacket = true;
        }

        updateRenderData();

        if (world.getGameTime() % 2 == 0) {
            needsPacket = true;
        }

        return needsPacket;
    }

    private void updateRenderData() {

        GasStack stack1 =
                inputTank1.getStack();

        GasStack stack2 =
                inputTank2.getStack();

        if (stack1.isEmpty()) {

            renderInput1Color = -1;
            renderInput1Name = "";

        } else {

            Gas gas =
                    stack1.getType();

            renderInput1Color =
                    gas.getColorRepresentation();

            renderInput1Name =
                    gas.getRegistryName()
                            .toString();
        }

        if (stack2.isEmpty()) {

            renderInput2Color = -1;
            renderInput2Name = "";

        } else {

            Gas gas =
                    stack2.getType();

            renderInput2Color =
                    gas.getColorRepresentation();

            renderInput2Name =
                    gas.getRegistryName()
                            .toString();
        }

        renderEnergy =
                lastReceivedEnergy.doubleValue();

        renderProcessed =
                lastProcessed;

        renderProcessRate =
                getProcessRate();

        renderProgress =
                getScaledProgress();
    }

    @Override
    public void writeUpdateTag(
            CompoundTag tag
    ) {
        super.writeUpdateTag(tag);

        tag.putInt(
                "amr_render_input1_color",
                renderInput1Color
        );

        tag.putInt(
                "amr_render_input2_color",
                renderInput2Color
        );

        tag.putString(
                "amr_render_input1_name",
                renderInput1Name
        );

        tag.putString(
                "amr_render_input2_name",
                renderInput2Name
        );

        tag.putDouble(
                "amr_render_energy",
                renderEnergy
        );

        tag.putDouble(
                "amr_render_processed",
                renderProcessed
        );

        tag.putDouble(
                "amr_render_process_rate",
                renderProcessRate
        );

        tag.putDouble(
                "amr_render_progress",
                renderProgress
        );

        ListTag coilList =
                new ListTag();

        for (BlockPos coil :
                coils) {

            coilList.add(
                    NbtUtils.writeBlockPos(
                            coil
                    )
            );
        }

        tag.put(
                "amr_coils",
                coilList
        );
    }

    @Override
    public void readUpdateTag(
            CompoundTag tag
    ) {
        super.readUpdateTag(tag);

        renderInput1Color =
                tag.getInt(
                        "amr_render_input1_color"
                );

        renderInput2Color =
                tag.getInt(
                        "amr_render_input2_color"
                );

        renderInput1Name =
                tag.getString(
                        "amr_render_input1_name"
                );

        renderInput2Name =
                tag.getString(
                        "amr_render_input2_name"
                );

        renderEnergy =
                tag.getDouble(
                        "amr_render_energy"
                );

        renderProcessed =
                tag.getDouble(
                        "amr_render_processed"
                );

        renderProcessRate =
                tag.getDouble(
                        "amr_render_process_rate"
                );

        renderProgress =
                tag.getDouble(
                        "amr_render_progress"
                );

        coils.clear();

        ListTag coilList =
                tag.getList(
                        "amr_coils",
                        Tag.TAG_COMPOUND
                );

        for (int i = 0;
             i < coilList.size();
             i++) {

            coils.add(
                    NbtUtils.readBlockPos(
                            coilList.getCompound(i)
                    )
            );
        }
    }

    public double getProcessRate() {

        if (lastProcessed <= 0) {
            return 0;
        }

        GasStack stack1 =
                inputTank1.getStack();

        GasStack stack2 =
                inputTank2.getStack();

        if (stack1.isEmpty()
                || stack2.isEmpty()) {
            return 0;
        }

        AtomicAMRRecipe recipe =
                AtomicRecipes.AMR_RECIPES.findRecipe(
                        stack1.getType(),
                        stack1.getAmount(),
                        stack2.getType(),
                        stack2.getAmount()
                );

        if (recipe == null) {
            return 0;
        }

        return Math.round(
                lastProcessed
                        * recipe.getOutputAmount()
                        * 1_000
        ) / 1_000D;
    }

    public double getScaledProgress() {
        return Math.min(
                1,
                processProgress
        );
    }
}