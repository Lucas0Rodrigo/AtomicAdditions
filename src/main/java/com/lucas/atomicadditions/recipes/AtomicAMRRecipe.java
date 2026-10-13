package com.lucas.atomicadditions.recipes;

import com.lucas.atomicadditions.AtomicAdditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mekanism.api.chemical.Chemical;
import net.minecraft.resources.ResourceLocation;

public class AtomicAMRRecipe {

    public static final Codec<AtomicAMRRecipe> CODEC =
            RecordCodecBuilder.create(
                    instance -> instance.group(
                            Chemical.CODEC.fieldOf("input1").forGetter(
                                    AtomicAMRRecipe::getInput1
                            ),
                            Codec.LONG.fieldOf("input1_amount").forGetter(
                                    AtomicAMRRecipe::getInput1Amount
                            ),
                            Chemical.CODEC.fieldOf("input2").forGetter(
                                    AtomicAMRRecipe::getInput2
                            ),
                            Codec.LONG.fieldOf("input2_amount").forGetter(
                                    AtomicAMRRecipe::getInput2Amount
                            ),
                            Chemical.CODEC.fieldOf("output").forGetter(
                                    AtomicAMRRecipe::getOutput
                            ),
                            Codec.LONG.fieldOf("output_amount").forGetter(
                                    AtomicAMRRecipe::getOutputAmount
                            ),
                            Codec.LONG.fieldOf("energy_per_tick").forGetter(
                                    AtomicAMRRecipe::getEnergyPerTick
                            ),
                            Codec.INT.fieldOf("duration").forGetter(
                                    AtomicAMRRecipe::getDuration
                            )
                    ).apply(
                            instance,
                            AtomicAMRRecipe::new
                    )
            );

    public ResourceLocation getRegistryName() {
        return ResourceLocation.fromNamespaceAndPath(
                AtomicAdditions.MODID,
                "amr/"
                        + output.getRegistryName().getPath()
                        + "/"
                        + input1.getRegistryName().getPath()
                        + "_"
                        + input2.getRegistryName().getPath()
        );
    }

    private final Chemical input1;
    private final long input1Amount;

    private final Chemical input2;
    private final long input2Amount;

    private final Chemical output;
    private final long outputAmount;

    private final long energyPerTick;
    private final int duration;

    public AtomicAMRRecipe(
            Chemical input1,
            long input1Amount,
            Chemical input2,
            long input2Amount,
            Chemical output,
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

    public Chemical getInput1() {
        return input1;
    }

    public long getInput1Amount() {
        return input1Amount;
    }

    public Chemical getInput2() {
        return input2;
    }

    public long getInput2Amount() {
        return input2Amount;
    }

    public Chemical getOutput() {
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
            Chemical gas1,
            long amount1,
            Chemical gas2,
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