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
            boolean shouldPlay,
            double processRate
    ) {
        long key =
                pos.asLong();

        AtomicReactorSound sound =
                SOUND_MAP.get(key);

        if (shouldPlay) {
            if (sound == null || sound.isStopped()) {
                sound =
                        new AtomicReactorSound(
                                key,
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

            sound.updateProcessRate(
                    processRate
            );

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

    private static float getPitchForRate(
            double processRate
    ) {
        if (processRate <= 0) {
            return 1.0F;
        }

        /*
         * The AMR has different maximum rates depending on the recipe.
         *
         * Tantalum:
         * 1000 mB per recipe / 200 ticks
         * with the current energy capacity:
         * 800 mB/t maximum
         *
         * Rhenium:
         * 1000 mB per recipe / 400 ticks
         * with the current energy capacity:
         * 400 mB/t maximum
         *
         * We therefore use the current rate itself to select
         * three practical audio bands.
         */

        if (processRate < 200) {
            return 1.0F;
        }

        if (processRate < 500) {
            return 1.5F;
        }

        return 2.0F;
    }

    private static class AtomicReactorSound
            extends AbstractTickableSoundInstance {

        private final long mapKey;

        private final float baseVolume;

        private float fadeProgress;

        private double processRate;

        private boolean fadingOut;

        private AtomicReactorSound(
                long mapKey,
                BlockPos pos
        ) {
            super(
                    AtomicAdditions.AMR_SOUND.get(),
                    SoundSource.BLOCKS,
                    RandomSource.create()
            );

            this.mapKey =
                    mapKey;

            this.baseVolume =
                    MekanismConfig.client.baseSoundVolume.get();

            this.fadeProgress =
                    0.0F;

            this.processRate =
                    0.0D;

            this.fadingOut =
                    false;

            this.looping =
                    true;

            this.delay =
                    0;

            this.x =
                    pos.getX() + 0.5F;

            this.y =
                    pos.getY() + 0.5F;

            this.z =
                    pos.getZ() + 0.5F;

            this.volume =
                    0.0F;

            this.pitch =
                    1.0F;
        }

        private void updateProcessRate(
                double processRate
        ) {
            this.processRate =
                    Math.max(
                            0.0D,
                            processRate
                    );
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
                            * (
                            3.0F
                                    - 2.0F
                                    * fadeProgress
                    );

            volume =
                    baseVolume
                            * smoothProgress;

            pitch =
                    getPitchForRate(
                            processRate
                    );

            if (fadingOut
                    && fadeProgress <= 0.0F) {

                volume =
                        0.0F;

                stop();

                SOUND_MAP.remove(
                        mapKey
                );
            }
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }
}