package com.lucas.atomicadditions.item;

import com.lucas.atomicadditions.AtomicAdditions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ActivatedCrystalItem extends Item {

    public static final String SOURCE_CRYSTAL_TAG =
            AtomicAdditions.MODID + ":source_crystal";

    public static final String SOURCE_CRYSTAL_STACK_TAG =
            AtomicAdditions.MODID + ":source_crystal_stack";

    public ActivatedCrystalItem(
            Properties properties
    ) {
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

    public static void setSourceCrystal(
            ItemStack stack,
            ItemStack sourceCrystal
    ) {
        ResourceLocation sourceId =
                BuiltInRegistries.ITEM.getKey(
                        sourceCrystal.getItem()
                );

        setSourceCrystal(
                stack,
                sourceId
        );

        stack.getOrCreateTag().put(
                SOURCE_CRYSTAL_STACK_TAG,
                sourceCrystal.save(
                        new CompoundTag()
                )
        );
    }

    public static ResourceLocation getSourceCrystalId(
            ItemStack stack
    ) {
        CompoundTag tag =
                stack.getTag();

        if (tag == null ||
                !tag.contains(
                        SOURCE_CRYSTAL_TAG,
                        Tag.TAG_STRING
                )) {
            return null;
        }

        return ResourceLocation.tryParse(
                tag.getString(
                        SOURCE_CRYSTAL_TAG
                )
        );
    }

    public static ItemStack getSourceCrystalStack(
            ItemStack stack
    ) {
        CompoundTag tag =
                stack.getTag();

        if (tag != null &&
                tag.contains(
                        SOURCE_CRYSTAL_STACK_TAG,
                        Tag.TAG_COMPOUND
                )) {

            ItemStack sourceStack =
                    ItemStack.of(
                            tag.getCompound(
                                    SOURCE_CRYSTAL_STACK_TAG
                            )
                    );

            if (!sourceStack.isEmpty()) {
                return sourceStack;
            }
        }

        ResourceLocation sourceId =
                getSourceCrystalId(stack);

        if (sourceId == null ||
                !BuiltInRegistries.ITEM.containsKey(
                        sourceId
                )) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(
                BuiltInRegistries.ITEM.get(
                        sourceId
                )
        );
    }

    @Override
    public Component getName(
            ItemStack stack
    ) {
        ItemStack sourceStack =
                getSourceCrystalStack(
                        stack
                );

        if (!sourceStack.isEmpty()) {
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