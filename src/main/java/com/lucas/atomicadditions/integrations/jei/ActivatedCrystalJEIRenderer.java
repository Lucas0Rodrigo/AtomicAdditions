package com.lucas.atomicadditions.integrations.jei;

import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ActivatedCrystalJEIRenderer
        implements IIngredientRenderer<ItemStack> {

    public ActivatedCrystalJEIRenderer(ItemStack output) {
    }

    @Override
    public void render(
            @NotNull GuiGraphics guiGraphics,
            @NotNull ItemStack ingredient
    ) {
        guiGraphics.renderItem(
                ingredient, 0, 0
        );
    }

    @SuppressWarnings("removal")
    @Override
    public List<Component> getTooltip(
            ItemStack ingredient,
            TooltipFlag tooltipFlag
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null) {
            return List.of(
                    ingredient.getHoverName()
            );
        }

        return ingredient.getTooltipLines(
                minecraft.player,
                tooltipFlag
        );
    }

    @Override
    public void getTooltip(
            @NotNull ITooltipBuilder tooltip,
            @NotNull ItemStack ingredient,
            @NotNull TooltipFlag tooltipFlag
    ) {
        tooltip.addAll(
                getTooltip(
                        ingredient,
                        tooltipFlag
                )
        );
    }

    @Override
    public Font getFontRenderer(
            Minecraft minecraft,
            ItemStack ingredient
    ) {
        return minecraft.font;
    }
}