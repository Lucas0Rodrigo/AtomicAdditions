package com.lucas.atomicadditions.recipes;

import com.lucas.atomicadditions.AtomicAdditions;
import com.lucas.atomicadditions.chemical.AtomicGases;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.recipes.ItemStackGasToItemStackRecipe;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient.GasStackIngredient;
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

        List<ItemStackGasToItemStackRecipe>
                injectingRecipes =
                new ArrayList<>(
                        MekanismRecipeType.INJECTING
                                .get()
                                .getRecipes(null)
                );

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] Receitas INJECTING encontradas: {}",
                injectingRecipes.size()
        );

        List<Recipe<?>> finalRecipes =
                new ArrayList<>();

        int removedGenerated = 0;

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

            for (ItemStack crystal :
                    representations) {

                if (crystal.isEmpty()) {
                    continue;
                }

                /*
                 * Descobrimos o que essa receita aceita
                 * usando HCl.
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

                ResourceLocation crystalId =
                        net.minecraft.core.registries
                                .BuiltInRegistries.ITEM
                                .getKey(
                                        crystal.getItem()
                                );

                ResourceLocation shardId =
                        shard.isEmpty()
                                ? null
                                :
                                net.minecraft.core.registries
                                        .BuiltInRegistries.ITEM
                                        .getKey(
                                                shard.getItem()
                                        );

                /*
                 * Só considera como candidato aquilo que
                 * realmente produz um output.
                 */
                if (shard.isEmpty()) {

                    AtomicAdditions.LOGGER.info(
                            "[AA DEBUG] Candidato descartado: {} -> output vazio",
                            crystalId
                    );

                    continue;
                }

                crystalsFound++;

                AtomicAdditions.LOGGER.info(
                        "[AA DEBUG] ========================================"
                );

                AtomicAdditions.LOGGER.info(
                        "[AA DEBUG] CRISTAL CANDIDATO"
                );

                AtomicAdditions.LOGGER.info(
                        "[AA DEBUG] Cristal: {}",
                        crystalId
                );

                AtomicAdditions.LOGGER.info(
                        "[AA DEBUG] Shard encontrado: {}",
                        shardId
                );

                AtomicAdditions.LOGGER.info(
                        "[AA DEBUG] Receita original INJECTING: {}",
                        injectingRecipe.getId()
                );

                /*
                 * Verifica a cadeia seguinte:
                 *
                 * Shard + O2 -> Clump
                 */
                boolean purification =
                        hasPurificationRecipe(
                                shard
                        );

                AtomicAdditions.LOGGER.info(
                        "[AA DEBUG] Possui receita de purificação com O2: {}",
                        purification
                );

                if (!purification) {

                    AtomicAdditions.LOGGER.info(
                            "[AA DEBUG] DESCARTADO: não possui purificação compatível"
                    );

                    continue;
                }

                crystalsWithPurification++;

                AtomicActivatedCrystalRecipe
                        activatedRecipe =
                        AtomicActivatedCrystalRecipe
                                .createActivationRecipe(
                                        createGeneratedId(
                                                crystalId
                                        ),
                                        crystal
                                );

                /*
                 * Mostra exatamente o gás configurado
                 * na receita.
                 */
                logRecipeDetails(
                        activatedRecipe,
                        crystal
                );

                finalRecipes.add(
                        activatedRecipe
                );

                crystalsAccepted++;
                recipesCreated++;

                AtomicAdditions.LOGGER.info(
                        "[AA DEBUG] RECEITA ADICIONADA AO RecipeManager"
                );

                AtomicAdditions.LOGGER.info(
                        "[AA DEBUG] ========================================"
                );
            }
        }

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] Cristais candidatos encontrados: {}",
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
                "[AA DEBUG] Receitas de ativação criadas: {}",
                recipesCreated
        );

        /*
         * Substitui todas as receitas no RecipeManager.
         */
        recipeManager.replaceRecipes(
                finalRecipes
        );

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] RecipeManager.replaceRecipes() executado"
        );

        /*
         * Limpa os caches internos do Mekanism.
         */
        MekanismRecipeType.clearCache();

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] MekanismRecipeType.clearCache() executado"
        );

        /*
         * Segunda leitura:
         * verifica o que efetivamente está presente
         * depois do replaceRecipes().
         */
        List<ItemStackGasToItemStackRecipe>
                finalInjectingRecipes =
                MekanismRecipeType.INJECTING
                        .get()
                        .getRecipes(null);

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] Receitas INJECTING após replace/cache: {}",
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

            logRecipeDetails(
                    activatedRecipe,
                    activatedRecipe
                            .getItemInput()
                            .getRepresentations()
                            .stream()
                            .findFirst()
                            .orElse(
                                    ItemStack.EMPTY
                            )
            );
        }

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] Receitas geradas realmente presentes no INJECTING: {}",
                generatedPresent
        );
    }

    private static void logRecipeDetails(
            ItemStackGasToItemStackRecipe recipe,
            ItemStack representativeItem
    ) {

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] --- DETALHES DA RECEITA ---"
        );

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] ID: {}",
                recipe.getId()
        );

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] Tipo: {}",
                recipe.getType()
        );

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] Item representations: {}",
                recipe.getItemInput()
                        .getRepresentations()
                        .size()
        );

        for (ItemStack item :
                recipe.getItemInput()
                        .getRepresentations()) {

            ResourceLocation itemId =
                    net.minecraft.core.registries
                            .BuiltInRegistries.ITEM
                            .getKey(
                                    item.getItem()
                            );

            AtomicAdditions.LOGGER.info(
                    "[AA DEBUG] Item aceito: {} x{}",
                    itemId,
                    item.getCount()
            );
        }

        GasStack hcl =
                new GasStack(
                        MekanismGases
                                .HYDROGEN_CHLORIDE
                                .get(),
                        1
                );

        GasStack tantalum =
                new GasStack(
                        AtomicGases.TANTALUM.get(),
                        1
                );

        GasStack rhenium =
                new GasStack(
                        AtomicGases.RHENIUM.get(),
                        1
                );

        /*
         * Testa explicitamente os gases contra a receita.
         */
        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] test(HCl): {}",
                recipe.test(
                        representativeItem,
                        hcl
                )
        );

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] test(Tantalum): {}",
                recipe.test(
                        representativeItem,
                        tantalum
                )
        );

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] test(Rhenium): {}",
                recipe.test(
                        representativeItem,
                        rhenium
                )
        );

        AtomicAdditions.LOGGER.info(
                "[AA DEBUG] --- FIM DETALHES ---"
        );
    }

    private static boolean hasPurificationRecipe(
            ItemStack shard
    ) {

        GasStack oxygen =
                new GasStack(
                        MekanismGases.OXYGEN.get(),
                        1_000_000
                );

        for (
                ItemStackGasToItemStackRecipe
                        purificationRecipe :
                MekanismRecipeType.PURIFYING
                        .get()
                        .getRecipes(null)
        ) {

            if (!purificationRecipe.test(
                    shard,
                    oxygen
            )) {
                continue;
            }

            ItemStack output =
                    purificationRecipe.getOutput(
                            shard,
                            oxygen
                    );

            if (!output.isEmpty()) {

                AtomicAdditions.LOGGER.info(
                        "[AA DEBUG] Purificação encontrada: {} -> {}",
                        shard.getItem(),
                        output.getItem()
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
                id.getPath()
                        .startsWith(
                                GENERATED_PATH
                        );
    }
}