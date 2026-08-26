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
            GASES.register("niobium", 0x8A8A8A);

    public static final GasRegistryObject<Gas> GERMANIUM =
            GASES.register("germanium", 0xA6A6A6);

    // Saída da Receita 1
    public static final GasRegistryObject<Gas> TANTALUM =
            GASES.register("tantalum", 0x6F8FAF);

    // Entradas da Receita 2
    public static final GasRegistryObject<Gas> PALLADIUM =
            GASES.register("palladium", 0xB8B8B8);

    public static final GasRegistryObject<Gas> COPPER =
            GASES.register("copper", 0xD47A4A);

    // Saída da Receita 2
    public static final GasRegistryObject<Gas> RHENIUM =
            GASES.register("rhenium", 0x7F9AA8);
}