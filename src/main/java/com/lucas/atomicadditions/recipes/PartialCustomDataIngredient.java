package com.lucas.atomicadditions.recipes;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

public final class PartialCustomDataIngredient
        implements ICustomIngredient {

    public static final MapCodec<PartialCustomDataIngredient> CODEC =
            RecordCodecBuilder.mapCodec(
                    instance -> instance.group(
                            HolderSetCodec.create(
                                    Registries.ITEM,
                                    BuiltInRegistries.ITEM.holderByNameCodec(),
                                    false
                            ).fieldOf("items").forGetter(
                                    ingredient -> ingredient.items
                            ),
                            CompoundTag.CODEC.fieldOf("nbt").forGetter(
                                    ingredient -> ingredient.requiredTag
                            )
                    ).apply(
                            instance,
                            PartialCustomDataIngredient::new
                    )
            );

    public static final IngredientType<PartialCustomDataIngredient> TYPE =
            new IngredientType<>(
                    CODEC
            );

    private final HolderSet<Item> items;

    private final CompoundTag requiredTag;

    public PartialCustomDataIngredient(
            HolderSet<Item> items,
            CompoundTag requiredTag
    ) {
        this.items =
                items;

        this.requiredTag =
                requiredTag;
    }

    public static PartialCustomDataIngredient of(
            Item item,
            CompoundTag requiredTag
    ) {
        return new PartialCustomDataIngredient(
                HolderSet.direct(
                        item.builtInRegistryHolder()
                ),
                requiredTag
        );
    }

    @Override
    public boolean test(
            ItemStack stack
    ) {
        if (!items.contains(
                stack.getItemHolder()
        )) {
            return false;
        }

        CustomData customData =
                stack.get(
                        DataComponents.CUSTOM_DATA
                );

        return customData != null
                && customData.matchedBy(
                requiredTag
        );
    }

    @Override
    public Stream<ItemStack> getItems() {
        return items.stream().map(
                holder -> {
                    ItemStack stack =
                            new ItemStack(
                                    holder
                            );

                    stack.set(
                            DataComponents.CUSTOM_DATA,
                            CustomData.of(
                                    requiredTag.copy()
                            )
                    );

                    return stack;
                }
        );
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return TYPE;
    }
}
