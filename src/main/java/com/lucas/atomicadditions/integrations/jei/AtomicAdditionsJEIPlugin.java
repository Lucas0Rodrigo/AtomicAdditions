package com.lucas.atomicadditions.integrations.jei;

import com.lucas.atomicadditions.AtomicAdditions;
import com.lucas.atomicadditions.recipes.AtomicRecipes;
import mekanism.client.jei.MekanismJEI;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;

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
                )
        );
    }

    @Override
    public void registerRecipes(
            IRecipeRegistration registration
    ) {

        registration.addRecipes(
                MekanismJEI.recipeType(
                        AtomicAMRRecipeCategory.TYPE
                ),
                AtomicRecipes.AMR_RECIPES
                        .getRecipesForJEI()
        );
    }
}