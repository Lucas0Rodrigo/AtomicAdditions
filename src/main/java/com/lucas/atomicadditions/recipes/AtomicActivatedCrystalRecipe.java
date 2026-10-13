package com.lucas.atomicadditions.recipes;

import com.lucas.atomicadditions.AtomicAdditions;
import com.lucas.atomicadditions.chemical.AtomicGases;
import com.lucas.atomicadditions.item.ActivatedCrystalItem;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.ItemStackChemicalToItemStackRecipe;
import mekanism.api.recipes.basic.BasicItemStackChemicalToItemStackRecipe;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.registries.MekanismChemicals;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class AtomicActivatedCrystalRecipe
        extends BasicItemStackChemicalToItemStackRecipe {

    private final AtomicRecipeSerializers.Operation operation;

    private static final List<RecipeHolder<AtomicActivatedCrystalRecipe>>
            GENERATED_RECIPES =
            new ArrayList<>();

    public static void clearGeneratedRecipes() {
        GENERATED_RECIPES.clear();
    }

    public static void registerGeneratedRecipe(
            RecipeHolder<AtomicActivatedCrystalRecipe> recipe
    ) {
        GENERATED_RECIPES.add(recipe);
    }

    public static List<RecipeHolder<AtomicActivatedCrystalRecipe>>
    getGeneratedRecipes() {
        return List.copyOf(
                GENERATED_RECIPES
        );
    }

    public AtomicActivatedCrystalRecipe(
            ItemStackIngredient itemInput,
            ChemicalStackIngredient chemicalInput,
            ItemStack output,
            boolean perTickUsage,
            AtomicRecipeSerializers.Operation operation
    ) {
        super(
                itemInput,
                chemicalInput,
                output,
                perTickUsage,
                MekanismRecipeType.INJECTING.get()
        );

        this.operation =
                operation;
    }

    public static AtomicActivatedCrystalRecipe
    createActivationRecipe(
            ItemStack sourceCrystal
    ) {
        ItemStack output =
                new ItemStack(
                        AtomicAdditions.ACTIVATED_CRYSTAL.get(),
                        8
                );

        ActivatedCrystalItem.setSourceCrystal(
                output,
                sourceCrystal
        );

        return new AtomicActivatedCrystalRecipe(
                IngredientCreatorAccess.item().from(
                        sourceCrystal,
                        5
                ),
                IngredientCreatorAccess.chemicalStack().fromHolder(
                        AtomicGases.TANTALUM,
                        1L
                ),
                output,
                true,
                AtomicRecipeSerializers.Operation.ACTIVATE
        );
    }

    public static AtomicActivatedCrystalRecipe
    createHclRecipe(
            ItemStack sourceCrystal,
            ItemStack shardOutput
    ) {
        ItemStack output =
                shardOutput.copy();

        output.setCount(1);

        return new AtomicActivatedCrystalRecipe(
                activatedCrystalInput(
                        sourceCrystal,
                        1
                ),
                IngredientCreatorAccess.chemicalStack().fromHolder(
                        MekanismChemicals.HYDROGEN_CHLORIDE,
                        1L
                ),
                output,
                true,
                AtomicRecipeSerializers.Operation.HYDROGEN_CHLORIDE
        );
    }

    public static AtomicActivatedCrystalRecipe
    createRheniumRecipe(
            ItemStack sourceCrystal,
            ItemStack shardOutput
    ) {
        ItemStack output =
                shardOutput.copy();

        output.setCount(16);

        return new AtomicActivatedCrystalRecipe(
                activatedCrystalInput(
                        sourceCrystal,
                        8
                ),
                IngredientCreatorAccess.chemicalStack().fromHolder(
                        AtomicGases.RHENIUM,
                        1L
                ),
                output,
                true,
                AtomicRecipeSerializers.Operation.RHENIUM
        );
    }

    private static ItemStackIngredient activatedCrystalInput(
            ItemStack sourceCrystal,
            int count
    ) {
        ResourceLocation sourceId =
                BuiltInRegistries.ITEM.getKey(
                        sourceCrystal.getItem()
                );

        CompoundTag requiredTag =
                new CompoundTag();

        requiredTag.putString(
                ActivatedCrystalItem.SOURCE_CRYSTAL_TAG,
                sourceId.toString()
        );

        return IngredientCreatorAccess.item().from(
                PartialCustomDataIngredient.of(
                        AtomicAdditions.ACTIVATED_CRYSTAL.get(),
                        requiredTag
                ).toVanilla(),
                count
        );
    }

    public AtomicRecipeSerializers.Operation getOperation() {
        return operation;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return switch (operation) {
            case ACTIVATE ->
                    AtomicRecipeSerializers.ACTIVATE.get();

            case HYDROGEN_CHLORIDE ->
                    AtomicRecipeSerializers.HYDROGEN_CHLORIDE.get();

            case RHENIUM ->
                    AtomicRecipeSerializers.RHENIUM.get();
        };
    }

    @Override
    public String getGroup() {
        return "activated_crystal";
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(
                MekanismBlocks
                        .CHEMICAL_INJECTION_CHAMBER
                        .asItem()
        );
    }

    @Override
    public boolean test(
            ItemStack itemStack,
            ChemicalStack chemicalStack
    ) {
        if (itemStack.isEmpty()
                || chemicalStack.isEmpty()) {
            return false;
        }

        return this.itemInput.test(
                itemStack
        )
                && this.chemicalInput.test(
                chemicalStack
        );
    }

    public static ItemStack findOriginalShardOutput(
            Level level,
            ItemStack sourceCrystal
    ) {
        if (level == null
                || sourceCrystal.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ChemicalStack hcl =
                MekanismChemicals.HYDROGEN_CHLORIDE.asStack(
                        1_000_000
                );

        for (
                RecipeHolder<ItemStackChemicalToItemStackRecipe> holder :
                MekanismRecipeType.INJECTING.getRecipes(level)
        ) {
            ItemStackChemicalToItemStackRecipe recipe =
                    holder.value();

            if (recipe instanceof
                    AtomicActivatedCrystalRecipe) {
                continue;
            }

            if (recipe.test(
                    sourceCrystal,
                    hcl
            )) {
                ItemStack result =
                        recipe.getOutput(
                                sourceCrystal,
                                hcl
                        );

                if (!result.isEmpty()) {
                    return result;
                }
            }
        }

        return ItemStack.EMPTY;
    }
}
