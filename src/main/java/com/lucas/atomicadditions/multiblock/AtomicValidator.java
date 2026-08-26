package com.lucas.atomicadditions.multiblock;

import com.lucas.atomicadditions.AtomicAdditions;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.EnumSet;
import java.util.Set;
import mekanism.common.lib.math.voxel.VoxelCuboid;
import mekanism.common.lib.math.voxel.VoxelCuboid.CuboidSide;
import mekanism.common.lib.math.voxel.VoxelCuboid.WallRelative;
import mekanism.common.lib.multiblock.CuboidStructureValidator;
import mekanism.common.lib.multiblock.FormationProtocol.CasingType;
import mekanism.common.lib.multiblock.FormationProtocol.FormationResult;
import mekanism.common.lib.multiblock.FormationProtocol.StructureRequirement;
import mekanism.common.lib.multiblock.IValveHandler.ValveData;
import mekanism.common.lib.multiblock.Structure.Axis;
import mekanism.common.lib.multiblock.StructureHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.registries.ForgeRegistries;

public class AtomicValidator extends CuboidStructureValidator<AtomicMultiblockData> {

    private static final VoxelCuboid BOUNDS = new VoxelCuboid(7, 7, 7);

    private static final byte[][] ALLOWED_GRID = {
            {0, 0, 1, 1, 1, 0, 0},
            {0, 1, 2, 2, 2, 1, 0},
            {1, 2, 2, 2, 2, 2, 1},
            {1, 2, 2, 2, 2, 2, 1},
            {1, 2, 2, 2, 2, 2, 1},
            {0, 1, 2, 2, 2, 1, 0},
            {0, 0, 1, 1, 1, 0, 0}
    };

    private static final Block SUPERCHARGED_COIL =
            ForgeRegistries.BLOCKS.getValue(
                    ResourceLocation.fromNamespaceAndPath("mekanism", "supercharged_coil")
            );

    @Override
    protected StructureRequirement getStructureRequirement(BlockPos pos) {
        WallRelative relative = cuboid.getWallRelative(pos);
        if (relative.isWall()) {
            Axis axis = Axis.get(cuboid.getSide(pos));
            Axis horizontal = axis.horizontal();
            Axis vertical = axis.vertical();
            pos = pos.subtract(cuboid.getMinPos());
            return StructureRequirement.REQUIREMENTS[
                    ALLOWED_GRID[horizontal.getCoord(pos)][vertical.getCoord(pos)]
            ];
        }
        return super.getStructureRequirement(pos);
    }

    @Override
    protected CasingType getCasingType(BlockState state) {
        Block block = state.getBlock();
        if (block == AtomicAdditions.CASING.get()) {
            return CasingType.FRAME;
        }
        if (block == AtomicAdditions.CASING_PORT.get()) {
            return CasingType.VALVE;
        }
        return CasingType.INVALID;
    }

    @Override
    protected boolean validateInner(
            BlockState state,
            Long2ObjectMap<ChunkAccess> chunkMap,
            BlockPos pos
    ) {
        return super.validateInner(state, chunkMap, pos)
                || state.getBlock() == SUPERCHARGED_COIL;
    }

    @Override
    public boolean precheck() {
        cuboid = StructureHelper.fetchCuboid(
                structure,
                BOUNDS,
                BOUNDS,
                EnumSet.allOf(CuboidSide.class),
                72
        );
        return cuboid != null;
    }

    @Override
    public FormationResult postcheck(
            AtomicMultiblockData structure,
            Long2ObjectMap<ChunkAccess> chunkMap
    ) {
        Set<BlockPos> validCoils = new ObjectOpenHashSet<>();

        for (ValveData valve : structure.valves) {
            BlockPos coilPos = valve.location.relative(valve.side.getOpposite());
            if (structure.internalLocations.contains(coilPos)) {
                structure.addCoil(coilPos);
                validCoils.add(coilPos);
            }
        }

        if (structure.internalLocations.size() != validCoils.size()) {
            return FormationResult.fail(
                    net.minecraft.network.chat.Component.literal(
                            "Atomic Additions: Supercharged Coil desconectada de uma Port."
                    )
            );
        }

        return FormationResult.SUCCESS;
    }
}
