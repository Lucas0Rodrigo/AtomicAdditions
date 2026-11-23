package com.lucas.atomicadditions.advancement;

import com.lucas.atomicadditions.AtomicAdditions;
import com.lucas.atomicadditions.chemical.AtomicGases;
import mekanism.api.chemical.Chemical;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AtomicAdvancements {

    public static final String FORMED = "formed";
    public static final String OPERATING = "operating";
    public static final String TANTALUM = "tantalum";
    public static final String RHENIUM = "rhenium";
    public static final String GAS_NIOBIUM = "gas_niobium";
    public static final String GAS_GERMANIUM = "gas_germanium";
    public static final String GAS_COPPER = "gas_copper";
    public static final String GAS_PALLADIUM = "gas_palladium";
    public static final String EASY_PATH = "easy_path";
    public static final String GREAT_MULTIPLICATION = "great_multiplication";

    /** Raio (em blocos) em que jogadores recebem conquistas de multiblocks. */
    public static final double MULTIBLOCK_RADIUS = 32.0D;

    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(
                    Registries.TRIGGER_TYPE,
                    AtomicAdditions.MODID
            );

    public static final DeferredHolder<CriterionTrigger<?>, AtomicTrigger>
            MILESTONE = TRIGGERS.register(
            "milestone",
            AtomicTrigger::new
    );

    private AtomicAdvancements() {
    }

    public static void grant(
            ServerPlayer player,
            String milestone
    ) {
        MILESTONE.get().trigger(player, milestone);
    }

    public static void grantNear(
            Level level,
            BlockPos center,
            String milestone
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        double radiusSqr =
                MULTIBLOCK_RADIUS * MULTIBLOCK_RADIUS;

        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(
                    center.getX() + 0.5D,
                    center.getY() + 0.5D,
                    center.getZ() + 0.5D
            ) <= radiusSqr) {
                grant(player, milestone);
            }
        }
    }

    /** Chamado pelo AMR sempre que uma receita termina. */
    public static void onAmrOutput(
            Level level,
            BlockPos center,
            Chemical output
    ) {
        if (output == AtomicGases.TANTALUM.get()) {
            grantNear(level, center, TANTALUM);
        } else if (output == AtomicGases.RHENIUM.get()) {
            grantNear(level, center, RHENIUM);
        }
    }
}
