package com.lucas.atomicadditions.recipes;

import com.lucas.atomicadditions.AtomicAdditions;
import com.lucas.atomicadditions.chemical.AtomicGases;
import com.lucas.atomicadditions.item.ActivatedCrystalItem;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.recipes.ItemStackGasToItemStackRecipe;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.registries.MekanismGases;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

        List<Recipe<?>> finalRecipes =
                new ArrayList<>();

        /*
         * Mantém todas as receitas normais e remove somente
         * as receitas geradas anteriormente pelo Atomic Additions.
         */
        for (Recipe<?> recipe : existingRecipes) {

            ResourceLocation id =
                    recipe.getId();

            if (isGeneratedRecipe(id)) {
                continue;
            }

            finalRecipes.add(recipe);
        }

        List<ItemStackGasToItemStackRecipe> injectingRecipes =
                new ArrayList<>(
                        MekanismRecipeType.INJECTING
                                .get()
                                .getRecipes(null)
                );

        /*
         * Descobre quais cristais realmente possuem uma etapa:
         *
         * Cristal + HCl -> Shard
         *
         * e depois:
         *
         * Shard + O2 -> Clump
         *
         * Isso evita habilitar itens que apenas estejam
         * relacionados a algum recipe de injecting.
         */
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

            GasStack hcl =
                    new GasStack(
                            MekanismGases
                                    .HYDROGEN_CHLORIDE
                                    .get(),
                            1_000_000
                    );

            for (ItemStack crystal :
                    representations) {

                if (crystal.isEmpty()) {
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

                if (!hasPurificationRecipe(shard)) {
                    continue;
                }

                ResourceLocation sourceId =
                        net.minecraft.core.registries
                                .BuiltInRegistries.ITEM
                                .getKey(
                                        crystal.getItem()
                                );

                if (sourceId == null) {
                    continue;
                }

                ResourceLocation recipeId =
                        createGeneratedId(
                                sourceId
                        );

                AtomicActivatedCrystalRecipe
                        activatedRecipe =
                        AtomicActivatedCrystalRecipe
                                .createActivationRecipe(
                                        recipeId,
                                        crystal
                                );

                finalRecipes.add(
                        activatedRecipe
                );
            }
        }

        recipeManager.replaceRecipes(
                finalRecipes
        );

        /*
         * Muito importante:
         * o Mekanism possui caches próprios para os recipe types.
         * Limpamos todos depois de substituir o RecipeManager.
         */
        MekanismRecipeType.clearCache();
    }

    private static boolean
    hasPurificationRecipe(
            ItemStack shard
    ) {

        GasStack oxygen =
                new GasStack(
                        MekanismGases.OXYGEN.get(),
                        1_000_000
                );

        for (
                ItemStackGasToItemStackRecipe
                        purificationRecipe :
                MekanismRecipeType.PURIFYING
                        .get()
                        .getRecipes(null)
        ) {

            if (!purificationRecipe.test(
                    shard,
                    oxygen
            )) {
                continue;
            }

            ItemStack output =
                    purificationRecipe.getOutput(
                            shard,
                            oxygen
                    );

            if (!output.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private static ResourceLocation
    createGeneratedId(
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

    private static boolean
    isGeneratedRecipe(
            ResourceLocation id
    ) {

        return AtomicAdditions.MODID.equals(
                id.getNamespace()
        )
                &&
                id.getPath()
                        .startsWith(
                                GENERATED_PATH
                        );
    }
}