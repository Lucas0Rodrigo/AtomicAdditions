package com.lucas.atomicadditions.recipes;

import com.lucas.atomicadditions.AtomicAdditions;
import mekanism.common.recipe.serializer.MekanismRecipeSerializer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class AtomicRecipeSerializers {

    private AtomicRecipeSerializers() {
    }

    public enum Operation {
        ACTIVATE,
        HYDROGEN_CHLORIDE,
        RHENIUM
    }

    public static final DeferredRegister<RecipeSerializer<?>>
            RECIPE_SERIALIZERS =
            DeferredRegister.create(
                    Registries.RECIPE_SERIALIZER,
                    AtomicAdditions.MODID
            );

    public static final DeferredRegister<IngredientType<?>>
            INGREDIENT_TYPES =
            DeferredRegister.create(
                    NeoForgeRegistries.INGREDIENT_TYPES,
                    AtomicAdditions.MODID
            );

    public static final DeferredHolder<IngredientType<?>, IngredientType<PartialCustomDataIngredient>>
            PARTIAL_CUSTOM_DATA =
            INGREDIENT_TYPES.register(
                    "partial_custom_data",
                    () -> PartialCustomDataIngredient.TYPE
            );

    public static final DeferredHolder<RecipeSerializer<?>, MekanismRecipeSerializer<AtomicActivatedCrystalRecipe>>
            ACTIVATE =
            RECIPE_SERIALIZERS.register(
                    "activated_crystal",
                    () -> serializer(
                            Operation.ACTIVATE
                    )
            );

    public static final DeferredHolder<RecipeSerializer<?>, MekanismRecipeSerializer<AtomicActivatedCrystalRecipe>>
            HYDROGEN_CHLORIDE =
            RECIPE_SERIALIZERS.register(
                    "activated_crystal_hydrogen_chloride",
                    () -> serializer(
                            Operation.HYDROGEN_CHLORIDE
                    )
            );

    public static final DeferredHolder<RecipeSerializer<?>, MekanismRecipeSerializer<AtomicActivatedCrystalRecipe>>
            RHENIUM =
            RECIPE_SERIALIZERS.register(
                    "activated_crystal_rhenium",
                    () -> serializer(
                            Operation.RHENIUM
                    )
            );

    private static MekanismRecipeSerializer<AtomicActivatedCrystalRecipe>
    serializer(
            Operation operation
    ) {
        return MekanismRecipeSerializer.itemChemicalToItem(
                (itemInput, chemicalInput, output, perTickUsage) ->
                        new AtomicActivatedCrystalRecipe(
                                itemInput,
                                chemicalInput,
                                output,
                                perTickUsage,
                                operation
                        )
        );
    }
}
