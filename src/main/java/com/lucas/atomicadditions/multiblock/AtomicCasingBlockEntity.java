package com.lucas.atomicadditions.multiblock;

import com.lucas.atomicadditions.AtomicAdditions;
import mekanism.api.NBTConstants;
import mekanism.api.chemical.gas.IGasTank;
import mekanism.api.providers.IBlockProvider;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.chemical.SyncableGasStack;
import mekanism.common.lib.multiblock.MultiblockManager;
import mekanism.common.tile.prefab.TileEntityMultiblock;
import mekanism.common.util.NBTUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
                new AtomicBlockProvider(state.getBlock()),
                pos,
                state
        );
    }

    protected AtomicCasingBlockEntity(
            IBlockProvider provider,
            BlockPos pos,
            BlockState state
    ) {
        super(provider, pos, state);
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
                handleSound
        );
    }

    @Override
    protected boolean canPlaySound() {
        return false;
    }

    @NotNull
    @Override
    public CompoundTag getReducedUpdateTag() {
        CompoundTag updateTag =
                super.getReducedUpdateTag();

        AtomicMultiblockData multiblock =
                getMultiblock();

        updateTag.putBoolean(
                NBTConstants.HANDLE_SOUND,
                isMaster()
                        && multiblock.isFormed()
                        && multiblock.lastProcessed > 0
        );

        return updateTag;
    }

    @Override
    public void handleUpdateTag(
            @NotNull CompoundTag tag
    ) {
        super.handleUpdateTag(tag);

        NBTUtils.setBooleanIfPresent(
                tag,
                NBTConstants.HANDLE_SOUND,
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

            for (IGasTank gasTank :
                    getMultiblock().getGasTanks(null)) {

                container.track(
                        SyncableGasStack.create(
                                gasTank,
                                isClient
                        )
                );
            }
        }
    }
}