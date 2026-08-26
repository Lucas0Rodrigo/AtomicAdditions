package com.lucas.atomicadditions.recipes;

import mekanism.api.chemical.gas.Gas;

public class AtomicAMRRecipe {

    private final Gas input1;
    private final long input1Amount;

    private final Gas input2;
    private final long input2Amount;

    private final Gas output;
    private final long outputAmount;

    private final long energyPerTick;
    private final int duration;

    public AtomicAMRRecipe(
            Gas input1,
            long input1Amount,
            Gas input2,
            long input2Amount,
            Gas output,
            long outputAmount,
            long energyPerTick,
            int duration
    ) {
        this.input1 = input1;
        this.input1Amount = input1Amount;

        this.input2 = input2;
        this.input2Amount = input2Amount;

        this.output = output;
        this.outputAmount = outputAmount;

        this.energyPerTick = energyPerTick;
        this.duration = duration;
    }

    public Gas getInput1() {
        return input1;
    }

    public long getInput1Amount() {
        return input1Amount;
    }

    public Gas getInput2() {
        return input2;
    }

    public long getInput2Amount() {
        return input2Amount;
    }

    public Gas getOutput() {
        return output;
    }

    public long getOutputAmount() {
        return outputAmount;
    }

    public long getEnergyPerTick() {
        return energyPerTick;
    }

    public int getDuration() {
        return duration;
    }

    public boolean matches(
            Gas gas1,
            long amount1,
            Gas gas2,
            long amount2
    ) {
        if (gas1 == null || gas2 == null) {
            return false;
        }

        boolean normalOrder =
                gas1 == input1 &&
                        amount1 >= input1Amount &&
                        gas2 == input2 &&
                        amount2 >= input2Amount;

        boolean reversedOrder =
                gas1 == input2 &&
                        amount1 >= input2Amount &&
                        gas2 == input1 &&
                        amount2 >= input1Amount;

        return normalOrder || reversedOrder;
    }
}