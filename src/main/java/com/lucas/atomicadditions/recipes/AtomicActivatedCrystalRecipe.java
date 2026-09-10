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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class AtomicActivatedCrystalRecipe
        extends ItemStackGasToItemStackRecipe {

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

    /*
     * 5x Cristal
     * +
     * 1 mB Tântalo
     * =
     * 8x Cristal Ativado
     */
    public static AtomicActivatedCrystalRecipe createActivationRecipe(
            ResourceLocation id,
            ItemStack sourceCrystal
    ) {
        return new AtomicActivatedCrystalRecipe(
                id,

                IngredientCreatorAccess
                        .item()
                        .from(
                                sourceCrystal,
                                5
                        ),

                IngredientCreatorAccess
                        .gas()
                        .from(
                                AtomicGases.TANTALUM,
                                1
                        ),

                new ItemStack(
                        AtomicAdditions
                                .ACTIVATED_CRYSTAL
                                .get(),
                        8
                ),

                AtomicRecipeSerializers.Operation.ACTIVATE
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

        RecipeSerializer<?> serializer =
                switch (operation) {

                    case ACTIVATE ->
                            AtomicRecipeSerializers
                                    .ACTIVATE
                                    .get();

                    case HYDROGEN_CHLORIDE ->
                            AtomicRecipeSerializers
                                    .HYDROGEN_CHLORIDE
                                    .get();

                    case RHENIUM ->
                            AtomicRecipeSerializers
                                    .RHENIUM
                                    .get();
                };

        return (RecipeSerializer<ItemStackGasToItemStackRecipe>)
                (RecipeSerializer<?>) serializer;
    }

    @Override
    public String getGroup() {
        return "activated_crystal";
    }

    @Override
    public ItemStack getToastSymbol() {
        return MekanismBlocks
                .CHEMICAL_INJECTION_CHAMBER
                .getItemStack();
    }

    @Override
    public boolean test(
            ItemStack itemStack,
            GasStack gasStack
    ) {

        if (itemStack.isEmpty() || gasStack.isEmpty()) {
            return false;
        }

        switch (operation) {

            /*
             * 5 Cristais + Tântalo
             */
            case ACTIVATE:

                if (itemStack.getCount() < 5) {
                    return false;
                }

                /*
                 * O próprio recipe foi criado para um cristal
                 * específico. Então conferimos a representação
                 * diretamente.
                 */
                boolean correctCrystal = false;

                for (ItemStack representation :
                        getItemInput().getRepresentations()) {

                    if (representation.isEmpty()) {
                        continue;
                    }

                    if (ItemStack.isSameItem(
                            representation,
                            itemStack
                    )) {
                        correctCrystal = true;
                        break;
                    }
                }

                if (!correctCrystal) {
                    return false;
                }

                /*
                 * Comparação nativa da API do Mekanism.
                 */
                return gasStack.isTypeEqual(
                        AtomicGases.TANTALUM.get()
                );

            /*
             * 1 Cristal Ativado + HCl
             */
            case HYDROGEN_CHLORIDE:

                return isActivatedCrystal(itemStack)
                        &&
                        gasStack.isTypeEqual(
                                MekanismGases
                                        .HYDROGEN_CHLORIDE
                                        .get()
                        );

            /*
             * 1 Cristal Ativado + Rênio
             *
             * Será convertido em 2 Shards.
             */
            case RHENIUM:

                return isActivatedCrystal(itemStack)
                        &&
                        gasStack.isTypeEqual(
                                AtomicGases.RHENIUM.get()
                        );

            default:
                return false;
        }
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
                            1
                    );

            case RHENIUM ->
                    createShardOutput(
                            inputItem,
                            2
                    );
        };
    }

    @Override
    public List<@NotNull ItemStack> getOutputDefinition() {

        if (operation ==
                AtomicRecipeSerializers.Operation.ACTIVATE) {

            return Collections.singletonList(
                    new ItemStack(
                            AtomicAdditions
                                    .ACTIVATED_CRYSTAL
                                    .get(),
                            8
                    )
            );
        }

        return super.getOutputDefinition();
    }

    private ItemStack createActivatedCrystal(
            ItemStack sourceCrystal
    ) {

        ItemStack result =
                new ItemStack(
                        AtomicAdditions
                                .ACTIVATED_CRYSTAL
                                .get(),
                        8
                );

        ResourceLocation sourceId =
                net.minecraft.core.registries
                        .BuiltInRegistries.ITEM
                        .getKey(
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
                createSourceStack(
                        sourceId
                );

        if (source.isEmpty()) {
            return ItemStack.EMPTY;
        }

        GasStack hcl =
                new GasStack(
                        MekanismGases
                                .HYDROGEN_CHLORIDE
                                .get(),
                        1_000_000
                );

        ItemStack result =
                recipe.getOutput(
                        source,
                        hcl
                );

        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }

        /*
         * HCl:
         * 1x Activated Crystal -> 1x Shard
         *
         * Rênio:
         * 1x Activated Crystal -> 2x Shards
         */
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

    private ItemStackGasToItemStackRecipe findOriginalShardRecipe(
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

        ItemStack source =
                createSourceStack(
                        sourceId
                );

        if (source.isEmpty()) {
            return null;
        }

        GasStack hcl =
                new GasStack(
                        MekanismGases
                                .HYDROGEN_CHLORIDE
                                .get(),
                        1_000_000
                );

        /*
         * Aqui procuramos somente a receita original
         * que transforma o cristal em shard.
         *
         * A consulta usa o cache do Mekanism porque este
         * método só é chamado depois que a receita já foi
         * efetivamente selecionada pela máquina.
         */
        for (
                ItemStackGasToItemStackRecipe recipe :
                MekanismRecipeType.INJECTING
                        .get()
                        .getRecipes(null)
        ) {

            /*
             * Nunca usar as nossas próprias receitas.
             */
            if (recipe instanceof
                    AtomicActivatedCrystalRecipe) {
                continue;
            }

            if (!recipe.test(
                    source,
                    hcl
            )) {
                continue;
            }

            ItemStack result =
                    recipe.getOutput(
                            source,
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
                !net.minecraft.core.registries
                        .BuiltInRegistries.ITEM
                        .containsKey(id)) {

            return ItemStack.EMPTY;
        }

        return new ItemStack(
                net.minecraft.core.registries
                        .BuiltInRegistries.ITEM
                        .get(id)
        );
    }
}