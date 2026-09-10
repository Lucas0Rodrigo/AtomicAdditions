package com.lucas.atomicadditions.item;

import com.lucas.atomicadditions.AtomicAdditions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.BakedModelWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ActivatedCrystalBakedModel
        extends BakedModelWrapper<BakedModel> {

    private static final ModelResourceLocation OVERLAY_MODEL =
            new ModelResourceLocation(
                    ResourceLocation.fromNamespaceAndPath(
                            AtomicAdditions.MODID,
                            "activated_crystal_overlay"
                    ),
                    "inventory"
            );

    private final ItemOverrides overrides;

    public ActivatedCrystalBakedModel(
            BakedModel originalModel
    ) {
        super(originalModel);

        this.overrides = new ItemOverrides() {
            @Override
            public BakedModel resolve(
                    @NotNull BakedModel originalModel,
                    @NotNull ItemStack stack,
                    @Nullable ClientLevel level,
                    @Nullable LivingEntity entity,
                    int seed
            ) {
                ResourceLocation sourceId =
                        ActivatedCrystalItem.getSourceCrystalId(stack);

                if (sourceId == null ||
                        !net.minecraft.core.registries.BuiltInRegistries.ITEM
                                .containsKey(sourceId)) {
                    return originalModel;
                }

                ItemStack sourceStack =
                        new ItemStack(
                                net.minecraft.core.registries
                                        .BuiltInRegistries.ITEM
                                        .get(sourceId)
                        );

                BakedModel sourceModel =
                        Minecraft.getInstance()
                                .getItemRenderer()
                                .getModel(
                                        sourceStack,
                                        level,
                                        entity,
                                        seed
                                );

                BakedModel overlayModel =
                        Minecraft.getInstance()
                                .getModelManager()
                                .getModel(OVERLAY_MODEL);

                return new CompositeModel(
                        sourceModel,
                        overlayModel
                );
            }
        };
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }

    private static class CompositeModel
            extends BakedModelWrapper<BakedModel> {

        private final BakedModel overlayModel;

        private CompositeModel(
                BakedModel sourceModel,
                BakedModel overlayModel
        ) {
            super(sourceModel);
            this.overlayModel = overlayModel;
        }

        @Override
        public List<BakedModel> getRenderPasses(
                ItemStack itemStack,
                boolean fabulous
        ) {
            return List.of(
                    originalModel,
                    overlayModel
            );
        }
    }
}