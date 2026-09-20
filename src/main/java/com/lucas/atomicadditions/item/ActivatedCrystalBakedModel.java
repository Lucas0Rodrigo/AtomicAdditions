package com.lucas.atomicadditions.item;

import com.lucas.atomicadditions.AtomicAdditions;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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
                    @NotNull BakedModel model,
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
                    return model;
                }

                ItemStack sourceStack =
                        ActivatedCrystalItem.getSourceCrystalStack(stack);

                if (sourceStack.isEmpty()) {
                    return model;
                }

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
                        overlayModel,
                        sourceStack
                );
            }
        };
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }

    @Override
    public BakedModel applyTransform(
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            boolean applyLeftHandTransform
    ) {
        originalModel.applyTransform(
                displayContext,
                poseStack,
                applyLeftHandTransform
        );

        return this;
    }

    private static class CompositeModel
            extends BakedModelWrapper<BakedModel> {

        private final BakedModel overlayModel;
        private final ItemStack sourceStack;

        private CompositeModel(
                BakedModel sourceModel,
                BakedModel overlayModel,
                ItemStack sourceStack
        ) {
            super(sourceModel);

            this.overlayModel = overlayModel;
            this.sourceStack =
                    sourceStack.copy();
        }

        @Override
        public List<BakedModel> getRenderPasses(
                ItemStack stack,
                boolean fabulous
        ) {
            List<BakedModel> passes =
                    new ArrayList<>(
                            originalModel.getRenderPasses(
                                    sourceStack,
                                    fabulous
                            )
                    );

            passes.add(
                    new OverlayModel(
                            overlayModel
                    )
            );

            return passes;
        }

        @Override
        public BakedModel applyTransform(
                ItemDisplayContext displayContext,
                PoseStack poseStack,
                boolean applyLeftHandTransform
        ) {
            originalModel.applyTransform(
                    displayContext,
                    poseStack,
                    applyLeftHandTransform
            );

            return this;
        }
    }

    private static class OverlayModel
            extends BakedModelWrapper<BakedModel> {

        private OverlayModel(
                BakedModel originalModel
        ) {
            super(originalModel);
        }

        @Override
        public List<BakedQuad> getQuads(
                @Nullable BlockState state,
                @Nullable Direction side,
                @NotNull RandomSource rand
        ) {
            List<BakedQuad> originalQuads =
                    originalModel.getQuads(
                            state,
                            side,
                            rand
                    );

            List<BakedQuad> quads =
                    new ArrayList<>(
                            originalQuads.size()
                    );

            for (BakedQuad quad :
                    originalQuads) {

                quads.add(
                        new BakedQuad(
                                quad.getVertices().clone(),
                                -1,
                                quad.getDirection(),
                                quad.getSprite(),
                                quad.isShade()
                        )
                );
            }

            return quads;
        }

        @Override
        public BakedModel applyTransform(
                ItemDisplayContext displayContext,
                PoseStack poseStack,
                boolean applyLeftHandTransform
        ) {
            originalModel.applyTransform(
                    displayContext,
                    poseStack,
                    applyLeftHandTransform
            );

            poseStack.translate(
                    0.0F,
                    0.0F,
                    -0.001F
            );

            return null;
        }
    }
}