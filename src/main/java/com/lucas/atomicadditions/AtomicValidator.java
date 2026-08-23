package com.lucas.atomicadditions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class AtomicValidator {

    // Tamanho da estrutura: 7x7x7
    public static final int SIZE = 7;

    public static boolean validate(Level level, BlockPos controllerPos) {

        BlockPos min = controllerPos;

        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {

                    BlockPos pos = min.offset(x, y, z);

                    // Verifica apenas as paredes externas
                    if (x == 0 || x == SIZE - 1 ||
                            y == 0 || y == SIZE - 1 ||
                            z == 0 || z == SIZE - 1) {

                        Block block = level.getBlockState(pos).getBlock();

                        if (block != AtomicAdditions.CASING.get()) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }
}