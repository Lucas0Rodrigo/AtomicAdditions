package com.lucas.atomicadditions.advancement;

import com.lucas.atomicadditions.AtomicAdditions;
import com.lucas.atomicadditions.chemical.AtomicGases;
import com.lucas.atomicadditions.item.ActivatedCrystalItem;
import java.util.HashSet;
import java.util.Set;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.registries.MekanismGases;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.LogicalSide;

/**
 * O Mekanism não avisa quando um gás é produzido nem quando uma máquina
 * termina uma receita. Por isso, a cada 2 segundos, olhamos o que há
 * perto de cada jogador (inventário e máquinas num raio curto) e
 * concedemos os marcos correspondentes.
 */
@Mod.EventBusSubscriber(modid = AtomicAdditions.MODID)
public final class AtomicProgressScanner {

    private static final int INTERVAL_TICKS = 40;
    private static final int RADIUS_BLOCKS = 24;

    private static final Direction[] SIDES = {
            null,
            Direction.DOWN,
            Direction.UP,
            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST
    };

    private AtomicProgressScanner() {
    }

    @SubscribeEvent
    public static void onPlayerTick(
            TickEvent.PlayerTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END
                || event.side != LogicalSide.SERVER
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (player.tickCount % INTERVAL_TICKS != 0) {
            return;
        }

        scan(player);
    }

    private static void scan(ServerPlayer player) {
        Set<Gas> gasesFound = new HashSet<>();

        scanInventory(player, gasesFound);

        boolean easyPath = false;
        boolean greatMultiplication = false;

        ServerLevel level = player.serverLevel();

        int radiusSqr = RADIUS_BLOCKS * RADIUS_BLOCKS;

        int minChunkX = (player.getBlockX() - RADIUS_BLOCKS) >> 4;
        int maxChunkX = (player.getBlockX() + RADIUS_BLOCKS) >> 4;
        int minChunkZ = (player.getBlockZ() - RADIUS_BLOCKS) >> 4;
        int maxChunkZ = (player.getBlockZ() + RADIUS_BLOCKS) >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {

                LevelChunk chunk =
                        level.getChunkSource().getChunkNow(
                                chunkX,
                                chunkZ
                        );

                if (chunk == null) {
                    continue;
                }

                for (BlockEntity be : chunk.getBlockEntities().values()) {

                    if (!(be instanceof TileEntityMekanism tile)) {
                        continue;
                    }

                    if (be.getBlockPos().distSqr(
                            player.blockPosition()
                    ) > radiusSqr) {
                        continue;
                    }

                    Set<Gas> tileGases = new HashSet<>();

                    for (Direction side : SIDES) {
                        collectGases(be, side, tileGases);
                    }

                    gasesFound.addAll(tileGases);

                    if (tileGases.isEmpty()
                            || !tile.getActive()
                            || !holdsActivatedCrystal(be)) {
                        continue;
                    }

                    if (tileGases.contains(
                            AtomicGases.RHENIUM.get()
                    )) {
                        greatMultiplication = true;
                    }

                    if (tileGases.contains(
                            MekanismGases.HYDROGEN_CHLORIDE.get()
                    )) {
                        easyPath = true;
                    }
                }
            }
        }

        if (gasesFound.contains(AtomicGases.NIOBIUM.get())) {
            AtomicAdvancements.grant(
                    player,
                    AtomicAdvancements.GAS_NIOBIUM
            );
        }

        if (gasesFound.contains(AtomicGases.GERMANIUM.get())) {
            AtomicAdvancements.grant(
                    player,
                    AtomicAdvancements.GAS_GERMANIUM
            );
        }

        if (gasesFound.contains(AtomicGases.COPPER.get())) {
            AtomicAdvancements.grant(
                    player,
                    AtomicAdvancements.GAS_COPPER
            );
        }

        if (gasesFound.contains(AtomicGases.PALLADIUM.get())) {
            AtomicAdvancements.grant(
                    player,
                    AtomicAdvancements.GAS_PALLADIUM
            );
        }

        if (easyPath) {
            AtomicAdvancements.grant(
                    player,
                    AtomicAdvancements.EASY_PATH
            );
        }

        if (greatMultiplication) {
            AtomicAdvancements.grant(
                    player,
                    AtomicAdvancements.GREAT_MULTIPLICATION
            );
        }
    }

    private static void scanInventory(
            ServerPlayer player,
            Set<Gas> gasesFound
    ) {
        Inventory inventory = player.getInventory();

        for (ItemStack stack : inventory.items) {
            collectGases(stack, null, gasesFound);
        }

        for (ItemStack stack : inventory.offhand) {
            collectGases(stack, null, gasesFound);
        }
    }

    private static void collectGases(
            ICapabilityProvider provider,
            Direction side,
            Set<Gas> out
    ) {
        provider.getCapability(
                Capabilities.GAS_HANDLER,
                side
        ).ifPresent(handler -> {
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                GasStack stack =
                        handler.getChemicalInTank(tank);

                if (!stack.isEmpty()) {
                    out.add(stack.getType());
                }
            }
        });
    }

    private static boolean holdsActivatedCrystal(
            BlockEntity be
    ) {
        for (Direction side : SIDES) {
            boolean found =
                    be.getCapability(
                            ForgeCapabilities.ITEM_HANDLER,
                            side
                    ).map(handler -> {
                        for (int slot = 0;
                             slot < handler.getSlots();
                             slot++) {

                            if (handler.getStackInSlot(slot)
                                    .getItem()
                                    instanceof ActivatedCrystalItem) {
                                return true;
                            }
                        }

                        return false;
                    }).orElse(false);

            if (found) {
                return true;
            }
        }

        return false;
    }
}
