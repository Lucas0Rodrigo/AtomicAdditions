package com.lucas.atomicadditions.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

/**
 * Gatilho genérico do mod: dispara um "marco" identificado por texto.
 * Uso no JSON da conquista:
 * {"trigger": "atomicadditions:milestone", "conditions": {"milestone": "tantalum"}}
 */
public class AtomicTrigger
        extends SimpleCriterionTrigger<AtomicTrigger.Instance> {

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(
            ServerPlayer player,
            String milestone
    ) {
        trigger(
                player,
                instance -> instance.milestone().equals(milestone)
        );
    }

    public record Instance(
            Optional<ContextAwarePredicate> player,
            String milestone
    ) implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<Instance> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        EntityPredicate.ADVANCEMENT_CODEC
                                .optionalFieldOf("player")
                                .forGetter(Instance::player),
                        Codec.STRING
                                .fieldOf("milestone")
                                .forGetter(Instance::milestone)
                ).apply(instance, Instance::new));
    }
}
