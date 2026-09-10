package com.lucas.atomicadditions.item;

import com.lucas.atomicadditions.AtomicAdditions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ActivatedCrystalItem extends Item {

    public static final String SOURCE_CRYSTAL_TAG =
            AtomicAdditions.MODID + ":source_crystal";

    public ActivatedCrystalItem(Properties properties) {
        super(properties);
    }

    public static void setSourceCrystal(
            ItemStack stack,
            ResourceLocation sourceCrystal
    ) {
        stack.getOrCreateTag().putString(
                SOURCE_CRYSTAL_TAG,
                sourceCrystal.toString()
        );
    }

    public static ResourceLocation getSourceCrystalId(
            ItemStack stack
    ) {
        CompoundTag tag = stack.getTag();

        if (tag == null ||
                !tag.contains(SOURCE_CRYSTAL_TAG)) {
            return null;
        }

        return ResourceLocation.tryParse(
                tag.getString(SOURCE_CRYSTAL_TAG)
        );
    }

    @Override
    public Component getName(
            ItemStack stack
    ) {
        ResourceLocation sourceId =
                getSourceCrystalId(stack);

        if (sourceId != null &&
                net.minecraft.core.registries
                        .BuiltInRegistries.ITEM
                        .containsKey(sourceId)) {

            ItemStack sourceStack =
                    new ItemStack(
                            net.minecraft.core.registries
                                    .BuiltInRegistries.ITEM
                                    .get(sourceId)
                    );

            return Component.translatable(
                    "item.atomicadditions.activated_crystal",
                    sourceStack.getHoverName()
            );
        }

        return Component.translatable(
                "item.atomicadditions.activated_crystal_fallback"
        );
    }
}