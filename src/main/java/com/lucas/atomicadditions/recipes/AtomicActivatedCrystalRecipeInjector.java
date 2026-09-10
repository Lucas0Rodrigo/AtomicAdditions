package com.lucas.atomicadditions.recipes;

import com.lucas.atomicadditions.AtomicAdditions;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.recipes.ChemicalCrystallizerRecipe;
import mekanism.api.recipes.ItemStackGasToItemStackRecipe;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.registries.MekanismGases;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.event.OnDatapackSyncEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AtomicActivatedCrystalRecipeInjector {

    private static final String GENERATED_PATH =
            "generated/activated_crystal/";

    private AtomicActivatedCrystalRecipeInjector() {
    }

    public static void onDatapackSync(
            OnDatapackSyncEvent event
    ) {

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] ========================================"
        );

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] OnDatapackSyncEvent disparado"
        );

        RecipeManager recipeManager =
                event.getPlayerList()
                        .getServer()
                        .getRecipeManager();

        injectRecipes(recipeManager);

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] ========================================"
        );
    }

    public static void injectRecipes(
            RecipeManager recipeManager
    ) {

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] Iniciando análise de receitas..."
        );

        Collection<Recipe<?>> existingRecipes =
                recipeManager.getRecipes();

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] Total de receitas no RecipeManager: {}",
                existingRecipes.size()
        );

        /*
         * =========================================================
         * 1. OBTÉM TODOS OS OUTPUTS DA CHEMICAL CRYSTALLIZER
         * =========================================================
         *
         * Esses são os itens que realmente pertencem à etapa
         * de cristalização.
         *
         * Isso impede que:
         *
         * raw_iron
         * iron_ore
         * raw_copper
         * etc.
         *
         * sejam tratados como cristais.
         */
        Set<Item> crystallizerOutputs =
                new HashSet<>();

        List<ChemicalCrystallizerRecipe>
                crystallizingRecipes =
                recipeManager.getAllRecipesFor(
                        MekanismRecipeType.CRYSTALLIZING.get()
                );

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] Receitas CRYSTALLIZING encontradas: {}",
                crystallizingRecipes.size()
        );

        for (
                ChemicalCrystallizerRecipe recipe :
                crystallizingRecipes
        ) {

            for (
                    ItemStack output :
                    recipe.getOutputDefinition()
            ) {

                if (output.isEmpty()) {
                    continue;
                }

                crystallizerOutputs.add(
                        output.getItem()
                );

                ResourceLocation outputId =
                        net.minecraft.core.registries
                                .BuiltInRegistries.ITEM
                                .getKey(
                                        output.getItem()
                                );

                AtomicAdditions.LOGGER.debug(
                        "[AA DEBUG] CRYSTALLIZING output detectado: {}",
                        outputId
                );
            }
        }

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] Itens produzidos por CRYSTALLIZING: {}",
                crystallizerOutputs.size()
        );

        /*
         * =========================================================
         * 2. OBTÉM INJECTING DIRETAMENTE DO RECIPE MANAGER
         * =========================================================
         */
        List<ItemStackGasToItemStackRecipe>
                injectingRecipes =
                recipeManager.getAllRecipesFor(
                        MekanismRecipeType.INJECTING.get()
                );

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] Receitas INJECTING no RecipeManager: {}",
                injectingRecipes.size()
        );

        List<Recipe<?>> finalRecipes =
                new ArrayList<>();

        int removedGenerated = 0;

        /*
         * Remove somente nossas receitas geradas anteriormente.
         */
        for (
                Recipe<?> recipe :
                existingRecipes
        ) {

            ResourceLocation id =
                    recipe.getId();

            if (isGeneratedRecipe(id)) {

                removedGenerated++;

                AtomicAdditions.LOGGER.info(
                        "[AA DEBUG] Removendo receita gerada antiga: {}",
                        id
                );

                continue;
            }

            finalRecipes.add(recipe);
        }

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] Receitas geradas removidas: {}",
                removedGenerated
        );

        /*
         * =========================================================
         * 3. IDENTIFICA CRISTAIS REAIS
         * =========================================================
         */
        GasStack hcl =
                new GasStack(
                        MekanismGases
                                .HYDROGEN_CHLORIDE
                                .get(),
                        1_000_000
                );

        int crystalsFound = 0;
        int crystalsAccepted = 0;
        int crystalsWithPurification = 0;
        int recipesCreated = 0;

        for (
                ItemStackGasToItemStackRecipe
                        injectingRecipe :
                injectingRecipes
        ) {

            if (injectingRecipe instanceof
                    AtomicActivatedCrystalRecipe) {
                continue;
            }

            List<ItemStack> representations =
                    injectingRecipe
                            .getItemInput()
                            .getRepresentations();

            if (representations.isEmpty()) {
                continue;
            }

            for (
                    ItemStack crystal :
                    representations
            ) {

                if (crystal.isEmpty()) {
                    continue;
                }

                /*
                 * FILTRO PRINCIPAL:
                 *
                 * O item precisa ser uma saída de uma
                 * Chemical Crystallizer.
                 */
                if (!crystallizerOutputs.contains(
                        crystal.getItem()
                )) {
                    continue;
                }

                /*
                 * Agora sim verificamos se existe a etapa:
                 *
                 * Crystal + HCl -> Shard
                 */
                if (!injectingRecipe.test(
                        crystal,
                        hcl
                )) {
                    continue;
                }

                ItemStack shard =
                        injectingRecipe.getOutput(
                                crystal,
                                hcl
                        );

                if (shard.isEmpty()) {
                    continue;
                }

                ResourceLocation crystalId =
                        net.minecraft.core.registries
                                .BuiltInRegistries.ITEM
                                .getKey(
                                        crystal.getItem()
                                );

                ResourceLocation shardId =
                        net.minecraft.core.registries
                                .BuiltInRegistries.ITEM
                                .getKey(
                                        shard.getItem()
                                );

                crystalsFound++;

                AtomicAdditions.LOGGER.info(
                        "[AA DEBUG] ----------------------------------------"
                );

                AtomicAdditions.LOGGER.info(
                        "[AA DEBUG] Cristal candidato: {}",
                        crystalId
                );

                AtomicAdditions.LOGGER.info(
                        "[AA DEBUG] Shard detectado: {}",
                        shardId
                );

                AtomicAdditions.LOGGER.info(
                        "[AA DEBUG] Receita original: {}",
                        injectingRecipe.getId()
                );

                /*
                 * =================================================
                 * 4. CONFERE A ETAPA DE PURIFICAÇÃO
                 * =================================================
                 *
                 * Shard + O2 -> Clump
                 */
                boolean hasPurification =
                        hasPurificationRecipe(
                                recipeManager,
                                shard
                        );

                AtomicAdditions.LOGGER.info(
                        "[AA DEBUG] Purificação com O2 encontrada: {}",
                        hasPurification
                );

                if (!hasPurification) {

                    AtomicAdditions.LOGGER.info(
                            "[AA DEBUG] Cristal descartado: sem purificação"
                    );

                    continue;
                }

                crystalsWithPurification++;

                /*
                 * =================================================
                 * 5. CRIA A RECEITA:
                 *
                 * 5 Crystal + 1 mB Tantalum
                 * =
                 * 8 Activated Crystal
                 * =================================================
                 */
                ResourceLocation generatedId =
                        createGeneratedId(
                                crystalId
                        );

                AtomicActivatedCrystalRecipe
                        activatedRecipe =
                        AtomicActivatedCrystalRecipe
                                .createActivationRecipe(
                                        generatedId,
                                        crystal
                                );

                /*
                 * Teste com exatamente 5 cristais.
                 */
                ItemStack activationInput =
                        crystal.copy();

                activationInput.setCount(5);

                GasStack tantalum =
                        new GasStack(
                                com.lucas.atomicadditions
                                        .chemical.AtomicGases
                                        .TANTALUM
                                        .get(),
                                1
                        );

                boolean acceptsTantalum =
                        activatedRecipe.test(
                                activationInput,
                                tantalum
                        );

                AtomicAdditions.LOGGER.info(
                        "[AA DEBUG] Receita criada: {}",
                        generatedId
                );

                AtomicAdditions.LOGGER.info(
                        "[AA DEBUG] Entrada da ativação: {} x{}",
                        crystalId,
                        activationInput.getCount()
                );

                AtomicAdditions.LOGGER.info(
                        "[AA DEBUG] Tântalo: {}",
                        tantalum.getTypeRegistryName()
                );

                AtomicAdditions.LOGGER.info(
                        "[AA DEBUG] Aceita Tântalo: {}",
                        acceptsTantalum
                );

                if (!acceptsTantalum) {

                    AtomicAdditions.LOGGER.error(
                            "[AA DEBUG] ERRO: receita gerada não aceita Tântalo!"
                    );

                    continue;
                }

                finalRecipes.add(
                        activatedRecipe
                );

                crystalsAccepted++;
                recipesCreated++;

                AtomicAdditions.LOGGER.info(
                        "[AA DEBUG] Receita adicionada ao RecipeManager."
                );
            }
        }

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] ========================================"
        );

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] Cristais encontrados: {}",
                crystalsFound
        );

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] Cristais aceitos: {}",
                crystalsAccepted
        );

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] Cristais com purificação: {}",
                crystalsWithPurification
        );

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] Receitas geradas: {}",
                recipesCreated
        );

        /*
         * =========================================================
         * 6. ATUALIZA O RECIPE MANAGER
         * =========================================================
         */
        recipeManager.replaceRecipes(
                finalRecipes
        );

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] RecipeManager.replaceRecipes() executado."
        );

        /*
         * Limpa os caches internos do Mekanism.
         */
        MekanismRecipeType.clearCache();

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] MekanismRecipeType.clearCache() executado."
        );

        /*
         * =========================================================
         * 7. CONFIRMA O RESULTADO
         * =========================================================
         */
        List<ItemStackGasToItemStackRecipe>
                finalInjectingRecipes =
                recipeManager.getAllRecipesFor(
                        MekanismRecipeType.INJECTING.get()
                );

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] Receitas INJECTING após replaceRecipes: {}",
                finalInjectingRecipes.size()
        );

        int generatedPresent = 0;

        for (
                ItemStackGasToItemStackRecipe recipe :
                finalInjectingRecipes
        ) {

            if (!(recipe instanceof
                    AtomicActivatedCrystalRecipe)) {
                continue;
            }

            generatedPresent++;

            AtomicActivatedCrystalRecipe
                    activatedRecipe =
                    (AtomicActivatedCrystalRecipe)
                            recipe;

            AtomicAdditions.LOGGER.info(
                    "[AA DEBUG] RECEITA GERADA PRESENTE: {}",
                    activatedRecipe.getId()
            );

            List<ItemStack> representations =
                    activatedRecipe
                            .getItemInput()
                            .getRepresentations();

            if (representations.isEmpty()) {
                continue;
            }

            ItemStack representative =
                    representations
                            .get(0)
                            .copy();

            representative.setCount(5);

            GasStack tantalum =
                    new GasStack(
                            com.lucas.atomicadditions
                                    .chemical.AtomicGases
                                    .TANTALUM
                                    .get(),
                            1
                    );

            boolean acceptsTantalum =
                    activatedRecipe.test(
                            representative,
                            tantalum
                    );

            AtomicAdditions.LOGGER.info(
                    "[AA DEBUG] Teste pós-registro - entrada: {} x{}",
                    net.minecraft.core.registries
                            .BuiltInRegistries.ITEM
                            .getKey(
                                    representative.getItem()
                            ),
                    representative.getCount()
            );

            AtomicAdditions.LOGGER.info(
                    "[AA DEBUG] Teste pós-registro - Tântalo: {}",
                    acceptsTantalum
            );
        }

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] Receitas geradas presentes no RecipeManager: {}",
                generatedPresent
        );

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] ========================================"
        );
    }

    private static boolean hasPurificationRecipe(
            RecipeManager recipeManager,
            ItemStack shard
    ) {

        GasStack oxygen =
                new GasStack(
                        MekanismGases.OXYGEN.get(),
                        1_000_000
                );

        /*
         * Consulta direta ao RecipeManager.
         */
        List<ItemStackGasToItemStackRecipe>
                purificationRecipes =
                recipeManager.getAllRecipesFor(
                        MekanismRecipeType.PURIFYING.get()
                );

        for (
                ItemStackGasToItemStackRecipe recipe :
                purificationRecipes
        ) {

            if (!recipe.test(
                    shard,
                    oxygen
            )) {
                continue;
            }

            ItemStack output =
                    recipe.getOutput(
                            shard,
                            oxygen
                    );

            if (!output.isEmpty()) {

                AtomicAdditions.LOGGER.info(
                        "[AA DEBUG] Purificação encontrada: {}",
                        recipe.getId()
                );

                return true;
            }
        }

        return false;
    }

    private static ResourceLocation
    createGeneratedId(
            ResourceLocation sourceId
    ) {

        return ResourceLocation.fromNamespaceAndPath(
                AtomicAdditions.MODID,
                GENERATED_PATH
                        + sourceId.getNamespace()
                        + "/"
                        + sourceId.getPath()
        );
    }

    private static boolean
    isGeneratedRecipe(
            ResourceLocation id
    ) {

        return AtomicAdditions.MODID.equals(
                id.getNamespace()
        )
                &&
                id.getPath().startsWith(
                        GENERATED_PATH
                );
    }
}