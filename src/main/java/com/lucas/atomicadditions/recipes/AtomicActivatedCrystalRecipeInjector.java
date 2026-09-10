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

        int removedGenerated = 0;

        for (
                Recipe<?> recipe :
                existingRecipes
        ) {
            ResourceLocation id =
                    recipe.getId();

            if (isGeneratedRecipe(id)) {
                removedGenerated++;
                continue;
            }

            finalRecipes.add(recipe);
        }

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

                ResourceLocation shardId =
                        net.minecraft.core.registries
                                .BuiltInRegistries.ITEM
                                .getKey(
                                        shard.getItem()
                                );

                crystalsFound++;

                boolean hasPurification =
                        hasPurificationRecipe(
                                recipeManager,
                                shard
                        );

                if (!hasPurification) {
                    continue;
                }

                crystalsWithPurification++;

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

                if (!acceptsTantalum) {
                    continue;
                }

                finalRecipes.add(
                        activatedRecipe
                );

                AtomicActivatedCrystalRecipe.clearGeneratedRecipes();

                crystalsAccepted++;
                recipesCreated++;
            }
        }

        recipeManager.replaceRecipes(
                finalRecipes
        );

        MekanismRecipeType.clearCache();

        List<ItemStackGasToItemStackRecipe>
                finalInjectingRecipes =
                recipeManager.getAllRecipesFor(
                        MekanismRecipeType.INJECTING.get()
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
        }
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