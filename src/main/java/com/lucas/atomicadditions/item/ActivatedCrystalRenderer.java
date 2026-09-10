package com.lucas.atomicadditions.item;

import com.lucas.atomicadditions.AtomicAdditions;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ActivatedCrystalRenderer
        extends BlockEntityWithoutLevelRenderer {

    private static final ModelResourceLocation OVERLAY_MODEL =
            new ModelResourceLocation(
                    ResourceLocation.fromNamespaceAndPath(
                            AtomicAdditions.MODID,
                            "activated_crystal_overlay"
                    ),
                    "inventory"
            );

    public ActivatedCrystalRenderer(
            BlockEntityRenderDispatcher dispatcher,
            EntityModelSet modelSet
    ) {
        super(dispatcher, modelSet);
    }

    @Override
    public void renderByItem(
            @NotNull ItemStack activatedStack,
            @NotNull ItemDisplayContext displayContext,
            @NotNull PoseStack poseStack,
            @NotNull MultiBufferSource bufferSource,
            int combinedLight,
            int combinedOverlay
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        ItemRenderer itemRenderer = minecraft.getItemRenderer();
        Level level = minecraft.level;

        ResourceLocation sourceId =
                ActivatedCrystalItem.getSourceCrystalId(
                        activatedStack
                );

        if (sourceId == null) {
            return;
        }

        if (!net.minecraft.core.registries.BuiltInRegistries.ITEM
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

        BakedModel sourceModel =
                itemRenderer.getModel(
                        sourceStack,
                        level,
                        null,
                        0
                );

        boolean leftHand =
                displayContext ==
                        ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                        ||
                        displayContext ==
                                ItemDisplayContext.THIRD_PERSON_LEFT_HAND;

        boolean hand =
                displayContext ==
                        ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                        ||
                        displayContext ==
                                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                        ||
                        displayContext ==
                                ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                        ||
                        displayContext ==
                                ItemDisplayContext.THIRD_PERSON_LEFT_HAND;

        float handAngle =
                leftHand ? 25.0F : -25.0F;

        poseStack.pushPose();

        if (hand) {
            poseStack.mulPose(
                    Axis.YP.rotationDegrees(
                            handAngle
                    )
            );
        }

        List<RenderType> sourceRenderTypes =
                sourceModel.getRenderTypes(
                        sourceStack,
                        false
                );

        for (RenderType renderType : sourceRenderTypes) {
            itemRenderer.renderModelLists(
                    sourceModel,
                    sourceStack,
                    combinedLight,
                    combinedOverlay,
                    poseStack,
                    bufferSource.getBuffer(renderType)
            );
        }

        poseStack.popPose();

        BakedModel overlayModel =
                minecraft.getModelManager()
                        .getModel(OVERLAY_MODEL);

        if (overlayModel == null) {
            return;
        }

        poseStack.pushPose();

        if (displayContext == ItemDisplayContext.GUI) {
            overlayModel.applyTransform(
                    displayContext,
                    poseStack,
                    leftHand
            );
        }

        if (hand) {
            poseStack.mulPose(
                    Axis.YP.rotationDegrees(
                            handAngle
                    )
            );

            poseStack.scale(
                    leftHand ? -1.0F : 1.0F,
                    1.0F,
                    1.0F
            );
        }

        List<RenderType> overlayRenderTypes =
                overlayModel.getRenderTypes(
                        activatedStack,
                        false
                );

        for (RenderType renderType : overlayRenderTypes) {
            itemRenderer.renderModelLists(
                    overlayModel,
                    activatedStack,
                    combinedLight,
                    combinedOverlay,
                    poseStack,
                    bufferSource.getBuffer(renderType)
            );
        }

        poseStack.popPose();
    }
}