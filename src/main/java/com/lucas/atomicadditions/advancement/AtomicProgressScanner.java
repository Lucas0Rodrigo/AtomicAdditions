package com.lucas.atomicadditions.advancement;

import com.lucas.atomicadditions.AtomicAdditions;
import com.lucas.atomicadditions.chemical.AtomicGases;
import com.lucas.atomicadditions.item.ActivatedCrystalItem;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.registries.MekanismChemicals;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * O Mekanism não avisa quando um gás é produzido nem quando uma máquina
 * termina uma receita. Por isso, a cada 2 segundos, olhamos o que há
 * perto de cada jogador (inventário e máquinas num raio curto) e
 * concedemos os marcos correspondentes.
 */
@EventBusSubscriber(modid = AtomicAdditions.MODID)
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
            PlayerTickEvent.Post event
    ) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.tickCount % INTERVAL_TICKS != 0) {
            return;
        }

        scan(player);
    }

    private static void scan(ServerPlayer player) {
        Set<Chemical> found = new HashSet<>();

        scanInventory(player, found);

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

                    BlockPos pos = be.getBlockPos();

                    if (pos.distSqr(player.blockPosition()) > radiusSqr) {
                        continue;
                    }

                    Set<Chemical> tileChemicals = new HashSet<>();

                    for (Direction side : SIDES) {
                        collectBlock(level, pos, side, tileChemicals);
                    }

                    found.addAll(tileChemicals);

                    if (tileChemicals.isEmpty()
                            || !tile.getActive()
                            || !holdsActivatedCrystal(level, pos)) {
                        continue;
                    }

                    if (tileChemicals.contains(
                            AtomicGases.RHENIUM.get()
                    )) {
                        greatMultiplication = true;
                    }

                    if (tileChemicals.contains(
                            MekanismChemicals.HYDROGEN_CHLORIDE.get()
                    )) {
                        easyPath = true;
                    }
                }
            }
        }

        if (found.contains(AtomicGases.NIOBIUM.get())) {
            AtomicAdvancements.grant(
                    player,
                    AtomicAdvancements.GAS_NIOBIUM
            );
        }

        if (found.contains(AtomicGases.GERMANIUM.get())) {
            AtomicAdvancements.grant(
                    player,
                    AtomicAdvancements.GAS_GERMANIUM
            );
        }

        if (found.contains(AtomicGases.COPPER.get())) {
            AtomicAdvancements.grant(
                    player,
                    AtomicAdvancements.GAS_COPPER
            );
        }

        if (found.contains(AtomicGases.PALLADIUM.get())) {
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
            Set<Chemical> found
    ) {
        Inventory inventory = player.getInventory();

        for (ItemStack stack : inventory.items) {
            collectItem(stack, found);
        }

        for (ItemStack stack : inventory.offhand) {
            collectItem(stack, found);
        }
    }

    private static void collectItem(
            ItemStack stack,
            Set<Chemical> out
    ) {
        if (stack.isEmpty()) {
            return;
        }

        addAll(
                stack.getCapability(Capabilities.CHEMICAL.item()),
                out
        );
    }

    private static void collectBlock(
            ServerLevel level,
            BlockPos pos,
            @Nullable Direction side,
            Set<Chemical> out
    ) {
        addAll(
                level.getCapability(
                        Capabilities.CHEMICAL.block(),
                        pos,
                        side
                ),
                out
        );
    }

    private static void addAll(
            @Nullable IChemicalHandler handler,
            Set<Chemical> out
    ) {
        if (handler == null) {
            return;
        }

        for (int tank = 0; tank < handler.getChemicalTanks(); tank++) {
            ChemicalStack stack = handler.getChemicalInTank(tank);

            if (!stack.isEmpty()) {
                out.add(stack.getChemical());
            }
        }
    }

    private static boolean holdsActivatedCrystal(
            ServerLevel level,
            BlockPos pos
    ) {
        for (Direction side : SIDES) {
            IItemHandler handler = level.getCapability(
                    net.neoforged.neoforge.capabilities.Capabilities
                            .ItemHandler.BLOCK,
                    pos,
                    side
            );

            if (handler == null) {
                continue;
            }

            for (int slot = 0; slot < handler.getSlots(); slot++) {
                if (handler.getStackInSlot(slot).getItem()
                        instanceof ActivatedCrystalItem) {
                    return true;
                }
            }
        }

        return false;
    }
}
