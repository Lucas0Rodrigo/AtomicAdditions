package com.lucas.atomicadditions.multiblock;

import com.lucas.atomicadditions.AtomicAdditions;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class AtomicReactorSound
        extends AbstractTickableSoundInstance {

    private static final Map<BlockPos, AtomicReactorSound> ACTIVE_SOUNDS =
            new HashMap<>();

    private final BlockPos pos;

    private AtomicReactorSound(
            BlockPos pos
    ) {
        super(
                AtomicAdditions.AMR_SOUND.get(),
                SoundSource.BLOCKS,
                RandomSource.create()
        );

        this.pos = pos;

        this.x = pos.getX() + 0.5D;
        this.y = pos.getY() + 0.5D;
        this.z = pos.getZ() + 0.5D;

        this.volume = 1.0F;
        this.pitch = 1.0F;

        this.looping = true;
        this.delay = 0;

        this.attenuation =
                SoundInstance.Attenuation.LINEAR;
    }

    public static void update(
            AtomicCasingBlockEntity tile
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        if (!tile.isMaster()) {
            return;
        }

        AtomicMultiblockData multiblock =
                tile.getMultiblock();

        BlockPos pos =
                tile.getBlockPos();

        boolean active =
                multiblock.isFormed()
                        && multiblock.renderEnergy > 0;

        AtomicReactorSound sound =
                ACTIVE_SOUNDS.get(pos);

        if (active) {
            if (sound == null
                    || !minecraft
                    .getSoundManager()
                    .isActive(sound)) {

                if (sound != null) {
                    sound.stop();
                }

                sound =
                        new AtomicReactorSound(pos);

                ACTIVE_SOUNDS.put(
                        pos,
                        sound
                );

                minecraft
                        .getSoundManager()
                        .play(sound);
            }
        } else {
            stop(pos);
        }
    }

    private static void stop(
            BlockPos pos
    ) {
        AtomicReactorSound sound =
                ACTIVE_SOUNDS.remove(pos);

        if (sound != null) {
            Minecraft.getInstance()
                    .getSoundManager()
                    .stop(sound);
        }
    }

    @Override
    public void tick() {
        Level level =
                Minecraft.getInstance().level;

        if (level == null) {
            removeAndStop();
            return;
        }

        BlockEntity blockEntity =
                level.getBlockEntity(pos);

        if (!(blockEntity
                instanceof AtomicCasingBlockEntity tile)) {

            removeAndStop();
            return;
        }

        if (!tile.isMaster()) {
            removeAndStop();
            return;
        }

        AtomicMultiblockData multiblock =
                tile.getMultiblock();

        if (!multiblock.isFormed()
                || multiblock.renderEnergy <= 0) {

            removeAndStop();
        }
    }

    private void removeAndStop() {
        ACTIVE_SOUNDS.remove(
                pos,
                this
        );

        stop();
    }
}