package com.lucas.atomicadditions.integrations.jei;

import com.lucas.atomicadditions.AtomicAdditions;
import com.lucas.atomicadditions.chemical.AtomicGases;
import com.lucas.atomicadditions.item.ActivatedCrystalItem;
import com.lucas.atomicadditions.recipes.AtomicActivatedCrystalRecipe;
import com.lucas.atomicadditions.recipes.AtomicRecipeSerializers;
import mekanism.client.jei.BaseRecipeCategory;
import mekanism.client.jei.MekanismJEI;
import mekanism.client.jei.MekanismJEIRecipeType;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class AtomicActivatedCrystalRecipeCategory
        extends BaseRecipeCategory<AtomicActivatedCrystalRecipe> {

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
                net.minecraft.network.chat.Component.translatable(
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
                )
                .addItemStack(output)
                .setCustomRenderer(
                        VanillaTypes.ITEM_STACK,
                        new ActivatedCrystalJEIRenderer(output)
                );
    }
}