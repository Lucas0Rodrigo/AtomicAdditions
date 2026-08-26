package com.lucas.atomicadditions.multiblock;

import java.util.HashSet;
import java.util.Set;
import mekanism.common.capabilities.chemical.multiblock.MultiblockChemicalTankBuilder;
import mekanism.common.lib.multiblock.MultiblockData;
import mekanism.api.chemical.gas.IGasTank;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

public class AtomicMultiblockData extends MultiblockData {

    public final Set<BlockPos> coils = new HashSet<>();

    public final IGasTank inputTank1;
    public final IGasTank inputTank2;
    public final IGasTank outputTank;

    public AtomicMultiblockData(BlockEntity tile) {
        super(tile);

        inputTank1 = MultiblockChemicalTankBuilder.GAS.input(
                this,
                () -> 1_000_000L,
                gas -> true,
                mekanism.api.chemical.attribute.ChemicalAttributeValidator.ALWAYS_ALLOW,
                createSaveAndComparator()
        );

        inputTank2 = MultiblockChemicalTankBuilder.GAS.input(
                this,
                () -> 1_000_000L,
                gas -> true,
                mekanism.api.chemical.attribute.ChemicalAttributeValidator.ALWAYS_ALLOW,
                createSaveAndComparator()
        );

        outputTank = MultiblockChemicalTankBuilder.GAS.output(
                this,
                () -> 1_000_000L,
                gas -> true,
                mekanism.api.chemical.attribute.ChemicalAttributeValidator.ALWAYS_ALLOW,
                this
        );

        gasTanks.add(inputTank1);
        gasTanks.add(inputTank2);
        gasTanks.add(outputTank);
    }

    public void addCoil(BlockPos pos) {
        coils.add(pos);
    }
}