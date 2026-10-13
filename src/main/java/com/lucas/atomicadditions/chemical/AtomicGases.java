package com.lucas.atomicadditions.chemical;

import com.lucas.atomicadditions.AtomicAdditions;
import mekanism.api.chemical.Chemical;
import mekanism.common.registration.impl.ChemicalDeferredRegister;
import mekanism.common.registration.impl.DeferredChemical;

public class AtomicGases {

    private AtomicGases() {
    }

    public static final ChemicalDeferredRegister GASES =
            new ChemicalDeferredRegister(AtomicAdditions.MODID);

    public static final DeferredChemical<Chemical> NIOBIUM =
            GASES.register("niobium", 0xC9A227);

    public static final DeferredChemical<Chemical> GERMANIUM =
            GASES.register("germanium", 0xe01122);

    public static final DeferredChemical<Chemical> TANTALUM =
            GASES.register("tantalum", 0x005c23);

    public static final DeferredChemical<Chemical> PALLADIUM =
            GASES.register("palladium", 0x294366);

    public static final DeferredChemical<Chemical> COPPER =
            GASES.register("copper", 0xD47A4A);

    public static final DeferredChemical<Chemical> RHENIUM =
            GASES.register("rhenium", 0xFF8C00);
}
