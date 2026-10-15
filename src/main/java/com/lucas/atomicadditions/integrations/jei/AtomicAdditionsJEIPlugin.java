package com.lucas.atomicadditions.integrations.jei;

import com.lucas.atomicadditions.AtomicAdditions;
import com.lucas.atomicadditions.item.ActivatedCrystalItem;
import com.lucas.atomicadditions.recipes.AtomicActivatedCrystalRecipe;
import com.lucas.atomicadditions.recipes.AtomicRecipeSerializers;
import com.lucas.atomicadditions.recipes.AtomicRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeHolder;

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
                AtomicAdditions.ACTIVATED_CRYSTAL.get(),
                new ISubtypeInterpreter<ItemStack>() {

                    @Override
                    public Object getSubtypeData(
                            ItemStack ingredient,
                            UidContext context
                    ) {
                        return getSourceCrystal(
                                ingredient
                        );
                    }

                    @Override
                    public String getLegacyStringSubtypeInfo(
                            ItemStack ingredient,
                            UidContext context
                    ) {
                        String sourceCrystal =
                                getSourceCrystal(
                                        ingredient
                                );

                        return sourceCrystal == null
                                ? ""
                                : sourceCrystal;
                    }
                }
        );
    }

    private static String getSourceCrystal(
            ItemStack stack
    ) {
        CustomData customData =
                stack.get(
                        DataComponents.CUSTOM_DATA
                );

        if (customData == null) {
            return null;
        }

        CompoundTag tag =
                customData.copyTag();

        if (!tag.contains(
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
                AtomicAMRRecipeCategory.TYPE,
                AtomicRecipes.AMR_RECIPES
                        .getRecipesForJEI()
        );
    }

    @Override
    public void onRuntimeAvailable(
            IJeiRuntime runtime
    ) {
        List<ItemStack> activatedCrystals =
                new ArrayList<>();

        for (
                RecipeHolder<AtomicActivatedCrystalRecipe> holder :
                AtomicActivatedCrystalRecipe
                        .getGeneratedRecipes()
        ) {
            AtomicActivatedCrystalRecipe recipe =
                    holder.value();

            if (recipe.getOperation() !=
                    AtomicRecipeSerializers.Operation.ACTIVATE) {
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
            output.setCount(1);

            ResourceLocation sourceId =
                    BuiltInRegistries.ITEM.getKey(
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

    @Override
    public void registerRecipeCatalysts(
            IRecipeCatalystRegistration registration
    ) {
        registration.addRecipeCatalyst(
                AtomicAdditions.CASING_ITEM.get(),
                AtomicAMRRecipeCategory.TYPE
        );

        registration.addRecipeCatalyst(
                AtomicAdditions.CASING_PORT_ITEM.get(),
                AtomicAMRRecipeCategory.TYPE
        );
    }
}