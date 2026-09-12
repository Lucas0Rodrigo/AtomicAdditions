package com.lucas.atomicadditions.recipes;

import com.lucas.atomicadditions.AtomicAdditions;
import com.lucas.atomicadditions.chemical.AtomicGases;
import com.lucas.atomicadditions.item.ActivatedCrystalItem;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.recipes.ItemStackGasToItemStackRecipe;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient.GasStackIngredient;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.registries.MekanismGases;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class AtomicActivatedCrystalRecipe extends ItemStackGasToItemStackRecipe {

    private final AtomicRecipeSerializers.Operation operation;
    private final ItemStackIngredient itemInput;
    private final GasStackIngredient gasInput;

    private static final List<AtomicActivatedCrystalRecipe> GENERATED_RECIPES = new ArrayList<>();

    public static void clearGeneratedRecipes() {
        GENERATED_RECIPES.clear();
    }

    public static void registerGeneratedRecipe(AtomicActivatedCrystalRecipe recipe) {
        GENERATED_RECIPES.add(recipe);
    }

    public static List<AtomicActivatedCrystalRecipe> getGeneratedRecipes() {
        return List.copyOf(GENERATED_RECIPES);
    }

    public AtomicActivatedCrystalRecipe(
            ResourceLocation id,
            ItemStackIngredient itemInput,
            GasStackIngredient gasInput,
            ItemStack output,
            AtomicRecipeSerializers.Operation operation
    ) {
        super(id, itemInput, gasInput, output);
        this.itemInput = itemInput;
        this.gasInput = gasInput;
        this.operation = operation;
    }

    public static AtomicActivatedCrystalRecipe createActivationRecipe(
            ResourceLocation id,
            ItemStack sourceCrystal
    ) {
        ResourceLocation sourceId = BuiltInRegistries.ITEM.getKey(sourceCrystal.getItem());

        ItemStack output = new ItemStack(AtomicAdditions.ACTIVATED_CRYSTAL.get(), 8);
        ActivatedCrystalItem.setSourceCrystal(output, sourceId);

        return new AtomicActivatedCrystalRecipe(
                id,
                IngredientCreatorAccess.item().from(sourceCrystal, 5),
                IngredientCreatorAccess.gas().from(AtomicGases.TANTALUM, 1),
                output,
                AtomicRecipeSerializers.Operation.ACTIVATE
        );
    }

    public static AtomicActivatedCrystalRecipe createHclRecipe(
            ResourceLocation id,
            ItemStack sourceCrystal,
            ItemStack shardOutput
    ) {
        ResourceLocation sourceId = BuiltInRegistries.ITEM.getKey(sourceCrystal.getItem());

        ItemStack activatedInput = new ItemStack(AtomicAdditions.ACTIVATED_CRYSTAL.get(), 1);
        ActivatedCrystalItem.setSourceCrystal(activatedInput, sourceId);

        ItemStack output = shardOutput.copy();
        output.setCount(1);

        return new AtomicActivatedCrystalRecipe(
                id,
                IngredientCreatorAccess.item().from(activatedInput),
                IngredientCreatorAccess.gas().from(MekanismGases.HYDROGEN_CHLORIDE, 1),
                output,
                AtomicRecipeSerializers.Operation.HYDROGEN_CHLORIDE
        );
    }

    public static AtomicActivatedCrystalRecipe createRheniumRecipe(
            ResourceLocation id,
            ItemStack sourceCrystal,
            ItemStack shardOutput
    ) {
        ResourceLocation sourceId = BuiltInRegistries.ITEM.getKey(sourceCrystal.getItem());

        ItemStack activatedInput = new ItemStack(AtomicAdditions.ACTIVATED_CRYSTAL.get(), 8);
        ActivatedCrystalItem.setSourceCrystal(activatedInput, sourceId);

        ItemStack output = shardOutput.copy();
        output.setCount(16);

        return new AtomicActivatedCrystalRecipe(
                id,
                IngredientCreatorAccess.item().from(activatedInput),
                IngredientCreatorAccess.gas().from(AtomicGases.RHENIUM, 1),
                output,
                AtomicRecipeSerializers.Operation.RHENIUM
        );
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
        RecipeSerializer<?> serializer = switch (operation) {
            case ACTIVATE -> AtomicRecipeSerializers.ACTIVATE.get();
            case HYDROGEN_CHLORIDE -> AtomicRecipeSerializers.HYDROGEN_CHLORIDE.get();
            case RHENIUM -> AtomicRecipeSerializers.RHENIUM.get();
        };

        return (RecipeSerializer<ItemStackGasToItemStackRecipe>) (RecipeSerializer<?>) serializer;
    }

    @Override
    public String getGroup() {
        return "activated_crystal";
    }

    @Override
    public ItemStack getToastSymbol() {
        return MekanismBlocks.CHEMICAL_INJECTION_CHAMBER.getItemStack();
    }

    @Override
    public boolean test(ItemStack itemStack, GasStack gasStack) {
        if (itemStack.isEmpty() || gasStack.isEmpty()) {
            return false;
        }

        boolean itemMatches = this.itemInput.test(itemStack);
        boolean gasMatches = this.gasInput.test(gasStack);

        return itemMatches && gasMatches;
    }

    @Override
    public ItemStack getOutput(ItemStack inputItem, GasStack inputGas) {
        List<ItemStack> outputs = getOutputDefinition();

        if (outputs.isEmpty()) {
            return ItemStack.EMPTY;
        }

        return outputs.get(0).copy();
    }

    public static ItemStack findOriginalShardOutput(Level level, ItemStack sourceCrystal) {
        if (level == null || sourceCrystal.isEmpty()) {
            return ItemStack.EMPTY;
        }

        GasStack hcl = new GasStack(
                MekanismGases.HYDROGEN_CHLORIDE.get(),
                1_000_000
        );

        List<ItemStackGasToItemStackRecipe> injectingRecipes =
                MekanismRecipeType.INJECTING.get().getRecipes(level);

        for (ItemStackGasToItemStackRecipe recipe : injectingRecipes) {
            if (recipe instanceof AtomicActivatedCrystalRecipe) {
                continue;
            }

            if (recipe.test(sourceCrystal, hcl)) {
                ItemStack result = recipe.getOutput(sourceCrystal, hcl);

                if (!result.isEmpty()) {
                    return result;
                }
            }
        }

        return ItemStack.EMPTY;
    }
}