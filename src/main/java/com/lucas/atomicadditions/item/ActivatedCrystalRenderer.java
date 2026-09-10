package com.lucas.atomicadditions.item;

import com.lucas.atomicadditions.AtomicAdditions;
import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

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

        if (!net.minecraftforge.registries.ForgeRegistries.ITEMS
                .containsKey(sourceId)) {
            return;
        }

        ItemStack sourceStack =
                new ItemStack(
                        Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(sourceId))
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

        itemRenderer.render(
                sourceStack,
                displayContext,
                leftHand,
                poseStack,
                bufferSource,
                combinedLight,
                combinedOverlay,
                sourceModel
        );

        BakedModel overlayModel =
                minecraft.getModelManager()
                        .getModel(OVERLAY_MODEL);

        poseStack.pushPose();

        overlayModel.applyTransform(
                displayContext,
                poseStack,
                leftHand
        );

        itemRenderer.renderModelLists(
                overlayModel,
                activatedStack,
                combinedLight,
                combinedOverlay,
                poseStack,
                bufferSource.getBuffer(
                        overlayModel.getRenderTypes(
                                activatedStack,
                                false
                        ).iterator().next()
                )
        );

        poseStack.popPose();
    }
}