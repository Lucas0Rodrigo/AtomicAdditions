package com.lucas.atomicadditions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class AtomicValidator {

    private static final int SIZE = 7;

    private static final byte[][] ALLOWED_GRID = {
            {0, 0, 1, 1, 1, 0, 0},
            {0, 1, 2, 2, 2, 1, 0},
            {1, 2, 2, 2, 2, 2, 1},
            {1, 2, 2, 2, 2, 2, 1},
            {1, 2, 2, 2, 2, 2, 1},
            {0, 1, 2, 2, 2, 1, 0},
            {0, 0, 1, 1, 1, 0, 0}
    };

    public static boolean validate(Level level, BlockPos center) {

        for (int x = -6; x <= 0; x++) {
            for (int y = -6; y <= 0; y++) {
                for (int z = -6; z <= 0; z++) {

                    BlockPos min = center.offset(x, y, z);

                    if (validateCuboid(level, min)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean validateCuboid(Level level, BlockPos min) {

        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {

                    BlockPos pos = min.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);

                    boolean wall =
                            x == 0 || x == 6 ||
                                    y == 0 || y == 6 ||
                                    z == 0 || z == 6;

                    if (wall) {

                        int gridX = x;
                        int gridY = y;

                        int requirement = ALLOWED_GRID[gridX][gridY];

                        if (requirement == 0) {
                            continue;
                        }

                        Block block = state.getBlock();

                        if (block != AtomicAdditions.CASING.get()) {
                            return false;
                        }

                    } else {

                        Block block = state.getBlock();

                        if (block != AtomicAdditions.CASING.get()
                                && !isSuperchargedCoil(block)) {

                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    private static boolean isSuperchargedCoil(Block block) {
        return block.getDescriptionId().contains("supercharged_coil");
    }
}