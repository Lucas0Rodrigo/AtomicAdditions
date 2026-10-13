package com.lucas.atomicadditions.multiblock;

import com.lucas.atomicadditions.chemical.AtomicGases;
import com.lucas.atomicadditions.recipes.AtomicAMRRecipe;
import com.lucas.atomicadditions.recipes.AtomicRecipes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.attribute.ChemicalAttributeValidator;
import mekanism.api.energy.IEnergyContainer;
import mekanism.common.capabilities.chemical.VariableCapacityChemicalTank;
import mekanism.common.capabilities.energy.VariableCapacityEnergyContainer;
import mekanism.common.inventory.container.sync.dynamic.ContainerSync;
import mekanism.common.lib.multiblock.IValveHandler.ValveData;
import mekanism.common.lib.multiblock.MultiblockData;
import mekanism.common.registration.impl.DeferredChemical;
import mekanism.common.util.ChemicalUtil;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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
    public final IChemicalTank inputTank1;

    @ContainerSync
    public final IChemicalTank inputTank2;

    @ContainerSync
    public final IChemicalTank outputTank;

    private final List<MultiblockData.CapabilityOutputTarget<IChemicalHandler>>
            chemicalOutputTargets =
            new ArrayList<>();

    public final IEnergyContainer energyContainer;

    @ContainerSync
    public double processProgress = 0;

    @ContainerSync
    public double lastProcessed = 0;

    @ContainerSync
    public long lastReceivedEnergy = 0L;

    public AtomicMultiblockData(
            BlockEntity tile
    ) {
        super(tile);

        inputTank1 =
                VariableCapacityChemicalTank.input(
                        this,
                        () -> GAS_TANK_CAPACITY,
                        stack ->
                                isChemical(
                                        stack,
                                        AtomicGases.NIOBIUM,
                                        AtomicGases.PALLADIUM
                                ),
                        ChemicalAttributeValidator.ALWAYS_ALLOW,
                        createSaveAndComparator()
                );

        inputTank2 =
                VariableCapacityChemicalTank.input(
                        this,
                        () -> GAS_TANK_CAPACITY,
                        stack ->
                                isChemical(
                                        stack,
                                        AtomicGases.GERMANIUM,
                                        AtomicGases.COPPER
                                ),
                        ChemicalAttributeValidator.ALWAYS_ALLOW,
                        createSaveAndComparator()
                );

        outputTank =
                VariableCapacityChemicalTank.output(
                        () -> GAS_TANK_CAPACITY,
                        stack ->
                                isChemical(
                                        stack,
                                        AtomicGases.TANTALUM,
                                        AtomicGases.RHENIUM
                                ),
                        createSaveAndComparator()
                );

        chemicalTanks.add(inputTank1);
        chemicalTanks.add(inputTank2);
        chemicalTanks.add(outputTank);

        energyContainer =
                VariableCapacityEnergyContainer.create(
                        () -> ENERGY_CAPACITY,
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

    private static boolean isChemical(
            ChemicalStack stack,
            DeferredChemical<Chemical> first,
            DeferredChemical<Chemical> second
    ) {
        Chemical chemical =
                stack.getChemical();

        return chemical == first.get()
                || chemical == second.get();
    }

    public void addCoil(
            BlockPos pos
    ) {
        coils.add(pos);
    }

    @Override
    protected void updateEjectors(
            Level world
    ) {
        chemicalOutputTargets.clear();

        for (ValveData valve :
                valves) {

            AtomicPortBlockEntity tile =
                    WorldUtils.getTileEntity(
                            AtomicPortBlockEntity.class,
                            world,
                            valve.location
                    );

            if (tile != null) {
                tile.addChemicalTargetCapability(
                        chemicalOutputTargets,
                        valve.side
                );
            }
        }
    }

    @Override
    public boolean tick(
            Level world
    ) {
        boolean needsPacket =
                super.tick(world);

        lastProcessed = 0;

        lastReceivedEnergy = 0L;

        if (!isFormed()) {
            processProgress = 0;

            updateRenderData();

            return true;
        }

        ChemicalStack stack1 =
                inputTank1.getStack();

        ChemicalStack stack2 =
                inputTank2.getStack();

        if (stack1.isEmpty()
                || stack2.isEmpty()) {

            processProgress = 0;

            updateRenderData();

            return true;
        }

        AtomicAMRRecipe recipe =
                AtomicRecipes.AMR_RECIPES.findRecipe(
                        stack1.getChemical(),
                        stack1.getAmount(),
                        stack2.getChemical(),
                        stack2.getAmount()
                );

        if (recipe == null) {
            processProgress = 0;

            updateRenderData();

            return true;
        }

        ChemicalStack outputStack =
                outputTank.getStack();

        if (!outputStack.isEmpty()
                && outputStack.getChemical()
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
                (double) energyContainer.getEnergy()
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

        long energyToUse =
                (long) Math.floor(
                        Math.min(
                                energyToUseDouble,
                                (double) energyContainer.getEnergy()
                        )
                );

        if (energyToUse <= 0L) {
            updateRenderData();

            return true;
        }

        long extractedEnergy =
                energyContainer.extract(
                        energyToUse,
                        Action.EXECUTE,
                        AutomationType.INTERNAL
                );

        if (extractedEnergy <= 0L) {
            updateRenderData();

            return true;
        }

        lastReceivedEnergy =
                extractedEnergy;

        double actualProcessable =
                (double) extractedEnergy
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

            ChemicalStack currentInput1 =
                    inputTank1.getStack();

            ChemicalStack currentInput2 =
                    inputTank2.getStack();

            if (!recipe.matches(
                    currentInput1.isEmpty()
                            ? null
                            : currentInput1.getChemical(),
                    currentInput1.getAmount(),
                    currentInput2.isEmpty()
                            ? null
                            : currentInput2.getChemical(),
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

            ChemicalStack currentOutput =
                    outputTank.getStack();

            if (!currentOutput.isEmpty()
                    && currentOutput.getChemical()
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
                    recipe.getOutput().getStack(
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

        ChemicalUtil.emit(
                getActiveOutputs(
                        chemicalOutputTargets
                ),
                outputTank
        );

        updateRenderData();

        if (world.getGameTime() % 2 == 0) {
            needsPacket = true;
        }

        return needsPacket;
    }

    private void updateRenderData() {

        ChemicalStack stack1 =
                inputTank1.getStack();

        ChemicalStack stack2 =
                inputTank2.getStack();

        if (stack1.isEmpty()) {

            renderInput1Color = -1;
            renderInput1Name = "";

        } else {

            Chemical gas =
                    stack1.getChemical();

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

            Chemical gas =
                    stack2.getChemical();

            renderInput2Color =
                    gas.getColorRepresentation();

            renderInput2Name =
                    gas.getRegistryName()
                            .toString();
        }

        renderEnergy =
                (double) lastReceivedEnergy;

        renderProcessed =
                lastProcessed;

        renderProcessRate =
                getProcessRate();

        renderProgress =
                getScaledProgress();
    }

    @Override
    public void writeUpdateTag(
            CompoundTag tag,
            HolderLookup.Provider provider
    ) {
        super.writeUpdateTag(tag, provider);

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

        long[] coilPositions =
                new long[coils.size()];

        int index = 0;

        for (BlockPos coil :
                coils) {

            coilPositions[index++] =
                    coil.asLong();
        }

        tag.putLongArray(
                "amr_coils",
                coilPositions
        );
    }

    @Override
    public void readUpdateTag(
            CompoundTag tag,
            HolderLookup.Provider provider
    ) {
        super.readUpdateTag(tag, provider);

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

        for (long packedPosition :
                tag.getLongArray(
                        "amr_coils"
                )) {

            coils.add(
                    BlockPos.of(
                            packedPosition
                    )
            );
        }
    }

    public double getProcessRate() {

        if (lastProcessed <= 0) {
            return 0;
        }

        ChemicalStack stack1 =
                inputTank1.getStack();

        ChemicalStack stack2 =
                inputTank2.getStack();

        if (stack1.isEmpty()
                || stack2.isEmpty()) {
            return 0;
        }

        AtomicAMRRecipe recipe =
                AtomicRecipes.AMR_RECIPES.findRecipe(
                        stack1.getChemical(),
                        stack1.getAmount(),
                        stack2.getChemical(),
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