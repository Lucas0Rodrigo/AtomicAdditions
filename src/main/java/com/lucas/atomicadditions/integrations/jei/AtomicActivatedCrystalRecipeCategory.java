package com.lucas.atomicadditions.integrations.jei;

import com.lucas.atomicadditions.AtomicAdditions;
import com.lucas.atomicadditions.item.ActivatedCrystalItem;
import com.lucas.atomicadditions.recipes.AtomicActivatedCrystalRecipe;
import com.lucas.atomicadditions.recipes.AtomicRecipeSerializers;
import mekanism.client.MekanismClient;
import mekanism.client.jei.BaseRecipeCategory;
import mekanism.client.jei.MekanismJEI;
import mekanism.client.jei.MekanismJEIRecipeType;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class AtomicActivatedCrystalRecipeCategory
        extends BaseRecipeCategory<AtomicActivatedCrystalRecipe> {

    private static final ResourceLocation AURA_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    AtomicAdditions.MODID,
                    "textures/item/crystal_overlay.png"
            );

    public static final MekanismJEIRecipeType<AtomicActivatedCrystalRecipe> TYPE =
            new MekanismJEIRecipeType<>(
                    ResourceLocation.fromNamespaceAndPath(
                            AtomicAdditions.MODID,
                            "activated_crystal"
                    ),
                    AtomicActivatedCrystalRecipe.class
            );

    public AtomicActivatedCrystalRecipeCategory(
            IGuiHelper helper
    ) {
        super(
                helper,
                TYPE,
                Component.translatable(
                        "jei.atomicadditions.activated_crystal"
                ),
                helper.createDrawableIngredient(
                        VanillaTypes.ITEM_STACK,
                        AtomicAdditions.ACTIVATED_CRYSTAL
                                .get()
                                .getDefaultInstance()
                ),
                0,
                0,
                176,
                86
        );
    }

    @Override
    public void setRecipe(
            @NotNull IRecipeLayoutBuilder builder,
            @NotNull AtomicActivatedCrystalRecipe recipe,
            @NotNull IFocusGroup focuses
    ) {
        builder.addSlot(
                RecipeIngredientRole.INPUT,
                24,
                34
        ).addIngredients(
                VanillaTypes.ITEM_STACK,
                recipe.getItemInput()
                        .getRepresentations()
                        .stream()
                        .map(stack -> {
                            ItemStack display =
                                    stack.copy();

                            display.setCount(
                                    recipe.getItemInput()
                                            .getRepresentations()
                                            .get(0)
                                            .getCount()
                            );

                            return display;
                        })
                        .toList()
        );

        builder.addSlot(
                RecipeIngredientRole.INPUT,
                72,
                34
        ).addIngredients(
                MekanismJEI.TYPE_GAS,
                java.util.List.of(
                        recipe.getChemicalInput()
                                .getRepresentations()
                                .get(0)
                )
        );

        ItemStack output =
                recipe.getOutputDefinition()
                        .get(0)
                        .copy();

        if (recipe.getOperation() ==
                AtomicRecipeSerializers.Operation.ACTIVATE) {

            if (!recipe.getItemInput()
                    .getRepresentations()
                    .isEmpty()) {

                ResourceLocation sourceId =
                        net.minecraft.core.registries
                                .BuiltInRegistries.ITEM
                                .getKey(
                                        recipe.getItemInput()
                                                .getRepresentations()
                                                .get(0)
                                                .getItem()
                                );

                ActivatedCrystalItem.setSourceCrystal(
                        output,
                        sourceId
                );
            }
        }

        builder.addSlot(
                RecipeIngredientRole.OUTPUT,
                128,
                34
        ).addItemStack(output);
    }

    @Override
    public void draw(
            @NotNull AtomicActivatedCrystalRecipe recipe,
            @NotNull IRecipeSlotsView recipeSlotsView,
            @NotNull GuiGraphics guiGraphics,
            double mouseX,
            double mouseY
    ) {
        if (recipe.getItemInput()
                .getRepresentations()
                .isEmpty()) {
            return;
        }

        ItemStack sourceStack =
                recipe.getItemInput()
                        .getRepresentations()
                        .get(0)
                        .copy();

        sourceStack.setCount(1);

        guiGraphics.renderFakeItem(
                sourceStack,
                128,
                34
        );

        guiGraphics.blit(
                AURA_TEXTURE,
                128,
                34,
                0,
                0,
                16,
                16,
                16,
                16
        );
    }
}