package com.lucas.atomicadditions;

import mekanism.client.gui.GuiMekanismTile;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class AtomicScreen extends GuiMekanismTile<
        AtomicCasingBlockEntity,
        MekanismTileContainer<AtomicCasingBlockEntity>
        > {

    public AtomicScreen(
            MekanismTileContainer<AtomicCasingBlockEntity> container,
            Inventory inv,
            Component title
    ) {
        super(container, inv, title);

        dynamicSlots = true;

        // Mesmo ajuste vertical utilizado pelo SPS.
        imageHeight += 16;
        inventoryLabelY = imageHeight - 92;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
    }

    @Override
    protected void drawForegroundText(
            @NotNull GuiGraphics guiGraphics,
            int mouseX,
            int mouseY
    ) {
        renderTitleText(guiGraphics);

        drawString(
                guiGraphics,
                playerInventoryTitle,
                inventoryLabelX,
                inventoryLabelY,
                titleTextColor()
        );

        super.drawForegroundText(
                guiGraphics,
                mouseX,
                mouseY
        );
    }
}