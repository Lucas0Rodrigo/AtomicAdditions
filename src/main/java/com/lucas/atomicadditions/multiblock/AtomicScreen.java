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
import mekanism.common.inventory.container.tile.EmptyTileContainer;
import mekanism.common.lib.Color;
import mekanism.common.lib.Color.ColorFunction;
import mekanism.common.util.text.TextUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class AtomicScreen extends GuiMekanismTile<
        AtomicCasingBlockEntity,
        EmptyTileContainer<AtomicCasingBlockEntity>
        > {

    public AtomicScreen(
            EmptyTileContainer<AtomicCasingBlockEntity> container,
            Inventory inv,
            Component title
    ) {
        super(
                container,
                inv,
                Component.translatable("container.atomicadditions.atomic")
        );

        dynamicSlots = false;
        imageWidth += 18;
    }

    @Override
    protected void init() {
        imageHeight = 96;
        super.init();
    }

    @Override
    protected void addGuiElements() {

        // Gás de entrada 1
        addRenderableWidget(
                new GuiGasGauge(
                        () -> tile.getMultiblock().inputTank1,
                        () -> tile.getMultiblock().getGasTanks(null),
                        GaugeType.STANDARD,
                        this,
                        7,
                        17
                )
        );

        // Gás de entrada 2
        addRenderableWidget(
                new GuiGasGauge(
                        () -> tile.getMultiblock().inputTank2,
                        () -> tile.getMultiblock().getGasTanks(null),
                        GaugeType.STANDARD,
                        this,
                        25,
                        17
                )
        );

        // Gás de saída
        addRenderableWidget(
                new GuiGasGauge(
                        () -> tile.getMultiblock().outputTank,
                        () -> tile.getMultiblock().getGasTanks(null),
                        GaugeType.STANDARD,
                        this,
                        169,
                        17
                )
        );

        // Painel de status
        addRenderableWidget(
                new GuiInnerScreen(
                        this,
                        45,
                        17,
                        122,
                        60,
                        () -> {
                            List<Component> list = new ArrayList<>();

                            list.add(
                                    MekanismLang.STATUS.translate(
                                            MekanismLang.IDLE
                                    )
                            );

                            return list;
                        }
                )
        );

        // Barra de progresso
        addRenderableWidget(
                new GuiDynamicHorizontalRateBar(
                        this,
                        new IBarInfoHandler() {

                            @Override
                            public Component getTooltip() {
                                return MekanismLang.PROGRESS.translate(
                                        TextUtils.getPercent(0)
                                );
                            }

                            @Override
                            public double getLevel() {
                                return 0;
                            }
                        },
                        7,
                        79,
                        176,
                        ColorFunction.scale(
                                Color.rgbi(60, 45, 74),
                                Color.rgbi(100, 30, 170)
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
    }
}