package com.lucas.atomicadditions.integrations.jei;

import com.lucas.atomicadditions.AtomicAdditions;
import com.lucas.atomicadditions.recipes.AtomicAMRRecipe;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mekanism.api.chemical.gas.GasStack;
import mekanism.client.jei.MekanismJEI;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class AtomicAMRRecipeCategory
        implements IRecipeCategory<AtomicAMRRecipe> {

    public static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(
                    AtomicAdditions.MODID,
                    "amr"
            );

    public static final RecipeType<AtomicAMRRecipe> TYPE =
            RecipeType.create(
                    AtomicAdditions.MODID,
                    "amr",
                    AtomicAMRRecipe.class
            );

    private final IDrawable background;

    public AtomicAMRRecipeCategory(
            IGuiHelper guiHelper
    ) {
        background = guiHelper.createBlankDrawable(
                176,
                80
        );
    }

    @Override
    public RecipeType<AtomicAMRRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable(
                "jei.atomicadditions.amr"
        );
    }

    @SuppressWarnings("removal")
    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return null;
    }

    @Override
    public void setRecipe(
            IRecipeLayoutBuilder builder,
            AtomicAMRRecipe recipe,
            IFocusGroup focuses
    ) {
        builder.addSlot(
                RecipeIngredientRole.INPUT,
                25,
                20
        ).addIngredients(
                MekanismJEI.TYPE_GAS,
                List.of(
                        new GasStack(
                                recipe.getInput1(),
                                recipe.getInput1Amount()
                        )
                )
        );

        builder.addSlot(
                RecipeIngredientRole.INPUT,
                25,
                50
        ).addIngredients(
                MekanismJEI.TYPE_GAS,
                List.of(
                        new GasStack(
                                recipe.getInput2(),
                                recipe.getInput2Amount()
                        )
                )
        );

        builder.addSlot(
                RecipeIngredientRole.OUTPUT,
                125,
                35
        ).addIngredients(
                MekanismJEI.TYPE_GAS,
                List.of(
                        new GasStack(
                                recipe.getOutput(),
                                recipe.getOutputAmount()
                        )
                )
        );
    }
}