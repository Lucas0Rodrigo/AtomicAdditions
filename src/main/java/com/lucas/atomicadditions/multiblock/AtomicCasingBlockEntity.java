package com.lucas.atomicadditions.multiblock;

import com.lucas.atomicadditions.AtomicAdditions;
import mekanism.api.SerializationConstants;
import mekanism.api.chemical.IChemicalTank;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.chemical.SyncableChemicalStack;
import mekanism.common.lib.multiblock.MultiblockManager;
import mekanism.common.tile.prefab.TileEntityMultiblock;
import mekanism.common.util.NBTUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class AtomicCasingBlockEntity
        extends TileEntityMultiblock<AtomicMultiblockData> {

    private boolean handleSound;
    private boolean prevActive;

    public AtomicCasingBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        this(
                state.getBlockHolder(),
                pos,
                state
        );
    }

    protected AtomicCasingBlockEntity(
            Holder<Block> blockHolder,
            BlockPos pos,
            BlockState state
    ) {
        super(blockHolder, pos, state);
    }

    @Override
    protected boolean onUpdateServer(
            AtomicMultiblockData multiblock
    ) {
        boolean needsPacket =
                super.onUpdateServer(multiblock);

        boolean active =
                isMaster()
                        && multiblock.isFormed()
                        && multiblock.lastProcessed > 0;

        if (active != prevActive) {
            prevActive = active;
            needsPacket = true;
        }

        return needsPacket;
    }

    @Override
    protected void onUpdateClient() {
        super.onUpdateClient();

        AtomicSoundHandler.updateTileSound(
                getBlockPos(),
                handleSound,
                getMultiblock().renderProcessRate
        );
    }

    @Override
    protected boolean canPlaySound() {
        return false;
    }

    @NotNull
    @Override
    public CompoundTag getReducedUpdateTag(
            @NotNull HolderLookup.Provider provider
    ) {
        CompoundTag updateTag =
                super.getReducedUpdateTag(provider);

        AtomicMultiblockData multiblock =
                getMultiblock();

        updateTag.putBoolean(
                SerializationConstants.HANDLE_SOUND,
                isMaster()
                        && multiblock.isFormed()
                        && multiblock.lastProcessed > 0
        );

        return updateTag;
    }

    @Override
    public void handleUpdateTag(
            @NotNull CompoundTag tag,
            @NotNull HolderLookup.Provider provider
    ) {
        super.handleUpdateTag(tag, provider);

        NBTUtils.setBooleanIfPresent(
                tag,
                SerializationConstants.HANDLE_SOUND,
                value -> handleSound = value
        );
    }

    @Override
    public void setRemoved() {
        if (isRemote()) {
            AtomicSoundHandler.stopTileSound(
                    getBlockPos()
            );
        }

        super.setRemoved();
    }

    @Override
    public AtomicMultiblockData createMultiblock() {
        return new AtomicMultiblockData(this);
    }

    @Override
    public MultiblockManager<AtomicMultiblockData> getManager() {
        return AtomicAdditions.ATOMIC_MANAGER;
    }

    @Override
    public void addContainerTrackers(
            MekanismContainer container
    ) {
        super.addContainerTrackers(container);

        if (!(this instanceof AtomicPortBlockEntity)) {
            boolean isClient = isRemote();

            for (IChemicalTank chemicalTank :
                    getMultiblock().getChemicalTanks(null)) {

                container.track(
                        SyncableChemicalStack.create(
                                chemicalTank,
                                isClient
                        )
                );
            }
        }
    }
}