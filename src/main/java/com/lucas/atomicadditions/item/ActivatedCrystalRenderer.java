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

    private static final ModelResourceLocation OVERLAY_MODEL =
            new ModelResourceLocation(
                    ResourceLocation.fromNamespaceAndPath(
                            AtomicAdditions.MODID,
                            "item/activated_crystal_overlay"
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

        ResourceLocation sourceId =
                ActivatedCrystalItem.getSourceCrystalId(
                        activatedStack
                );

        if (sourceId == null) {
            return;
        }

        if (!net.minecraft.core.registries
                .BuiltInRegistries.ITEM
                .containsKey(sourceId)) {
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
         * 1. CRISTAL ORIGINAL
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

        /*
         * =========================================================
         * 2. OVERLAY
         * =========================================================
         *
         * Não usamos itemRenderer.render(), porque ele pode aplicar
         * novamente o transform do ItemDisplayContext.
         *
         * renderModelLists() desenha o modelo diretamente na pose
         * atual, mantendo o overlay exatamente sobre o cristal.
         */
        BakedModel overlayModel =
                minecraft.getModelManager()
                        .getModel(OVERLAY_MODEL);

        if (overlayModel == null) {
            AtomicAdditions.LOGGER.warn(
                    "[AA DEBUG] Não foi possível carregar o modelo do overlay: {}",
                    OVERLAY_MODEL
            );
            return;
        }

        for (RenderType renderType :
                overlayModel.getRenderTypes(
                        activatedStack,
                        false
                )) {

            VertexConsumer vertexConsumer =
                    bufferSource.getBuffer(
                            renderType
                    );

            itemRenderer.renderModelLists(
                    overlayModel,
                    activatedStack,
                    combinedLight,
                    combinedOverlay,
                    poseStack,
                    vertexConsumer
            );
        }
    }
}