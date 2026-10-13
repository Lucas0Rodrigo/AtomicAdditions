package com.lucas.atomicadditions.integrations.jei;

import com.lucas.atomicadditions.AtomicAdditions;
import com.lucas.atomicadditions.recipes.AtomicAMRRecipe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.GuiDynamicHorizontalRateBar;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiChemicalGauge;
import mekanism.client.gui.element.gauge.GuiGauge;
import mekanism.client.recipe_viewer.jei.BaseRecipeCategory;
import mekanism.common.MekanismLang;
import mekanism.common.lib.Color;
import mekanism.common.lib.Color.ColorFunction;
import com.mojang.serialization.Codec;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.ICodecHelper;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class AtomicAMRRecipeCategory
        extends BaseRecipeCategory<AtomicAMRRecipe> {

    public static final RecipeType<AtomicAMRRecipe> TYPE =
            new RecipeType<>(
                    ResourceLocation.fromNamespaceAndPath(
                            AtomicAdditions.MODID,
                            "amr"
                    ),
                    AtomicAMRRecipe.class
            );

    private final GuiGauge<?> input1;
    private final GuiGauge<?> input2;
    private final GuiGauge<?> output;

    private AtomicAMRRecipe currentRecipe;

    public AtomicAMRRecipeCategory(IGuiHelper helper) {
        super(
                helper,
                TYPE,
                Component.translatable(
                        "jei.atomicadditions.amr"
                ),
                helper.createDrawableIngredient(
                        mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
                        AtomicAdditions.CASING_ITEM.get().getDefaultInstance()
                ),
                0,
                0,
                194,
                96
        );

        input1 = addElement(
                GuiChemicalGauge.getDummy(
                        GaugeType.STANDARD,
                        this,
                        7,
                        17
                )
        );

        input2 = addElement(
                GuiChemicalGauge.getDummy(
                        GaugeType.STANDARD,
                        this,
                        25,
                        17
                )
        );

        output = addElement(
                GuiChemicalGauge.getDummy(
                        GaugeType.STANDARD,
                        this,
                        169,
                        17
                )
        );

        addElement(
                new GuiInnerScreen(
                        this,
                        45,
                        17,
                        122,
                        60,
                        () -> {

                            List<Component> list =
                                    new ArrayList<>();

                            list.add(
                                    MekanismLang.STATUS.translate(
                                            MekanismLang.ACTIVE
                                    )
                            );

                            if (currentRecipe != null) {

                                list.add(
                                        Component.translatable(
                                                "jei.atomicadditions.energy",
                                                currentRecipe.getEnergyPerTick()
                                        )
                                );

                                list.add(
                                        Component.translatable(
                                                "jei.atomicadditions.duration",
                                                currentRecipe.getDuration()
                                        )
                                );

                                double rate =
                                        currentRecipe.getOutputAmount()
                                                / (double) currentRecipe.getDuration();

                                list.add(
                                        Component.translatable(
                                                "jei.atomicadditions.rate",
                                                String.format(
                                                        java.util.Locale.ROOT,
                                                        "%.2f",
                                                        rate
                                                )
                                        )
                                );
                            }

                            return list;
                        }
                )
        );

        addElement(
                new GuiDynamicHorizontalRateBar(
                        this,
                        getBarProgressTimer(),
                        7,
                        79,
                        178,
                        ColorFunction.scale(
                                Color.rgbi(60, 45, 74),
                                Color.rgbi(100, 30, 170)
                        )
                )
        );
    }

    @Override
    public void draw(
            @NotNull AtomicAMRRecipe recipe,
            @NotNull mezz.jei.api.gui.ingredient.IRecipeSlotsView recipeSlotsView,
            @NotNull GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        currentRecipe = recipe;

        super.draw(
                recipe,
                recipeSlotsView,
                guiGraphics,
                mouseX,
                mouseY
        );
    }

    @Override
    public ResourceLocation getRegistryName(
            @NotNull AtomicAMRRecipe recipe
    ) {
        return recipe.getRegistryName();
    }

    @Override
    public Codec<AtomicAMRRecipe> getCodec(
            @NotNull ICodecHelper codecHelper,
            @NotNull IRecipeManager recipeManager
    ) {
        return AtomicAMRRecipe.CODEC;
    }

    @Override
    public void setRecipe(
            @NotNull IRecipeLayoutBuilder builder,
            @NotNull AtomicAMRRecipe recipe,
            @NotNull IFocusGroup focuses
    ) {

        initChemical(
                builder,
                RecipeIngredientRole.INPUT,
                input1,
                Collections.singletonList(
                        recipe.getInput1().getStack(
                                recipe.getInput1Amount()
                        )
                )
        );

        initChemical(
                builder,
                RecipeIngredientRole.INPUT,
                input2,
                Collections.singletonList(
                        recipe.getInput2().getStack(
                                recipe.getInput2Amount()
                        )
                )
        );

        initChemical(
                builder,
                RecipeIngredientRole.OUTPUT,
                output,
                Collections.singletonList(
                        recipe.getOutput().getStack(
                                recipe.getOutputAmount()
                        )
                )
        );
    }
}