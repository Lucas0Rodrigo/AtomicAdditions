package com.lucas.atomicadditions.item;

import com.lucas.atomicadditions.AtomicAdditions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class ActivatedCrystalItem extends Item {

    public static final String SOURCE_CRYSTAL_TAG =
            AtomicAdditions.MODID + ":source_crystal";

    private static BlockEntityWithoutLevelRenderer renderer;

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

    @Override
    public void initializeClient(
            Consumer<IClientItemExtensions> consumer
    ) {
        consumer.accept(
                new IClientItemExtensions() {

                    @Override
                    public BlockEntityWithoutLevelRenderer
                    getCustomRenderer() {

                        if (renderer == null) {
                            Minecraft minecraft =
                                    Minecraft.getInstance();

                            renderer =
                                    new ActivatedCrystalRenderer(
                                            minecraft
                                                    .getBlockEntityRenderDispatcher(),
                                            minecraft.getEntityModels()
                                    );
                        }

                        return renderer;
                    }
                }
        );
    }
}