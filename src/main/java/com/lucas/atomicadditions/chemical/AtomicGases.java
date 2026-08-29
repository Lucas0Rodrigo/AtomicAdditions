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
            GASES.register("niobium", 0xFFD700);

    public static final GasRegistryObject<Gas> GERMANIUM =
            GASES.register("germanium", 0x9B111E);

    // Saída da Receita 1
    public static final GasRegistryObject<Gas> TANTALUM =
            GASES.register("tantalum", 0x00A86B);

    // Entradas da Receita 2
    public static final GasRegistryObject<Gas> PALLADIUM =
            GASES.register("palladium", 0x0047AB);

    public static final GasRegistryObject<Gas> COPPER =
            GASES.register("copper", 0xD47A4A);

    // Saída da Receita 2
    public static final GasRegistryObject<Gas> RHENIUM =
            GASES.register("rhenium", 0xFF8C00);
}