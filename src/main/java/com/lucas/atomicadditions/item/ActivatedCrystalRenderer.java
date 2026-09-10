package com.lucas.atomicadditions.item;

import com.lucas.atomicadditions.AtomicAdditions;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

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

        if (sourceId == null ||
                !net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .containsKey(sourceId)) {
            return;
        }

        ItemStack sourceStack =
                new ItemStack(
                        net.minecraft.core.registries.BuiltInRegistries.ITEM
                                .get(sourceId)
                );

        /*
         * =========================================================
         * CRISTAL ORIGINAL
         * =========================================================
         *
         * Deixa o próprio ItemRenderer do Minecraft renderizar
         * o item original.
         *
         * Isso preserva:
         * - iluminação
         * - transform
         * - escala
         * - rotação
         * - render type
         * - tint
         * - emissividade
         * - quads
         *
         * exatamente como o item original.
         */
        poseStack.pushPose();

        itemRenderer.renderStatic(
                sourceStack,
                displayContext,
                combinedLight,
                combinedOverlay,
                poseStack,
                bufferSource,
                level,
                0
        );

        poseStack.popPose();

        /*
         * =========================================================
         * OVERLAY
         * =========================================================
         */

        BakedModel overlayModel =
                minecraft.getModelManager()
                        .getModel(OVERLAY_MODEL);

        if (overlayModel == null) {
            return;
        }

        boolean leftHand =
                displayContext ==
                        ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                        ||
                        displayContext ==
                                ItemDisplayContext.THIRD_PERSON_LEFT_HAND;

        poseStack.pushPose();

        BakedModel transformedOverlay =
                overlayModel.applyTransform(
                        displayContext,
                        poseStack,
                        leftHand
                );

        renderOverlay(
                itemRenderer,
                transformedOverlay,
                activatedStack,
                poseStack,
                bufferSource,
                combinedLight,
                combinedOverlay,
                minecraft
        );

        poseStack.popPose();
    }

    private void renderOverlay(
            ItemRenderer itemRenderer,
            BakedModel model,
            ItemStack stack,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int combinedLight,
            int combinedOverlay,
            Minecraft minecraft
    ) {
        boolean fabulous =
                minecraft.options.graphicsMode().get()
                        == net.minecraft.client.GraphicsStatus.FABULOUS;

        for (var renderType :
                model.getRenderTypes(stack, fabulous)) {

            VertexConsumer consumer =
                    bufferSource.getBuffer(renderType);

            itemRenderer.renderModelLists(
                    model,
                    stack,
                    combinedLight,
                    combinedOverlay,
                    poseStack,
                    consumer
            );
        }
    }
}