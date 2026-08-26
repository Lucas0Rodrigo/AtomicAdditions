package com.lucas.atomicadditions;

import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.registration.impl.ContainerTypeDeferredRegister;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;

public class AtomicContainerTypes {

    private AtomicContainerTypes() {
    }

    public static final ContainerTypeDeferredRegister CONTAINER_TYPES =
            new ContainerTypeDeferredRegister(AtomicAdditions.MODID);

    public static final ContainerTypeRegistryObject<
            MekanismTileContainer<AtomicCasingBlockEntity>
            > ATOMIC =
            CONTAINER_TYPES.custom(
                    "atomic",
                    AtomicCasingBlockEntity.class
            ).offset(0, 16).build();
}