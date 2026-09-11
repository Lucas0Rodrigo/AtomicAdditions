package com.lucas.atomicadditions.integrations.jei;

import com.lucas.atomicadditions.AtomicAdditions;
import com.lucas.atomicadditions.item.ActivatedCrystalItem;
import com.lucas.atomicadditions.recipes.AtomicActivatedCrystalRecipe;
import com.lucas.atomicadditions.recipes.AtomicRecipes;
import mekanism.client.jei.MekanismJEI;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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
    public void registerItemSubtypes(
            ISubtypeRegistration registration
    ) {
        registration.registerSubtypeInterpreter(
                VanillaTypes.ITEM_STACK,
                AtomicAdditions.ACTIVATED_CRYSTAL.get(),
                new IIngredientSubtypeInterpreter<ItemStack>() {

                    @Override
                    public String apply(ItemStack ingredient, UidContext context) {
                        return "";
                    }

                    public @Nullable Object getSubtypeData(
                            ItemStack ingredient,
                            UidContext context
                    ) {
                        CompoundTag tag =
                                ingredient.getTag();

                        if (tag == null ||
                                !tag.contains(
                                        ActivatedCrystalItem
                                                .SOURCE_CRYSTAL_TAG
                                )) {
                            return null;
                        }

                        return tag.getString(
                                ActivatedCrystalItem
                                        .SOURCE_CRYSTAL_TAG
                        );
                    }

                    public String getLegacyStringSubtypeInfo(
                            ItemStack ingredient,
                            UidContext context
                    ) {
                        CompoundTag tag =
                                ingredient.getTag();

                        if (tag == null ||
                                !tag.contains(
                                        ActivatedCrystalItem
                                                .SOURCE_CRYSTAL_TAG
                                )) {
                            return "";
                        }

                        return tag.getString(
                                ActivatedCrystalItem
                                        .SOURCE_CRYSTAL_TAG
                        );
                    }
                }
        );
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
        registration.addRecipes(
                MekanismJEI.recipeType(
                        AtomicAMRRecipeCategory.TYPE
                ),
                AtomicRecipes.AMR_RECIPES
                        .getRecipesForJEI()
        );

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

    @Override
    public void onRuntimeAvailable(
            IJeiRuntime runtime
    ) {
        List<ItemStack> activatedCrystals =
                new ArrayList<>();

        for (
                AtomicActivatedCrystalRecipe recipe :
                AtomicActivatedCrystalRecipe
                        .getGeneratedRecipes()
        ) {
            if (recipe.getOperation() !=
                    com.lucas.atomicadditions.recipes.AtomicRecipeSerializers
                            .Operation.ACTIVATE) {
                continue;
            }

            if (recipe.getItemInput()
                    .getRepresentations()
                    .isEmpty()) {
                continue;
            }

            ItemStack output =
                    recipe.getOutputDefinition()
                            .get(0)
                            .copy();

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

            activatedCrystals.add(output);
        }

        if (!activatedCrystals.isEmpty()) {
            runtime.getIngredientManager()
                    .addIngredientsAtRuntime(
                            VanillaTypes.ITEM_STACK,
                            activatedCrystals
                    );
        }
    }
}