package com.lucas.atomicadditions.integrations.jei;

import com.lucas.atomicadditions.AtomicAdditions;
import com.lucas.atomicadditions.recipes.AtomicAMRRecipe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.GuiDynamicHorizontalRateBar;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiGasGauge;
import mekanism.client.gui.element.gauge.GuiGauge;
import mekanism.client.jei.BaseRecipeCategory;
import mekanism.client.jei.MekanismJEI;
import mekanism.client.jei.MekanismJEIRecipeType;
import mekanism.common.MekanismLang;
import mekanism.common.lib.Color;
import mekanism.common.lib.Color.ColorFunction;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class AtomicAMRRecipeCategory
        extends BaseRecipeCategory<AtomicAMRRecipe> {

    public static final MekanismJEIRecipeType<AtomicAMRRecipe> TYPE =
            new MekanismJEIRecipeType<>(
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

        /*
         * MESMA posição da GUI REAL do AMR.
         *
         * AtomicScreen:
         * input 1 -> 7,17
         * input 2 -> 25,17
         * output  -> 169,17
         */

        input1 = addElement(
                GuiGasGauge.getDummy(
                        GaugeType.STANDARD,
                        this,
                        7,
                        17
                )
        );

        input2 = addElement(
                GuiGasGauge.getDummy(
                        GaugeType.STANDARD,
                        this,
                        25,
                        17
                )
        );

        output = addElement(
                GuiGasGauge.getDummy(
                        GaugeType.STANDARD,
                        this,
                        169,
                        17
                )
        );

        /*
         * MESMO painel central da GUI REAL do AMR.
         */
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

        /*
         * MESMA barra de progresso da GUI REAL do AMR.
         */
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
    public void setRecipe(
            @NotNull IRecipeLayoutBuilder builder,
            @NotNull AtomicAMRRecipe recipe,
            @NotNull IFocusGroup focuses
    ) {

        /*
         * EXATAMENTE como o SPS do Mekanism:
         *
         * initChemical(...)
         *
         * Isso conecta o ingrediente do JEI
         * diretamente à GuiGasGauge.
         */

        initChemical(
                builder,
                MekanismJEI.TYPE_GAS,
                RecipeIngredientRole.INPUT,
                input1,
                Collections.singletonList(
                        new mekanism.api.chemical.gas.GasStack(
                                recipe.getInput1(),
                                recipe.getInput1Amount()
                        )
                )
        );

        initChemical(
                builder,
                MekanismJEI.TYPE_GAS,
                RecipeIngredientRole.INPUT,
                input2,
                Collections.singletonList(
                        new mekanism.api.chemical.gas.GasStack(
                                recipe.getInput2(),
                                recipe.getInput2Amount()
                        )
                )
        );

        initChemical(
                builder,
                MekanismJEI.TYPE_GAS,
                RecipeIngredientRole.OUTPUT,
                output,
                Collections.singletonList(
                        new mekanism.api.chemical.gas.GasStack(
                                recipe.getOutput(),
                                recipe.getOutputAmount()
                        )
                )
        );
    }
}