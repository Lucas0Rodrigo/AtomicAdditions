package com.lucas.atomicadditions.integrations.jei;

import com.lucas.atomicadditions.item.ActivatedCrystalItem;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ActivatedCrystalJEIRenderer
        implements IIngredientRenderer<ItemStack> {

    private static final ResourceLocation AURA_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    "atomicadditions",
                    "textures/item/crystal_overlay.png"
            );

    @Override
    public void render(
            @NotNull GuiGraphics guiGraphics,
            @NotNull ItemStack ingredient
    ) {
        ResourceLocation sourceId =
                ActivatedCrystalItem.getSourceCrystalId(
                        ingredient
                );

        if (sourceId == null ||
                !net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .containsKey(sourceId)) {
            return;
        }

        ItemStack sourceStack =
                new ItemStack(
                        net.minecraft.core.registries
                                .BuiltInRegistries.ITEM
                                .get(sourceId)
                );

        if (sourceStack.isEmpty()) {
            return;
        }

        guiGraphics.renderFakeItem(
                sourceStack,
                0,
                0
        );

        guiGraphics.blit(
                AURA_TEXTURE,
                0,
                0,
                0,
                0,
                16,
                16,
                16,
                16
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