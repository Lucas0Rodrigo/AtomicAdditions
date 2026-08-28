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
import
        mekanism.common.capabilities.chemical.multiblock.MultiblockChemicalTankBuilder;
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
            1_000_000L;
    private static final long ENERGY_CAPACITY =
            400_000_000L;
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
                                        || gas ==
                                        AtomicGases.PALLADIUM.get(),
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
        /*
         * Energia total necessária para produzir
         * toda a quantidade de saída definida pela receita.
         *
         * Exemplo do Rênio:
         *
         * 250.000 FE/t × 400 ticks
         * = 100.000.000 FE
         * para produzir 1.000 mB de Rênio.
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
         * Converte o custo total da receita para
         * custo energético por 1 mB de produto.
         */
        double energyPerOutputMb =
                totalEnergyPerRecipe
                        / recipe.getOutputAmount();
        /*
         * Quantos mB de produto a energia disponível
         * permite produzir neste tick.
         */
        double processableByEnergy =
                energyContainer.getEnergy().doubleValue()
                        / energyPerOutputMb;
        /*
         * Quantos mB de produto o primeiro gás permite produzir.
         */
        double processableByInput1 =
                (double) stack1.getAmount()
                        * recipe.getOutputAmount()
                        / recipe.getInput1Amount();
        /*
         * Quantos mB de produto o segundo gás permite produzir.
         */
        double processableByInput2 =
                (double) stack2.getAmount()
                        * recipe.getOutputAmount()
                        / recipe.getInput2Amount();
        /*
         * Espaço disponível no tanque de saída.
         */
        double processableByOutput =
                (double) outputTank.getNeeded();
        /*
         * A produção é limitada pelo recurso mais escasso.
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
        /*
         * Acumula produção fracionária.
         *
         * Isso permite que o AMR, assim como o SPS,
         * trabalhe com quantidades que não sejam números
         * inteiros a cada tick.
         */
        processProgress += processable;
        long outputToProcess =
                (long) Math.floor(processProgress);
        if (outputToProcess <= 0) {
            logDebugState(world);
            return needsPacket;
        }
        /*
         * Recalcula a quantidade efetivamente processável
         * para impedir que arredondamentos ultrapassem
         * qualquer recurso disponível.
         */
        long maxByInput1 =
                stack1.getAmount()
                        * recipe.getOutputAmount()
                        / recipe.getInput1Amount();
        long maxByInput2 =
                stack2.getAmount()
                        * recipe.getOutputAmount()
                        / recipe.getInput2Amount();
        long maxByOutput =
                outputTank.getNeeded();
        outputToProcess =
                Math.min(
                        outputToProcess,
                        Math.min(
                                maxByInput1,
                                Math.min(
                                        maxByInput2,
                                        maxByOutput
                                )
                        )
                );
        if (outputToProcess <= 0) {
            logDebugState(world);
            return needsPacket;
        }
        /*
         * Quanto dos gases é necessário para a quantidade
         * efetivamente produzida.
         *
         * A divisão inteira é segura porque o processo
         * trabalha com quantidades inteiras de mB.
         */
        long input1ToConsume =
                (long) Math.ceil(
                        (double) outputToProcess
                                * recipe.getInput1Amount()
                                / recipe.getOutputAmount()
                );
        long input2ToConsume =
                (long) Math.ceil(
                        (double) outputToProcess
                                * recipe.getInput2Amount()
                                / recipe.getOutputAmount()
                );
        /*
         * Revalidação final para evitar ultrapassar
         * os tanques devido ao arredondamento.
         */
        if (input1ToConsume > inputTank1.getStored()
                || input2ToConsume > inputTank2.getStored()) {
            long byInput1 =
                    (long) Math.floor(
                            (double) inputTank1.getStored()
                                    * recipe.getOutputAmount()
                                    / recipe.getInput1Amount()
                    );
            long byInput2 =
                    (long) Math.floor(
                            (double) inputTank2.getStored()
                                    * recipe.getOutputAmount()
                                    / recipe.getInput2Amount()
                    );
            outputToProcess =
                    Math.min(
                            outputToProcess,
                            Math.min(
                                    byInput1,
                                    byInput2
                            )
                    );
            if (outputToProcess <= 0) {
                processProgress = 0;
                logDebugState(world);
                return needsPacket;
            }
            input1ToConsume =
                    (long) Math.ceil(
                            (double) outputToProcess
                                    * recipe.getInput1Amount()
                                    / recipe.getOutputAmount()
                    );
            input2ToConsume =
                    (long) Math.ceil(
                            (double) outputToProcess
                                    * recipe.getInput2Amount()
                                    / recipe.getOutputAmount()
                    );
        }
        /*
         * Energia necessária para a quantidade de produto
         * que será realmente produzida neste tick.
         */
        double energyToConsumeDouble =
                outputToProcess
                        * energyPerOutputMb;
        FloatingLong energyToConsume =
                FloatingLong.create(
                        Math.min(
                                energyToConsumeDouble,
                                energyContainer.getEnergy()
                                        .doubleValue()
                        )
                );
        if (energyToConsume.isZero()) {
            logDebugState(world);
            return needsPacket;
        }
        FloatingLong extractedEnergy =
                energyContainer.extract(
                        energyToConsume,
                        Action.EXECUTE,
                        AutomationType.INTERNAL
                );
        if (extractedEnergy.isZero()) {
            logDebugState(world);
            return needsPacket;
        }
        /*
         * Se por algum motivo o container extraiu menos
         * energia que o calculado, reduzimos a produção
         * para manter energia e gás perfeitamente alinhados.
         */
        double actualOutputDouble =
                extractedEnergy.doubleValue()
                        / energyPerOutputMb;
        long actualOutput =
                (long) Math.floor(
                        actualOutputDouble
                );
        if (actualOutput <= 0) {
            logDebugState(world);
            return needsPacket;
        }
        if (actualOutput < outputToProcess) {
            outputToProcess =
                    actualOutput;
            input1ToConsume =
                    (long) Math.ceil(
                            (double) outputToProcess
                                    * recipe.getInput1Amount()
                                    / recipe.getOutputAmount()
                    );
            input2ToConsume =
                    (long) Math.ceil(
                            (double) outputToProcess
                                    * recipe.getInput2Amount()
                                    / recipe.getOutputAmount()
                    );
        }
        /*
         * Consome os dois reagentes.
         */
        inputTank1.extract(
                input1ToConsume,
                Action.EXECUTE,
                AutomationType.INTERNAL
        );
        inputTank2.extract(
                input2ToConsume,
                Action.EXECUTE,
                AutomationType.INTERNAL
        );
        /*
         * Produz proporcionalmente ao processamento.
         */
        outputTank.insert(
                new GasStack(
                        recipe.getOutput(),
                        outputToProcess
                ),
                Action.EXECUTE,
                AutomationType.INTERNAL
        );
        /*
         * O progresso representa mB de produto produzido
         * e mantém a parte fracionária para o próximo tick.
         */
        processProgress -=
                outputToProcess;
        lastProcessed =
                outputToProcess;
        needsPacket = true;
        logDebugState(world);
        return needsPacket;
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
                        + "lastProcessed={}",
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
                lastProcessed
        );
    }
    /**
     * Retorna o progresso normalizado da produção atual.
     *
     * O progresso é baseado na quantidade de saída
     * produzida em relação à quantidade de saída de uma
     * receita completa.
     */
    public double getScaledProgress() {
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
        if (recipe == null
                || recipe.getOutputAmount() <= 0) {
            return 0;
        }
        return Math.min(
                1,
                processProgress
                        / recipe.getOutputAmount()
        );
    }
}