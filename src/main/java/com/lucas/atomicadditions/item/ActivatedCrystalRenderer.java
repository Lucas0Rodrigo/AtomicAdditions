package com.lucas.atomicadditions.item;

import com.lucas.atomicadditions.AtomicAdditions;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
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
            net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher dispatcher,
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

        ItemStack sourceStack =
                new ItemStack(
                        net.minecraft.core.registries
                                .BuiltInRegistries.ITEM
                                .get(sourceId)
                );

        if (sourceStack.isEmpty()) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        ItemRenderer itemRenderer =
                minecraft.getItemRenderer();

        Level level =
                minecraft.level;

        /*
         * Primeiro: cristal original.
         */
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
         * Segundo: overlay do Cristal Ativado.
         *
         * Chamamos render(...) diretamente com o modelo,
         * evitando voltar para o renderer customizado do próprio item.
         */
        BakedModel overlayModel =
                minecraft.getModelManager()
                        .getModel(OVERLAY_MODEL);

        boolean leftHand =
                displayContext ==
                        ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                        ||
                        displayContext ==
                                ItemDisplayContext.THIRD_PERSON_LEFT_HAND;

        itemRenderer.render(
                activatedStack,
                displayContext,
                leftHand,
                poseStack,
                bufferSource,
                combinedLight,
                combinedOverlay,
                overlayModel
        );
    }
}