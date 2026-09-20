package com.lucas.atomicadditions.multiblock;

import com.lucas.atomicadditions.AtomicAdditions;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import mekanism.common.config.MekanismConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = AtomicAdditions.MODID,
        value = Dist.CLIENT
)
public final class AtomicSoundHandler {

    private static final int FADE_TICKS = 40;

    private static final Long2ObjectMap<AtomicReactorSound> SOUND_MAP =
            new Long2ObjectOpenHashMap<>();

    private AtomicSoundHandler() {
    }

    public static void updateTileSound(
            BlockPos pos,
            boolean shouldPlay
    ) {
        long key =
                pos.asLong();

        AtomicReactorSound sound =
                SOUND_MAP.get(key);

        if (shouldPlay) {
            if (sound == null || sound.isStopped()) {
                sound =
                        new AtomicReactorSound(
                                pos
                        );

                SOUND_MAP.put(
                        key,
                        sound
                );

                Minecraft.getInstance()
                        .getSoundManager()
                        .play(sound);
            }

            sound.fadeIn();

        } else if (sound != null) {
            sound.fadeOut();
        }
    }

    public static void stopTileSound(
            BlockPos pos
    ) {
        AtomicReactorSound sound =
                SOUND_MAP.get(
                        pos.asLong()
                );

        if (sound != null) {
            sound.fadeOut();
        }
    }

    private static class AtomicReactorSound
            extends AbstractTickableSoundInstance {

        private final float baseVolume;

        private float fadeProgress;

        private boolean fadingOut;

        private AtomicReactorSound(
                BlockPos pos
        ) {
            super(
                    AtomicAdditions.AMR_SOUND.get(),
                    SoundSource.BLOCKS,
                    RandomSource.create()
            );

            this.baseVolume =
                    MekanismConfig.client.baseSoundVolume.get();

            this.fadeProgress = 0.0F;

            this.fadingOut = false;

            this.looping = true;

            this.delay = 0;

            this.x =
                    pos.getX() + 0.5F;

            this.y =
                    pos.getY() + 0.5F;

            this.z =
                    pos.getZ() + 0.5F;

            this.volume = 0.0F;
        }

        private void fadeIn() {
            fadingOut = false;
        }

        private void fadeOut() {
            fadingOut = true;
        }

        @Override
        public void tick() {
            if (fadingOut) {
                fadeProgress -=
                        1.0F / FADE_TICKS;
            } else {
                fadeProgress +=
                        1.0F / FADE_TICKS;
            }

            fadeProgress =
                    Mth.clamp(
                            fadeProgress,
                            0.0F,
                            1.0F
                    );

            float smoothProgress =
                    fadeProgress
                            * fadeProgress
                            * (3.0F - 2.0F * fadeProgress);

            volume =
                    baseVolume
                            * smoothProgress;

            if (fadingOut
                    && fadeProgress <= 0.0F) {

                volume = 0.0F;

                stop();
            }
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }
}