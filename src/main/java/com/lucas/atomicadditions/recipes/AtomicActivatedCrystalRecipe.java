package com.lucas.atomicadditions.recipes;

import com.lucas.atomicadditions.AtomicAdditions;
import com.lucas.atomicadditions.item.ActivatedCrystalItem;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.recipes.ItemStackGasToItemStackRecipe;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient.GasStackIngredient;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.registries.MekanismGases;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AtomicActivatedCrystalRecipe
        extends ItemStackGasToItemStackRecipe {

    private static final TagKey<Item> CRYSTALS_TAG =
            TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(
                            "c",
                            "crystals"
                    )
            );

    private final AtomicRecipeSerializers.Operation operation;

    public AtomicActivatedCrystalRecipe(
            ResourceLocation id,
            ItemStackIngredient itemInput,
            GasStackIngredient gasInput,
            ItemStack output,
            AtomicRecipeSerializers.Operation operation
    ) {
        super(
                id,
                itemInput,
                gasInput,
                output
        );

        this.operation = operation;
    }

    public AtomicRecipeSerializers.Operation getOperation() {
        return operation;
    }

    @Override
    public RecipeType<ItemStackGasToItemStackRecipe> getType() {
        return MekanismRecipeType.INJECTING.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<ItemStackGasToItemStackRecipe> getSerializer() {

        RecipeSerializer<?> serializer;

        switch (operation) {
            case ACTIVATE ->
                    serializer =
                            AtomicRecipeSerializers.ACTIVATE.get();

            case HYDROGEN_CHLORIDE ->
                    serializer =
                            AtomicRecipeSerializers.HYDROGEN_CHLORIDE.get();

            case RHENIUM ->
                    serializer =
                            AtomicRecipeSerializers.RHENIUM.get();

            default ->
                    throw new IllegalStateException(
                            "Unknown activated crystal operation: "
                                    + operation
                    );
        }

        return (RecipeSerializer<ItemStackGasToItemStackRecipe>)
                (RecipeSerializer<?>) serializer;
    }

    @Override
    public String getGroup() {
        return MekanismBlocks.CHEMICAL_INJECTION_CHAMBER
                .getName();
    }

    @Override
    public ItemStack getToastSymbol() {
        return MekanismBlocks.CHEMICAL_INJECTION_CHAMBER
                .getItemStack();
    }

    @Override
    public boolean test(
            ItemStack itemStack,
            GasStack gasStack
    ) {

        if (!super.test(
                itemStack,
                gasStack
        )) {
            return false;
        }

        return switch (operation) {

            case ACTIVATE ->
                    isCompatibleCrystal(itemStack);

            case HYDROGEN_CHLORIDE,
                 RHENIUM ->
                    isActivatedCrystal(itemStack)
                            &&
                            findOriginalShardRecipe(
                                    itemStack
                            ) != null;
        };
    }

    @Override
    public ItemStack getOutput(
            ItemStack inputItem,
            GasStack inputGas
    ) {

        return switch (operation) {

            case ACTIVATE ->
                    createActivatedCrystal(
                            inputItem
                    );

            case HYDROGEN_CHLORIDE ->
                    createShardOutput(
                            inputItem,
                            8
                    );

            case RHENIUM ->
                    createShardOutput(
                            inputItem,
                            16
                    );
        };
    }

    @Override
    public List<@NotNull ItemStack> getOutputDefinition() {

        List<ItemStack> outputs =
                new ArrayList<>();

        GasStack hcl =
                new GasStack(
                        MekanismGases.HYDROGEN_CHLORIDE.get(),
                        1
                );

        List<ItemStackGasToItemStackRecipe>
                injectingRecipes =
                MekanismRecipeType.INJECTING
                        .get()
                        .getRecipes(null);

        if (operation ==
                AtomicRecipeSerializers.Operation.ACTIVATE) {

            for (
                    ItemStackGasToItemStackRecipe recipe :
                    injectingRecipes
            ) {

                if (recipe instanceof
                        AtomicActivatedCrystalRecipe) {
                    continue;
                }

                for (
                        ItemStack representation :
                        recipe.getItemInput()
                                .getRepresentations()
                ) {

                    if (!representation.is(
                            CRYSTALS_TAG
                    )) {
                        continue;
                    }

                    if (!recipe.test(
                            representation,
                            hcl
                    )) {
                        continue;
                    }

                    ResourceLocation sourceId =
                            BuiltInRegistries.ITEM.getKey(
                                    representation.getItem()
                            );

                    ItemStack activated =
                            new ItemStack(
                                    AtomicAdditions
                                            .ACTIVATED_CRYSTAL
                                            .get()
                            );

                    ActivatedCrystalItem.setSourceCrystal(
                            activated,
                            sourceId
                    );

                    outputs.add(activated);
                }
            }

        } else {

            int amount =
                    operation ==
                            AtomicRecipeSerializers
                                    .Operation.RHENIUM
                            ? 16
                            : 8;

            for (
                    ItemStackGasToItemStackRecipe recipe :
                    injectingRecipes
            ) {

                if (recipe instanceof
                        AtomicActivatedCrystalRecipe) {
                    continue;
                }

                for (
                        ItemStack representation :
                        recipe.getItemInput()
                                .getRepresentations()
                ) {

                    if (!representation.is(
                            CRYSTALS_TAG
                    )) {
                        continue;
                    }

                    if (!recipe.test(
                            representation,
                            hcl
                    )) {
                        continue;
                    }

                    ItemStack result =
                            recipe.getOutput(
                                    representation,
                                    hcl
                            );

                    if (result.isEmpty()) {
                        continue;
                    }

                    result.setCount(amount);

                    outputs.add(result);
                }
            }
        }

        if (outputs.isEmpty()) {
            return super.getOutputDefinition();
        }

        return Collections.unmodifiableList(
                outputs
        );
    }

    private ItemStack createActivatedCrystal(
            ItemStack sourceCrystal
    ) {

        ItemStack result =
                new ItemStack(
                        AtomicAdditions
                                .ACTIVATED_CRYSTAL
                                .get()
                );

        ResourceLocation sourceId =
                BuiltInRegistries.ITEM.getKey(
                        sourceCrystal.getItem()
                );

        ActivatedCrystalItem.setSourceCrystal(
                result,
                sourceId
        );

        return result;
    }

    private ItemStack createShardOutput(
            ItemStack activatedCrystal,
            int amount
    ) {

        ItemStackGasToItemStackRecipe recipe =
                findOriginalShardRecipe(
                        activatedCrystal
                );

        if (recipe == null) {
            return ItemStack.EMPTY;
        }

        ResourceLocation sourceId =
                ActivatedCrystalItem
                        .getSourceCrystalId(
                                activatedCrystal
                        );

        ItemStack source =
                createSourceStack(sourceId);

        if (source.isEmpty()) {
            return ItemStack.EMPTY;
        }

        GasStack hcl =
                new GasStack(
                        MekanismGases.HYDROGEN_CHLORIDE.get(),
                        1
                );

        ItemStack result =
                recipe.getOutput(
                        source,
                        hcl
                );

        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }

        result.setCount(amount);

        return result;
    }

    private boolean isActivatedCrystal(
            ItemStack stack
    ) {
        return stack.getItem()
                ==
                AtomicAdditions
                        .ACTIVATED_CRYSTAL
                        .get();
    }

    private boolean isCompatibleCrystal(
            ItemStack stack
    ) {

        if (!stack.is(CRYSTALS_TAG)) {
            return false;
        }

        if (!getItemInput().test(stack)) {
            return false;
        }

        return findShardRecipe(stack) != null;
    }

    private ItemStackGasToItemStackRecipe
    findOriginalShardRecipe(
            ItemStack activatedStack
    ) {

        ResourceLocation sourceId =
                ActivatedCrystalItem
                        .getSourceCrystalId(
                                activatedStack
                        );

        if (sourceId == null) {
            return null;
        }

        return findShardRecipe(
                createSourceStack(sourceId)
        );
    }

    private ItemStackGasToItemStackRecipe
    findShardRecipe(
            ItemStack crystal
    ) {

        if (crystal.isEmpty()) {
            return null;
        }

        if (!crystal.is(CRYSTALS_TAG)) {
            return null;
        }

        GasStack hcl =
                new GasStack(
                        MekanismGases.HYDROGEN_CHLORIDE.get(),
                        1
                );

        for (
                ItemStackGasToItemStackRecipe recipe :
                MekanismRecipeType.INJECTING
                        .get()
                        .getRecipes(null)
        ) {

            if (recipe instanceof
                    AtomicActivatedCrystalRecipe) {
                continue;
            }

            if (!recipe.test(
                    crystal,
                    hcl
            )) {
                continue;
            }

            ItemStack result =
                    recipe.getOutput(
                            crystal,
                            hcl
                    );

            if (!result.isEmpty()) {
                return recipe;
            }
        }

        return null;
    }

    private static ItemStack createSourceStack(
            ResourceLocation id
    ) {

        if (id == null ||
                !BuiltInRegistries.ITEM.containsKey(id)) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(
                BuiltInRegistries.ITEM.get(id)
        );
    }
}