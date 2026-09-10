package com.lucas.atomicadditions.recipes;

import com.lucas.atomicadditions.AtomicAdditions;
import com.lucas.atomicadditions.item.ActivatedCrystalItem;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.recipes.ItemStackGasToItemStackRecipe;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient.GasStackIngredient;
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
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

    public static AtomicActivatedCrystalRecipe
    createActivationRecipe(
            ResourceLocation id,
            ItemStack sourceCrystal
    ) {

        return new AtomicActivatedCrystalRecipe(
                id,

                /*
                 * 5 Cristais
                 */
                IngredientCreatorAccess
                        .item()
                        .from(
                                sourceCrystal,
                                5
                        ),

                /*
                 * 1 mB de Tântalo
                 */
                IngredientCreatorAccess
                        .gas()
                        .from(
                                com.lucas.atomicadditions
                                        .chemical.AtomicGases
                                        .TANTALUM,
                                1
                        ),

                /*
                 * 8 Cristais Ativados
                 */
                new ItemStack(
                        AtomicAdditions
                                .ACTIVATED_CRYSTAL
                                .get(),
                        8
                ),

                AtomicRecipeSerializers
                        .Operation
                        .ACTIVATE
        );
    }

    public AtomicRecipeSerializers.Operation
    getOperation() {
        return operation;
    }

    @Override
    public RecipeType<ItemStackGasToItemStackRecipe>
    getType() {
        return MekanismRecipeType.INJECTING.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<ItemStackGasToItemStackRecipe>
    getSerializer() {

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

        if (itemStack.isEmpty() ||
                gasStack.isEmpty()) {
            return false;
        }

        switch (operation) {

            /*
             * 5x Cristal + Tântalo
             *
             * NÃO usamos super.test() aqui porque o
             * ingrediente gerado dinamicamente é validado
             * diretamente abaixo.
             */
            case ACTIVATE:

                if (itemStack.getCount() < 5) {
                    return false;
                }

                return gasStack.getType()
                        ==
                        com.lucas.atomicadditions
                                .chemical.AtomicGases
                                .TANTALUM
                                .get();

            /*
             * Cristal Ativado + HCl
             */
            case HYDROGEN_CHLORIDE:

                if (!isActivatedCrystal(itemStack)) {
                    return false;
                }

                if (itemStack.getCount() < 1) {
                    return false;
                }

                return gasStack.getType()
                        ==
                        MekanismGases
                                .HYDROGEN_CHLORIDE
                                .get();

            /*
             * Cristal Ativado + Rênio
             */
            case RHENIUM:

                if (!isActivatedCrystal(itemStack)) {
                    return false;
                }

                if (itemStack.getCount() < 1) {
                    return false;
                }

                return gasStack.getType()
                        ==
                        com.lucas.atomicadditions
                                .chemical.AtomicGases
                                .RHENIUM
                                .get();

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

            /*
             * 5 Cristais + Tântalo
             *       ↓
             * 8 Cristais Ativados
             */
            case ACTIVATE ->
                    createActivatedCrystal(
                            inputItem
                    );

            /*
             * 1 Cristal Ativado + HCl
             *       ↓
             * 1 Shard
             */
            case HYDROGEN_CHLORIDE ->
                    createShardOutput(
                            inputItem,
                            1
                    );

            /*
             * 1 Cristal Ativado + Rênio
             *       ↓
             * 2 Shards
             */
            case RHENIUM ->
                    createShardOutput(
                            inputItem,
                            2
                    );
        };
    }

    @Override
    public List<@NotNull ItemStack>
    getOutputDefinition() {

        if (operation ==
                AtomicRecipeSerializers
                        .Operation
                        .ACTIVATE) {

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

    private ItemStack
    createActivatedCrystal(
            ItemStack sourceCrystal
    ) {

        /*
         * A quantidade final é 8,
         * independentemente da quantidade disponível
         * no slot acima do mínimo de 5.
         */
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

        ActivatedCrystalItem
                .setSourceCrystal(
                        result,
                        sourceId
                );

        return result;
    }

    private ItemStack
    createShardOutput(
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
         * Mantém somente a multiplicação definida
         * pelo caminho escolhido.
         *
         * HCl  -> 1x shard
         * Rênio -> 2x shard
         */
        result.setCount(amount);

        return result;
    }

    private boolean
    isActivatedCrystal(
            ItemStack stack
    ) {

        return stack.getItem()
                ==
                AtomicAdditions
                        .ACTIVATED_CRYSTAL
                        .get();
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
         * Aqui usamos as receitas originais do cache do
         * Mekanism porque estamos procurando a receita que
         * transforma o cristal original em shard.
         */
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

    private static ItemStack
    createSourceStack(
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