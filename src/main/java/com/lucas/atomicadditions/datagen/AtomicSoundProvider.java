package com.lucas.atomicadditions.datagen;

import com.lucas.atomicadditions.AtomicAdditions;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.SoundDefinitionsProvider;

public class AtomicSoundProvider
        extends SoundDefinitionsProvider {

    public AtomicSoundProvider(
            PackOutput output,
            ExistingFileHelper existingFileHelper
    ) {
        super(
                output,
                AtomicAdditions.MODID,
                existingFileHelper
        );
    }

    @Override
    public void registerSounds() {
        add(
                AtomicAdditions.AMR_SOUND.get(),
                definition().with(
                        sound("amr")
                                .stream()
                                .attenuationDistance(16)
                )
        );
    }
}