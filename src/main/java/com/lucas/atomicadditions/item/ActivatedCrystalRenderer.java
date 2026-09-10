package com.lucas.atomicadditions.item;

import com.lucas.atomicadditions.AtomicAdditions;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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

public class ActivatedCrystalRenderer
        extends BlockEntityWithoutLevelRenderer {

    /*
     * Modelo do overlay.
     *
     * Arquivo:
     *
     * assets/atomicadditions/models/item/
     * activated_crystal_overlay.json
     */
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

        /*
         * =========================================================
         * 1. DESCOBRIR O CRISTAL ORIGINAL
         * =========================================================
         */
        ResourceLocation sourceId =
                ActivatedCrystalItem.getSourceCrystalId(
                        activatedStack
                );

        if (sourceId == null) {

            AtomicAdditions.LOGGER.warn(
                    "[AA DEBUG] Activated Crystal sem source_crystal."
            );

            return;
        }

        if (!net.minecraft.core.registries
                .BuiltInRegistries.ITEM
                .containsKey(sourceId)) {

            AtomicAdditions.LOGGER.warn(
                    "[AA DEBUG] Source Crystal não encontrado: {}",
                    sourceId
            );

            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        ItemRenderer itemRenderer =
                minecraft.getItemRenderer();

        Level level =
                minecraft.level;

        /*
         * =========================================================
         * 2. STACK DO CRISTAL ORIGINAL
         * =========================================================
         */
        ItemStack sourceStack =
                new ItemStack(
                        net.minecraft.core.registries
                                .BuiltInRegistries.ITEM
                                .get(sourceId)
                );

        if (sourceStack.isEmpty()) {
            return;
        }

        /*
         * =========================================================
         * 3. MODELO DO CRISTAL ORIGINAL
         * =========================================================
         */
        BakedModel sourceModel =
                itemRenderer.getModel(
                        sourceStack,
                        level,
                        null,
                        0
                );

        if (sourceModel == null) {

            AtomicAdditions.LOGGER.warn(
                    "[AA DEBUG] Modelo do cristal não encontrado: {}",
                    sourceId
            );

            return;
        }

        /*
         * =========================================================
         * 4. APLICAR O TRANSFORM DO CONTEXTO
         * =========================================================
         *
         * Esse é o ponto que estava faltando.
         *
         * renderModelLists() apenas desenha o modelo.
         * Ele não aplica sozinho o transform de:
         *
         * GUI
         * mão direita
         * mão esquerda
         * chão
         * item frame
         * etc.
         *
         * applyTransform() faz exatamente essa parte.
         */
        boolean leftHand =
                displayContext ==
                        ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                        ||
                        displayContext ==
                                ItemDisplayContext.THIRD_PERSON_LEFT_HAND;

        poseStack.pushPose();

        BakedModel transformedSourceModel =
                sourceModel.applyTransform(
                        displayContext,
                        poseStack,
                        leftHand
                );

        /*
         * =========================================================
         * 5. RENDERIZAR O CRISTAL ORIGINAL
         * =========================================================
         */
        renderModel(
                itemRenderer,
                transformedSourceModel,
                sourceStack,
                poseStack,
                bufferSource,
                combinedLight,
                combinedOverlay,
                minecraft
        );

        /*
         * =========================================================
         * 6. MODELO DO OVERLAY
         * =========================================================
         */
        BakedModel overlayModel =
                minecraft.getModelManager()
                        .getModel(
                                OVERLAY_MODEL
                        );

        if (overlayModel == null) {

            AtomicAdditions.LOGGER.warn(
                    "[AA DEBUG] Modelo do overlay não encontrado: {}",
                    OVERLAY_MODEL
            );

            poseStack.popPose();
            return;
        }

        /*
         * =========================================================
         * 7. RENDERIZAR OVERLAY
         * =========================================================
         *
         * O overlay é desenhado na MESMA PoseStack que já recebeu
         * o transform do cristal original.
         *
         * Não aplicamos um segundo transform.
         */
        renderModel(
                itemRenderer,
                overlayModel,
                activatedStack,
                poseStack,
                bufferSource,
                combinedLight,
                combinedOverlay,
                minecraft
        );

        poseStack.popPose();
    }

    private void renderModel(
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

        /*
         * O Forge permite que um BakedModel possua diferentes
         * RenderTypes.
         */
        for (
                RenderType renderType :
                model.getRenderTypes(
                        stack,
                        fabulous
                )
        ) {

            VertexConsumer vertexConsumer =
                    bufferSource.getBuffer(
                            renderType
                    );

            itemRenderer.renderModelLists(
                    model,
                    stack,
                    combinedLight,
                    combinedOverlay,
                    poseStack,
                    vertexConsumer
            );
        }
    }
}