package com.lucas.atomicadditions.integrations.jei;

import com.lucas.atomicadditions.AtomicAdditions;
import com.lucas.atomicadditions.recipes.AtomicActivatedCrystalRecipe;
import com.lucas.atomicadditions.recipes.AtomicRecipes;
import mekanism.client.jei.MekanismJEI;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

@JeiPlugin
public class AtomicAdditionsJEIPlugin implements IModPlugin {

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(
                    AtomicAdditions.MODID,
                    "jei_plugin"
            );

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(
            IRecipeCategoryRegistration registration
    ) {
        IGuiHelper guiHelper =
                registration
                        .getJeiHelpers()
                        .getGuiHelper();

        registration.addRecipeCategories(
                new AtomicAMRRecipeCategory(
                        guiHelper
                ),

                new AtomicActivatedCrystalRecipeCategory(
                        guiHelper
                )
        );
    }

    @Override
    public void registerRecipes(
            IRecipeRegistration registration
    ) {
        /*
         * =========================================================
         * AMR
         * =========================================================
         */

        registration.addRecipes(
                MekanismJEI.recipeType(
                        AtomicAMRRecipeCategory.TYPE
                ),
                AtomicRecipes.AMR_RECIPES
                        .getRecipesForJEI()
        );

        /*
         * =========================================================
         * ACTIVATED CRYSTAL
         * =========================================================
         *
         * NÃO existe lista manual de cristais.
         *
         * Pegamos diretamente todas as receitas geradas
         * dinamicamente pelo injector.
         */

        List<AtomicActivatedCrystalRecipe>
                activatedRecipes =
                AtomicActivatedCrystalRecipe
                        .getGeneratedRecipes();

        registration.addRecipes(
                MekanismJEI.recipeType(
                        AtomicActivatedCrystalRecipeCategory.TYPE
                ),
                activatedRecipes
        );
    }
}