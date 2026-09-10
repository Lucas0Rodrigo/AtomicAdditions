package com.lucas.atomicadditions.recipes;

import com.lucas.atomicadditions.AtomicAdditions;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.recipes.ItemStackGasToItemStackRecipe;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient.GasStackIngredient;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.common.recipe.ingredient.chemical.ChemicalIngredientDeserializer;
import mekanism.common.recipe.serializer.ItemStackGasToItemStackRecipeSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

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
                    ForgeRegistries.RECIPE_SERIALIZERS,
                    AtomicAdditions.MODID
            );

    public static final RegistryObject<RecipeSerializer<?>>
            ACTIVATE =
            RECIPE_SERIALIZERS.register(
                    "activated_crystal",
                    () -> new Serializer(
                            Operation.ACTIVATE
                    )
            );

    public static final RegistryObject<RecipeSerializer<?>>
            HYDROGEN_CHLORIDE =
            RECIPE_SERIALIZERS.register(
                    "activated_crystal_hydrogen_chloride",
                    () -> new Serializer(
                            Operation.HYDROGEN_CHLORIDE
                    )
            );

    public static final RegistryObject<RecipeSerializer<?>>
            RHENIUM =
            RECIPE_SERIALIZERS.register(
                    "activated_crystal_rhenium",
                    () -> new Serializer(
                            Operation.RHENIUM
                    )
            );

    private static final class Serializer
            extends ItemStackGasToItemStackRecipeSerializer<
            AtomicActivatedCrystalRecipe
            > {

        private final Operation operation;

        private Serializer(
                Operation operation
        ) {
            super(
                    (
                            ResourceLocation id,
                            ItemStackIngredient itemInput,
                            GasStackIngredient gasInput,
                            ItemStack output
                    ) ->
                            new AtomicActivatedCrystalRecipe(
                                    id,
                                    itemInput,
                                    gasInput,
                                    output,
                                    operation
                            )
            );

            this.operation = operation;
        }

        @Override
        protected ChemicalIngredientDeserializer<
                Gas,
                GasStack,
                GasStackIngredient
                > getDeserializer() {

            return ChemicalIngredientDeserializer.GAS;
        }
    }
}