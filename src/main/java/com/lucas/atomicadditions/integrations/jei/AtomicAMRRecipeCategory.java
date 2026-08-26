package com.lucas.atomicadditions.integrations.jei;

import com.lucas.atomicadditions.AtomicAdditions;
import com.lucas.atomicadditions.recipes.AtomicAMRRecipe;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mekanism.api.chemical.gas.GasStack;
import mekanism.client.jei.MekanismJEI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
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

    public AtomicAMRRecipeCategory() {
        background = new AMRBackground();
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
                17,
                18
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
                17,
                49
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
                137,
                34
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

    private static class AMRBackground implements IDrawable {

        private static final int WIDTH = 176;
        private static final int HEIGHT = 80;

        @Override
        public int getWidth() {
            return WIDTH;
        }

        @Override
        public int getHeight() {
            return HEIGHT;
        }

        @Override
        public void draw(
                GuiGraphics guiGraphics,
                int xOffset,
                int yOffset
        ) {
            int x = xOffset;
            int y = yOffset;

            /*
             * Fundo da interface
             */
            guiGraphics.fill(
                    x,
                    y,
                    x + WIDTH,
                    y + HEIGHT,
                    0xFF202020
            );

            /*
             * Borda externa
             */
            drawBorder(
                    guiGraphics,
                    x,
                    y,
                    WIDTH,
                    HEIGHT,
                    0xFF555555
            );

            /*
             * Painel central
             */
            guiGraphics.fill(
                    x + 43,
                    y + 8,
                    x + 133,
                    y + 67,
                    0xFF101010
            );

            drawBorder(
                    guiGraphics,
                    x + 43,
                    y + 8,
                    90,
                    59,
                    0xFF454545
            );

            /*
             * Tanque de entrada 1
             */
            drawTank(
                    guiGraphics,
                    x + 6,
                    y + 14,
                    28,
                    24
            );

            /*
             * Tanque de entrada 2
             */
            drawTank(
                    guiGraphics,
                    x + 6,
                    y + 45,
                    28,
                    24
            );

            /*
             * Tanque de saída
             */
            drawTank(
                    guiGraphics,
                    x + 142,
                    y + 29,
                    28,
                    24
            );

            /*
             * Linha entrada 1
             */
            guiGraphics.fill(
                    x + 34,
                    y + 25,
                    x + 55,
                    y + 27,
                    0xFF777777
            );

            /*
             * Linha entrada 2
             */
            guiGraphics.fill(
                    x + 34,
                    y + 56,
                    x + 55,
                    y + 58,
                    0xFF777777
            );

            /*
             * Linha saída
             */
            guiGraphics.fill(
                    x + 121,
                    y + 40,
                    x + 142,
                    y + 42,
                    0xFF777777
            );

            /*
             * Setas
             */
            drawArrow(
                    guiGraphics,
                    x + 52,
                    y + 26
            );

            drawArrow(
                    guiGraphics,
                    x + 52,
                    y + 57
            );

            drawArrow(
                    guiGraphics,
                    x + 139,
                    y + 41
            );

            /*
             * Núcleo externo
             */
            guiGraphics.fill(
                    x + 63,
                    y + 25,
                    x + 113,
                    y + 56,
                    0xFF29152F
            );

            drawBorder(
                    guiGraphics,
                    x + 63,
                    y + 25,
                    50,
                    31,
                    0xFF71438A
            );

            /*
             * Núcleo interno
             */
            guiGraphics.fill(
                    x + 75,
                    y + 32,
                    x + 101,
                    y + 49,
                    0xFF4C2364
            );

            drawBorder(
                    guiGraphics,
                    x + 75,
                    y + 32,
                    26,
                    17,
                    0xFF9863B5
            );

            /*
             * Indicador central
             */
            guiGraphics.fill(
                    x + 83,
                    y + 37,
                    x + 93,
                    y + 44,
                    0xFFB17BC9
            );

            /*
             * Barra de progresso
             */
            guiGraphics.fill(
                    x + 6,
                    y + 72,
                    x + 170,
                    y + 76,
                    0xFF111111
            );

            guiGraphics.fill(
                    x + 7,
                    y + 73,
                    x + 169,
                    y + 75,
                    0xFF6D378A
            );

            /*
             * Texto de energia/duração
             *
             * A categoria não possui um recipe atualmente
             * armazenado no background, portanto essas
             * informações serão adicionadas posteriormente
             * através de widgets próprios do JEI.
             */
        }

        private void drawTank(
                GuiGraphics guiGraphics,
                int x,
                int y,
                int width,
                int height
        ) {
            guiGraphics.fill(
                    x,
                    y,
                    x + width,
                    y + height,
                    0xFF111111
            );

            drawBorder(
                    guiGraphics,
                    x,
                    y,
                    width,
                    height,
                    0xFF555555
            );

            guiGraphics.fill(
                    x + 3,
                    y + 3,
                    x + width - 3,
                    y + height - 3,
                    0xFF272033
            );
        }

        private void drawBorder(
                GuiGraphics guiGraphics,
                int x,
                int y,
                int width,
                int height,
                int color
        ) {
            guiGraphics.fill(
                    x,
                    y,
                    x + width,
                    y + 1,
                    color
            );

            guiGraphics.fill(
                    x,
                    y + height - 1,
                    x + width,
                    y + height,
                    color
            );

            guiGraphics.fill(
                    x,
                    y,
                    x + 1,
                    y + height,
                    color
            );

            guiGraphics.fill(
                    x + width - 1,
                    y,
                    x + width,
                    y + height,
                    color
            );
        }

        private void drawArrow(
                GuiGraphics guiGraphics,
                int x,
                int y
        ) {
            guiGraphics.fill(
                    x,
                    y - 2,
                    x + 5,
                    y + 2,
                    0xFFAAAAAA
            );

            guiGraphics.fill(
                    x + 3,
                    y - 4,
                    x + 7,
                    y + 4,
                    0xFFAAAAAA
            );
        }
    }
}