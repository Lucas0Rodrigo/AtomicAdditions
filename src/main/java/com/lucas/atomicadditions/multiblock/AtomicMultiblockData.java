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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class AtomicMultiblockData
        extends MultiblockData {

    private static final long GAS_TANK_CAPACITY =
            8_000L;

    private static final long ENERGY_CAPACITY =
            80_000_000L;

    /*
     * Dados usados pelo renderer no cliente.
     *
     * -1 = nenhum gás.
     */
    public int renderInput1Color = -1;
    public int renderInput2Color = -1;

    public String renderInput1Name = "";
    public String renderInput2Name = "";

    public double renderEnergy = 0;
    public double renderProcessed = 0;
    public double renderProgress = 0;

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
     */
    @ContainerSync
    public double lastProcessed = 0;

    /*
     * Energia efetivamente consumida pelo AMR
     * no último tick de processamento.
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

        /*
         * Assim como no SPS, representa quanto realmente
         * foi processado durante o tick atual.
         */
        lastProcessed = 0;

        lastReceivedEnergy =
                FloatingLong.ZERO;

        if (!isFormed()) {
            processProgress = 0;

            /*
             * Mesmo parado, precisamos mandar para o cliente
             * quais gases existem para renderizar as esferas.
             */
            updateRenderData();

            return needsPacket;
        }

        GasStack stack1 =
                inputTank1.getStack();

        GasStack stack2 =
                inputTank2.getStack();

        if (stack1.isEmpty()
                || stack2.isEmpty()) {

            processProgress = 0;

            updateRenderData();

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

            updateRenderData();

            return needsPacket;
        }

        GasStack outputStack =
                outputTank.getStack();

        if (!outputStack.isEmpty()
                && outputStack.getType()
                != recipe.getOutput()) {

            processProgress = 0;

            updateRenderData();

            return needsPacket;
        }

        /*
         * Uma receita completa exige:
         *
         * energyPerTick × duration
         */
        double totalEnergyPerRecipe =
                (double) recipe.getEnergyPerTick()
                        * recipe.getDuration();

        if (totalEnergyPerRecipe <= 0) {
            processProgress = 0;

            updateRenderData();

            return needsPacket;
        }

        /*
         * Quantas receitas completas a energia disponível
         * permite avançar neste tick.
         */
        double processableByEnergy =
                energyContainer.getEnergy().doubleValue()
                        / totalEnergyPerRecipe;

        /*
         * Quantas receitas os gases permitem sustentar.
         */
        double processableByInput1 =
                (double) stack1.getAmount()
                        / recipe.getInput1Amount();

        double processableByInput2 =
                (double) stack2.getAmount()
                        / recipe.getInput2Amount();

        /*
         * Quantas receitas cabem na saída.
         */
        double processableByOutput =
                (double) outputTank.getNeeded()
                        / recipe.getOutputAmount();

        /*
         * O recurso mais escasso determina a velocidade.
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
            updateRenderData();

            return needsPacket;
        }

        /*
         * Energia necessária para avançar este fragmento.
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
            updateRenderData();

            return needsPacket;
        }

        FloatingLong extractedEnergy =
                energyContainer.extract(
                        energyToUse,
                        Action.EXECUTE,
                        AutomationType.INTERNAL
                );

        if (extractedEnergy.isZero()) {
            updateRenderData();

            return needsPacket;
        }

        /*
         * Guarda a energia realmente usada.
         */
        lastReceivedEnergy =
                extractedEnergy;

        /*
         * Converte a energia utilizada neste tick
         * em fração de receita.
         */
        double actualProcessable =
                extractedEnergy.doubleValue()
                        / totalEnergyPerRecipe;

        if (actualProcessable <= 0) {
            updateRenderData();

            return needsPacket;
        }

        /*
         * Acumula progresso.
         */
        processProgress +=
                actualProcessable;

        /*
         * Registra o trabalho deste tick.
         */
        lastProcessed =
                actualProcessable;

        /*
         * Quando chega a 100%, produz.
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
             * Consome os reagentes.
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
             * Produz a saída somente ao completar
             * uma receita inteira.
             */
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

        /*
         * Atualiza os dados que serão enviados ao renderer.
         */
        updateRenderData();

        /*
         * Mantemos uma frequência de atualização visual
         * alta o suficiente para a animação parecer contínua,
         * sem enviar um pacote a cada tick em qualquer situação.
         */
        if (world.getGameTime() % 2 == 0) {
            needsPacket = true;
        }

        return needsPacket;
    }

    /**
     * Prepara o snapshot utilizado pelo renderer.
     *
     * Este método roda no servidor.
     */
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

        renderProgress =
                getScaledProgress();
    }

    /**
     * Envia para o cliente somente os dados necessários
     * para a animação.
     */
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
                "amr_render_progress",
                renderProgress
        );
    }

    /**
     * Recebe no cliente os dados necessários
     * para o renderer.
     */
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

        renderProgress =
                tag.getDouble(
                        "amr_render_progress"
                );
    }

    /*
     * Retorna a taxa efetiva de produção em mB/t.
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

    /*
     * Retorna o progresso normalizado.
     */
    public double getScaledProgress() {
        return Math.min(
                1,
                processProgress
        );
    }
}