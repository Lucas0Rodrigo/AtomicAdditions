package com.lucas.atomicadditions.advancement;

import com.google.gson.JsonObject;
import com.lucas.atomicadditions.AtomicAdditions;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;

/**
 * Gatilho genérico do mod: dispara um "marco" identificado por texto.
 * Uso no JSON da conquista:
 * {"trigger": "atomicadditions:milestone", "conditions": {"milestone": "tantalum"}}
 */
public class AtomicTrigger
        extends SimpleCriterionTrigger<AtomicTrigger.Instance> {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(
                    AtomicAdditions.MODID,
                    "milestone"
            );

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected Instance createInstance(
            JsonObject json,
            ContextAwarePredicate player,
            DeserializationContext context
    ) {
        return new Instance(
                player,
                GsonHelper.getAsString(json, "milestone")
        );
    }

    public void trigger(
            ServerPlayer player,
            String milestone
    ) {
        trigger(
                player,
                instance -> instance.milestone.equals(milestone)
        );
    }

    public static class Instance
            extends AbstractCriterionTriggerInstance {

        private final String milestone;

        public Instance(
                ContextAwarePredicate player,
                String milestone
        ) {
            super(ID, player);
            this.milestone = milestone;
        }

        @Override
        public JsonObject serializeToJson(
                SerializationContext context
        ) {
            JsonObject json = super.serializeToJson(context);
            json.addProperty("milestone", milestone);
            return json;
        }
    }
}
