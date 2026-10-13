package com.lucas.atomicadditions.recipes;

import com.lucas.atomicadditions.AtomicAdditions;
import com.lucas.atomicadditions.chemical.AtomicGases;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.ChemicalCrystallizerRecipe;
import mekanism.api.recipes.ItemStackChemicalToItemStackRecipe;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.registries.MekanismChemicals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

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
        Collection<RecipeHolder<?>> existingRecipes =
                recipeManager.getRecipes();

        Set<Item> crystallizerOutputs =
                new HashSet<>();

        List<RecipeHolder<ChemicalCrystallizerRecipe>>
                crystallizingRecipes =
                MekanismRecipeType.CRYSTALLIZING.getRecipes(
                        recipeManager
                );

        for (
                RecipeHolder<ChemicalCrystallizerRecipe> holder :
                crystallizingRecipes
        ) {
            for (
                    ItemStack output :
                    holder.value().getOutputDefinition()
            ) {
                if (output.isEmpty()) {
                    continue;
                }

                crystallizerOutputs.add(
                        output.getItem()
                );
            }
        }

        List<RecipeHolder<ItemStackChemicalToItemStackRecipe>>
                injectingRecipes =
                MekanismRecipeType.INJECTING.getRecipes(
                        recipeManager
                );

        List<RecipeHolder<?>> finalRecipes =
                new ArrayList<>();

        for (
                RecipeHolder<?> recipe :
                existingRecipes
        ) {
            ResourceLocation id =
                    recipe.id();

            if (isGeneratedRecipe(id)) {
                continue;
            }

            finalRecipes.add(recipe);
        }

        AtomicActivatedCrystalRecipe
                .clearGeneratedRecipes();

        ChemicalStack hcl =
                MekanismChemicals
                        .HYDROGEN_CHLORIDE
                        .asStack(
                                1_000_000
                        );

        ChemicalStack tantalum =
                AtomicGases.TANTALUM.asStack(
                        1
                );

        ChemicalStack rhenium =
                AtomicGases.RHENIUM.asStack(
                        1
                );

        for (
                RecipeHolder<ItemStackChemicalToItemStackRecipe>
                        injectingHolder :
                injectingRecipes
        ) {
            ItemStackChemicalToItemStackRecipe injectingRecipe =
                    injectingHolder.value();

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

                RecipeHolder<AtomicActivatedCrystalRecipe>
                        activationHolder =
                        new RecipeHolder<>(
                                activationId,
                                activationRecipe
                        );

                finalRecipes.add(
                        activationHolder
                );

                AtomicActivatedCrystalRecipe
                        .registerGeneratedRecipe(
                                activationHolder
                        );

                ResourceLocation hclId =
                        createHclRecipeId(
                                crystalId
                        );

                AtomicActivatedCrystalRecipe
                        hclRecipe =
                        AtomicActivatedCrystalRecipe
                                .createHclRecipe(
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
                    RecipeHolder<AtomicActivatedCrystalRecipe>
                            hclHolder =
                            new RecipeHolder<>(
                                    hclId,
                                    hclRecipe
                            );

                    finalRecipes.add(
                            hclHolder
                    );

                    AtomicActivatedCrystalRecipe
                            .registerGeneratedRecipe(
                                    hclHolder
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
                    RecipeHolder<AtomicActivatedCrystalRecipe>
                            rheniumHolder =
                            new RecipeHolder<>(
                                    rheniumId,
                                    rheniumRecipe
                            );

                    finalRecipes.add(
                            rheniumHolder
                    );

                    AtomicActivatedCrystalRecipe
                            .registerGeneratedRecipe(
                                    rheniumHolder
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
        ChemicalStack oxygen =
                MekanismChemicals.OXYGEN.asStack(
                        1_000_000
                );

        List<RecipeHolder<ItemStackChemicalToItemStackRecipe>>
                purificationRecipes =
                MekanismRecipeType.PURIFYING.getRecipes(
                        recipeManager
                );

        for (
                RecipeHolder<ItemStackChemicalToItemStackRecipe> holder :
                purificationRecipes
        ) {
            ItemStackChemicalToItemStackRecipe recipe =
                    holder.value();

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