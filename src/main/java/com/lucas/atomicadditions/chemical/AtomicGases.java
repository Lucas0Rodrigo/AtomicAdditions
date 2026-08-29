package com.lucas.atomicadditions.chemical;

import com.lucas.atomicadditions.AtomicAdditions;
import mekanism.api.chemical.gas.Gas;
import mekanism.common.registration.impl.GasDeferredRegister;
import mekanism.common.registration.impl.GasRegistryObject;

public class AtomicGases {

    private AtomicGases() {
    }

    public static final GasDeferredRegister GASES =
            new GasDeferredRegister(AtomicAdditions.MODID);

    // Entradas da Receita 1
    public static final GasRegistryObject<Gas> NIOBIUM =
            GASES.register("niobium", 0xC9A227);

    public static final GasRegistryObject<Gas> GERMANIUM =
            GASES.register("germanium", 0xe01122);

    // Saída da Receita 1
    public static final GasRegistryObject<Gas> TANTALUM =
            GASES.register("tantalum", 0x005c23);

    // Entradas da Receita 2
    public static final GasRegistryObject<Gas> PALLADIUM =
            GASES.register("palladium", 0x294366);

    public static final GasRegistryObject<Gas> COPPER =
            GASES.register("copper", 0xD47A4A);

    // Saída da Receita 2
    public static final GasRegistryObject<Gas> RHENIUM =
            GASES.register("rhenium", 0xFF8C00);
}