package com.lucas.atomicadditions.multiblock;

import com.lucas.atomicadditions.AtomicAdditions;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.registration.impl.ContainerTypeDeferredRegister;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;

public class AtomicContainerTypes {

    private AtomicContainerTypes() {
    }

    public static final ContainerTypeDeferredRegister CONTAINER_TYPES =
            new ContainerTypeDeferredRegister(
                    AtomicAdditions.MODID
            );

    public static final ContainerTypeRegistryObject<
            MekanismTileContainer<AtomicCasingBlockEntity>
            > ATOMIC =
            CONTAINER_TYPES.register(
                    "atomic",
                    AtomicCasingBlockEntity.class
            );
}