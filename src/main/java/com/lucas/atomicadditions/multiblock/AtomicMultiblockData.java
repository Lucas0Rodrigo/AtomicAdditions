package com.lucas.atomicadditions.multiblock;

import com.lucas.atomicadditions.chemical.AtomicGases;
import com.lucas.atomicadditions.recipes.AtomicAMRRecipe;
import com.lucas.atomicadditions.recipes.AtomicRecipes;
import com.mojang.logging.LogUtils;
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
import org.slf4j.Logger;

public class AtomicMultiblockData
        extends MultiblockData {

    private static final Logger LOGGER =
            LogUtils.getLogger();

    private static final long GAS_TANK_CAPACITY =
            4_000L;

    private static final long ENERGY_CAPACITY =
            40_000_000L;

    public final Set<BlockPos> coils =
            new HashSet<>();

    @ContainerSync
    public final IGasTank inputTank1;

    @ContainerSync
    public final IGasTank inputTank2;

    @ContainerSync
    public final IGasTank outputTank;

    public final IEnergyContainer energyContainer;

    /*
     * Progresso fracionário da receita atual.
     *
     * 0.0 = início
     * 1.0 = uma receita completa
     */
    @ContainerSync
    public double processProgress = 0;

    /*
     * Quanto foi processado neste tick.
     *
     * É usado pela GUI para determinar se o AMR está ativo.
     */
    @ContainerSync
    public double lastProcessed = 0;

    /*
     * Energia efetivamente consumida pelo AMR
     * no último tick de processamento.
     *
     * Equivalente ao lastReceivedEnergy do SPS.
     */
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
            logDebugState(world);
            return needsPacket;
        }

        GasStack stack1 =
                inputTank1.getStack();

        GasStack stack2 =
                inputTank2.getStack();

        if (stack1.isEmpty()
                || stack2.isEmpty()) {

            processProgress = 0;
            logDebugState(world);
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
            logDebugState(world);
            return needsPacket;
        }

        GasStack outputStack =
                outputTank.getStack();

        if (!outputStack.isEmpty()
                && outputStack.getType()
                != recipe.getOutput()) {

            processProgress = 0;
            logDebugState(world);
            return needsPacket;
        }

        /*
         * Uma receita completa exige:
         *
         * energyPerTick × duration
         *
         * Exemplo do Rênio:
         *
         * 250.000 FE/t × 400 ticks
         * = 100.000.000 FE
         */
        double totalEnergyPerRecipe =
                (double) recipe.getEnergyPerTick()
                        * recipe.getDuration();

        if (totalEnergyPerRecipe <= 0) {
            processProgress = 0;
            logDebugState(world);
            return needsPacket;
        }

        /*
         * Quantas receitas completas a energia armazenada
         * permite avançar neste tick.
         */
        double processableByEnergy =
                energyContainer.getEnergy().doubleValue()
                        / totalEnergyPerRecipe;

        /*
         * Quantas receitas completas os gases disponíveis
         * permitem sustentar.
         */
        double processableByInput1 =
                (double) stack1.getAmount()
                        / recipe.getInput1Amount();

        double processableByInput2 =
                (double) stack2.getAmount()
                        / recipe.getInput2Amount();

        /*
         * Quantas receitas completas cabem no tanque de saída.
         */
        double processableByOutput =
                (double) outputTank.getNeeded()
                        / recipe.getOutputAmount();

        /*
         * Assim como no SPS, o processamento é limitado pelo
         * recurso mais escasso.
         */
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
            logDebugState(world);
            return needsPacket;
        }

        /*
         * Quanto de energia realmente precisamos consumir
         * para avançar esse pedaço da receita.
         */
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
            logDebugState(world);
            return needsPacket;
        }

        FloatingLong extractedEnergy =
                energyContainer.extract(
                        energyToUse,
                        Action.EXECUTE,
                        AutomationType.INTERNAL
                );

        if (extractedEnergy.isZero()) {
            logDebugState(world);
            return needsPacket;
        }

        /*
         * Guarda a energia realmente usada no último tick
         * para a interface, igual ao SPS.
         */
        lastReceivedEnergy =
                extractedEnergy;

        /*
         * Recalcula a fração real caso a extração efetiva
         * tenha sido ligeiramente menor que a solicitada.
         */
        double actualProcessable =
                extractedEnergy.doubleValue()
                        / totalEnergyPerRecipe;

        if (actualProcessable <= 0) {
            logDebugState(world);
            return needsPacket;
        }

        /*
         * Acumula progresso da receita.
         */
        processProgress +=
                actualProcessable;

        /*
         * A máquina esteve trabalhando neste tick.
         */
        lastProcessed =
                actualProcessable;

        /*
         * Quando a barra chega a 100%, produzimos a saída.
         *
         * Caso o processamento esteja muito rápido e
         * atravesse vários ciclos em um único tick, o loop
         * completa todas as receitas possíveis e conserva
         * o restante do progresso fracionário.
         */
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

            /*
             * Consome os dois reagentes.
             */
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

            /*
             * Produz somente quando a receita chegou
             * a 100% do progresso.
             */
            outputTank.insert(
                    new GasStack(
                            recipe.getOutput(),
                            recipe.getOutputAmount()
                    ),
                    Action.EXECUTE,
                    AutomationType.INTERNAL
            );

            /*
             * Remove uma receita completa do progresso.
             */
            processProgress -= 1.0;

            needsPacket = true;
        }

        if (processProgress > 0) {
            needsPacket = true;
        }

        logDebugState(world);

        return needsPacket;
    }

    /*
     * Retorna a taxa efetiva de produção em mB/t
     * durante o último tick.
     *
     * lastProcessed representa quantas receitas
     * fracionárias foram processadas neste tick.
     *
     * Multiplicamos pela quantidade de saída da receita.
     */
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

    private void logDebugState(
            Level world
    ) {
        if (world.isClientSide
                || world.getGameTime() % 20 != 0) {
            return;
        }

        GasStack debugInput1 =
                inputTank1.getStack();

        GasStack debugInput2 =
                inputTank2.getStack();

        GasStack debugOutput =
                outputTank.getStack();

        LOGGER.info(
                "[AMR-DEBUG] STATE | "
                        + "formed={} | "
                        + "input1={} mB ({}) | "
                        + "input2={} mB ({}) | "
                        + "output={} mB ({}) | "
                        + "energy={} | "
                        + "progress={} | "
                        + "scaledProgress={} | "
                        + "lastProcessed={} | "
                        + "lastReceivedEnergy={} | "
                        + "processRate={} mB/t",
                isFormed(),
                inputTank1.getStored(),
                debugInput1.isEmpty()
                        ? "EMPTY"
                        : debugInput1.getType(),
                inputTank2.getStored(),
                debugInput2.isEmpty()
                        ? "EMPTY"
                        : debugInput2.getType(),
                outputTank.getStored(),
                debugOutput.isEmpty()
                        ? "EMPTY"
                        : debugOutput.getType(),
                energyContainer.getEnergy(),
                processProgress,
                getScaledProgress(),
                lastProcessed,
                lastReceivedEnergy,
                getProcessRate()
        );
    }

    /*
     * Retorna o progresso normalizado da receita atual.
     *
     * 0.0 = 0%
     * 0.5 = 50%
     * 1.0 = 100%
     */
    public double getScaledProgress() {
        return Math.min(
                1,
                processProgress
        );
    }
}
