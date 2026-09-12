package com.lucas.atomicadditions.recipes;

import com.lucas.atomicadditions.AtomicAdditions;
import com.lucas.atomicadditions.chemical.AtomicGases;
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
        RecipeManager recipeManager =
                event.getPlayerList()
                        .getServer()
                        .getRecipeManager();

        injectRecipes(recipeManager);
    }

    public static void injectRecipes(
            RecipeManager recipeManager
    ) {
        Collection<Recipe<?>> existingRecipes =
                recipeManager.getRecipes();

        Set<Item> crystallizerOutputs =
                new HashSet<>();

        List<ChemicalCrystallizerRecipe>
                crystallizingRecipes =
                recipeManager.getAllRecipesFor(
                        MekanismRecipeType.CRYSTALLIZING.get()
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
            }
        }

        List<ItemStackGasToItemStackRecipe>
                injectingRecipes =
                recipeManager.getAllRecipesFor(
                        MekanismRecipeType.INJECTING.get()
                );

        List<Recipe<?>> finalRecipes =
                new ArrayList<>();

        for (
                Recipe<?> recipe :
                existingRecipes
        ) {
            ResourceLocation id =
                    recipe.getId();

            if (isGeneratedRecipe(id)) {
                continue;
            }

            finalRecipes.add(recipe);
        }

        AtomicActivatedCrystalRecipe
                .clearGeneratedRecipes();

        GasStack hcl =
                new GasStack(
                        MekanismGases
                                .HYDROGEN_CHLORIDE
                                .get(),
                        1_000_000
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

                if (!crystallizerOutputs.contains(
                        crystal.getItem()
                )) {
                    continue;
                }

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

                boolean hasPurification =
                        hasPurificationRecipe(
                                recipeManager,
                                shard
                        );

                if (!hasPurification) {
                    continue;
                }

                ResourceLocation activationId =
                        createGeneratedId(
                                crystalId
                        );

                AtomicActivatedCrystalRecipe
                        activationRecipe =
                        AtomicActivatedCrystalRecipe
                                .createActivationRecipe(
                                        activationId,
                                        crystal
                                );

                ItemStack activationInput =
                        crystal.copy();

                activationInput.setCount(5);

                if (!activationRecipe.test(
                        activationInput,
                        tantalum
                )) {
                    continue;
                }

                finalRecipes.add(
                        activationRecipe
                );

                AtomicActivatedCrystalRecipe
                        .registerGeneratedRecipe(
                                activationRecipe
                        );

                ResourceLocation hclId =
                        createHclRecipeId(
                                crystalId
                        );

                AtomicActivatedCrystalRecipe
                        hclRecipe =
                        AtomicActivatedCrystalRecipe
                                .createHclRecipe(
                                        hclId,
                                        crystal,
                                        shard
                                );

                if (hclRecipe.test(
                        hclRecipe
                                .getItemInput()
                                .getRepresentations()
                                .get(0),
                        hcl
                )) {
                    finalRecipes.add(
                            hclRecipe
                    );

                    AtomicActivatedCrystalRecipe
                            .registerGeneratedRecipe(
                                    hclRecipe
                            );
                }

                ResourceLocation rheniumId =
                        createRheniumRecipeId(
                                crystalId
                        );

                AtomicActivatedCrystalRecipe
                        rheniumRecipe =
                        AtomicActivatedCrystalRecipe
                                .createRheniumRecipe(
                                        rheniumId,
                                        crystal,
                                        shard
                                );

                if (rheniumRecipe.test(
                        rheniumRecipe
                                .getItemInput()
                                .getRepresentations()
                                .get(0),
                        rhenium
                )) {
                    finalRecipes.add(
                            rheniumRecipe
                    );

                    AtomicActivatedCrystalRecipe
                            .registerGeneratedRecipe(
                                    rheniumRecipe
                            );
                }
            }
        }

        recipeManager.replaceRecipes(
                finalRecipes
        );

        MekanismRecipeType.clearCache();
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
                return true;
            }
        }

        return false;
    }

    private static ResourceLocation createGeneratedId(
            ResourceLocation sourceId
    ) {
        return ResourceLocation.fromNamespaceAndPath(
                AtomicAdditions.MODID,
                GENERATED_PATH
                        + sourceId.getNamespace()
                        + "/"
                        + sourceId.getPath()
                        + "/activate"
        );
    }

    private static ResourceLocation createHclRecipeId(
            ResourceLocation sourceId
    ) {
        return ResourceLocation.fromNamespaceAndPath(
                AtomicAdditions.MODID,
                GENERATED_PATH
                        + sourceId.getNamespace()
                        + "/"
                        + sourceId.getPath()
                        + "/hydrogen_chloride"
        );
    }

    private static ResourceLocation createRheniumRecipeId(
            ResourceLocation sourceId
    ) {
        return ResourceLocation.fromNamespaceAndPath(
                AtomicAdditions.MODID,
                GENERATED_PATH
                        + sourceId.getNamespace()
                        + "/"
                        + sourceId.getPath()
                        + "/rhenium"
        );
    }

    private static boolean isGeneratedRecipe(
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