package com.lucas.atomicadditions;

import mekanism.common.inventory.container.tile.EmptyTileContainer;
import mekanism.common.registration.impl.ContainerTypeDeferredRegister;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;

public class AtomicContainerTypes {

    private AtomicContainerTypes() {
    }

    public static final ContainerTypeDeferredRegister CONTAINER_TYPES =
            new ContainerTypeDeferredRegister(AtomicAdditions.MODID);

    public static final ContainerTypeRegistryObject<
            EmptyTileContainer<AtomicCasingBlockEntity>
            > ATOMIC =
            CONTAINER_TYPES.registerEmpty(
                    "atomic",
                    AtomicCasingBlockEntity.class
            );
}