package com.lucas.atomicadditions.recipes;

import com.lucas.atomicadditions.AtomicAdditions;
import com.lucas.atomicadditions.item.ActivatedCrystalItem;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.recipes.ItemStackGasToItemStackRecipe;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.registries.MekanismGases;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.event.OnDatapackSyncEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
         * Consulta diretamente o RecipeManager.
         * Não usamos o cache do Mekanism para descobrir
         * as receitas originais.
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
         * Mantém todas as receitas originais e remove
         * somente as nossas receitas geradas anteriormente.
         */
        for (Recipe<?> recipe :
                existingRecipes) {

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
         * HCl usado para identificar as receitas originais
         * de cristal -> shard do Mekanism.
         *
         * O valor da quantidade aqui não representa a quantidade
         * consumida pela máquina. Ele serve apenas para testar
         * se a receita aceita o gás.
         */
        GasStack hcl =
                new GasStack(
                        MekanismGases
                                .HYDROGEN_CHLORIDE
                                .get(),
                        1
                );

        int crystalsFound = 0;
        int crystalsAccepted = 0;
        int crystalsWithPurification = 0;
        int recipesCreated = 0;

        /*
         * Procura todas as receitas INJECTING existentes.
         */
        for (
                ItemStackGasToItemStackRecipe
                        injectingRecipe :
                injectingRecipes
        ) {

            /*
             * Não analisa receitas do próprio Atomic Additions.
             */
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

            for (ItemStack crystal :
                    representations) {

                if (crystal.isEmpty()) {
                    continue;
                }

                /*
                 * Descobrimos se esta é uma receita de:
                 *
                 * CRISTAL + HCl -> SHARD
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
                 * Verifica se o shard possui purificação:
                 *
                 * SHARD + O2 -> CLUMP
                 *
                 * Isso elimina receitas de injecting que não
                 * pertencem à cadeia de quintuplicação.
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

                ResourceLocation generatedId =
                        createGeneratedId(
                                crystalId
                        );

                /*
                 * Cria:
                 *
                 * 5 CRISTAIS + TÂNTALO
                 *        ↓
                 * 8 CRISTAIS ATIVADOS
                 */
                AtomicActivatedCrystalRecipe
                        activatedRecipe =
                        AtomicActivatedCrystalRecipe
                                .createActivationRecipe(
                                        generatedId,
                                        crystal
                                );

                /*
                 * IMPORTANTE:
                 *
                 * A receita exige 5 cristais.
                 * Por isso o teste também deve usar
                 * um ItemStack contendo 5 unidades.
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
         * Substitui as receitas carregadas.
         */
        recipeManager.replaceRecipes(
                finalRecipes
        );

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] RecipeManager.replaceRecipes() executado."
        );

        /*
         * Limpa os caches do Mekanism.
         */
        MekanismRecipeType.clearCache();

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] MekanismRecipeType.clearCache() executado."
        );

        /*
         * Confere diretamente no RecipeManager se as
         * receitas geradas realmente foram adicionadas.
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

            ItemStack representative =
                    activatedRecipe
                            .getItemInput()
                            .getRepresentations()
                            .stream()
                            .findFirst()
                            .orElse(
                                    ItemStack.EMPTY
                            );

            if (representative.isEmpty()) {
                continue;
            }

            /*
             * A receita exige cinco cristais.
             */
            representative =
                    representative.copy();

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
                        1
                );

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