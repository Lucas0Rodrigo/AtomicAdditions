package com.lucas.atomicadditions.multiblock;

import java.util.ArrayList;
import java.util.List;
import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.GuiBar.IBarInfoHandler;
import mekanism.client.gui.element.bar.GuiDynamicHorizontalRateBar;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiGasGauge;
import mekanism.common.MekanismLang;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.lib.Color;
import mekanism.common.lib.Color.ColorFunction;
import mekanism.common.util.text.EnergyDisplay;
import mekanism.common.util.text.TextUtils;
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
        super(
                container,
                inv,
                Component.translatable(
                        "container.atomicadditions.atomic"
                )
        );

        /*
         * Igual ao SPS.
         */
        dynamicSlots = true;
        imageHeight += 16;
    }

    @Override
    protected void init() {
        /*
         * Igual ao SPS.
         */
        inventoryLabelY =
                imageHeight - 92;

        super.init();
    }

    @Override
    protected void addGuiElements() {

        /*
         * Adiciona os slots do inventário.
         */
        super.addGuiElements();

        /*
         * Primeiro tanque de entrada.
         */
        addRenderableWidget(
                new GuiGasGauge(
                        () ->
                                tile.getMultiblock().inputTank1,
                        () ->
                                tile.getMultiblock().getGasTanks(null),
                        GaugeType.STANDARD,
                        this,
                        7,
                        17
                )
        );

        /*
         * Segundo tanque de entrada.
         */
        addRenderableWidget(
                new GuiGasGauge(
                        () ->
                                tile.getMultiblock().inputTank2,
                        () ->
                                tile.getMultiblock().getGasTanks(null),
                        GaugeType.STANDARD,
                        this,
                        25,
                        17
                )
        );

        /*
         * Tanque de saída.
         *
         * Mesma posição do SPS.
         */
        addRenderableWidget(
                new GuiGasGauge(
                        () ->
                                tile.getMultiblock().outputTank,
                        () ->
                                tile.getMultiblock().getGasTanks(null),
                        GaugeType.STANDARD,
                        this,
                        151,
                        17
                )
        );

        /*
         * Painel central.
         *
         * O SPS usa 122 px.
         * Como o AMR possui um tanque adicional de 18 px
         * à esquerda, o painel fica com 104 px.
         */
        addRenderableWidget(
                new GuiInnerScreen(
                        this,
                        45,
                        17,
                        104,
                        60,
                        () -> {

                            List<Component> list =
                                    new ArrayList<>();

                            AtomicMultiblockData multiblock =
                                    tile.getMultiblock();

                            boolean active =
                                    multiblock.lastProcessed > 0;

                            list.add(
                                    MekanismLang.STATUS.translate(
                                            active
                                                    ? MekanismLang.ACTIVE
                                                    : MekanismLang.IDLE
                                    )
                            );

                            if (active) {

                                list.add(
                                        MekanismLang.SPS_ENERGY_INPUT.translate(
                                                EnergyDisplay.of(
                                                        multiblock.lastReceivedEnergy
                                                )
                                        )
                                );

                                list.add(
                                        MekanismLang.PROCESS_RATE_MB.translate(
                                                multiblock.getProcessRate()
                                        )
                                );
                            }

                            return list;
                        }
                )
        );

        /*
         * Barra de progresso.
         *
         * Mesma largura do SPS.
         */
        addRenderableWidget(
                new GuiDynamicHorizontalRateBar(
                        this,
                        new IBarInfoHandler() {

                            @Override
                            public Component getTooltip() {

                                return MekanismLang.PROGRESS.translate(
                                        TextUtils.getPercent(
                                                tile.getMultiblock()
                                                        .getScaledProgress()
                                        )
                                );
                            }

                            @Override
                            public double getLevel() {

                                return Math.min(
                                        1,
                                        tile.getMultiblock()
                                                .getScaledProgress()
                                );
                            }
                        },
                        7,
                        79,
                        160,
                        ColorFunction.scale(
                                Color.rgbi(
                                        60,
                                        45,
                                        74
                                ),
                                Color.rgbi(
                                        100,
                                        30,
                                        170
                                )
                        )
                )
        );
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