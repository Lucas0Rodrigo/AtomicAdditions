package com.lucas.atomicadditions.multiblock;

import com.lucas.atomicadditions.AtomicAdditions;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = AtomicAdditions.MODID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class AtomicSoundHandler {

    private static final Long2ObjectMap<SoundInstance> SOUND_MAP =
            new Long2ObjectOpenHashMap<>();

    private static final ResourceLocation AMR_SOUND_LOCATION =
            ResourceLocation.fromNamespaceAndPath(
                    AtomicAdditions.MODID,
                    "tile.machine.amr"
            );

    private AtomicSoundHandler() {
    }

    @SubscribeEvent
    public static void onSoundPlay(
            PlaySoundEvent event
    ) {
        SoundInstance sound =
                event.getSound();

        if (sound == null) {
            return;
        }

        ResourceLocation soundLocation =
                event.getOriginalSound().getLocation();

        if (!AMR_SOUND_LOCATION.equals(soundLocation)) {
            return;
        }

        BlockPos pos =
                BlockPos.containing(
                        sound.getX() - 0.5,
                        sound.getY() - 0.5,
                        sound.getZ() - 0.5
                );

        SOUND_MAP.put(
                pos.asLong(),
                sound
        );
    }

    public static void stopTileSound(
            BlockPos pos
    ) {
        long key =
                pos.asLong();

        SoundInstance sound =
                SOUND_MAP.remove(key);

        if (sound != null) {
            Minecraft.getInstance()
                    .getSoundManager()
                    .stop(sound);
        }
    }
}