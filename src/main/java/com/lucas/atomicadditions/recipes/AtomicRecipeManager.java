package com.lucas.atomicadditions.recipes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.lucas.atomicadditions.AtomicAdditions;
import com.lucas.atomicadditions.chemical.AtomicGases;
import mekanism.api.chemical.gas.Gas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AtomicRecipeManager extends SimpleJsonResourceReloadListener {

    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().create();

    private final List<AtomicAMRRecipe> recipes =
            new ArrayList<>();

    public AtomicRecipeManager() {
        super(GSON, "amr_recipes");
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> jsonFiles,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        recipes.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : jsonFiles.entrySet()) {

            ResourceLocation id = entry.getKey();

            if (!id.getNamespace().equals(AtomicAdditions.MODID)) {
                continue;
            }

            try {
                AtomicAMRRecipe recipe =
                        parseRecipe(
                                entry.getValue(),
                                id
                        );

                recipes.add(recipe);

                System.out.println(
                        "[Atomic Additions] AMR recipe carregada: "
                                + id
                );

            } catch (Exception exception) {

                System.err.println(
                        "[Atomic Additions] Erro ao carregar recipe AMR: "
                                + id
                );

                exception.printStackTrace();
            }
        }

        System.out.println(
                "[Atomic Additions] "
                        + recipes.size()
                        + " recipes do AMR carregadas."
        );
    }

    /**
     * Carrega as recipes diretamente dos recursos para o JEI.
     *
     * O JEI registra suas recipes antes do SimpleJsonResourceReloadListener
     * terminar o carregamento normal dos datapacks. Por isso o JEI utiliza
     * este método para ler os mesmos JSON diretamente dos recursos.
     */
    public List<AtomicAMRRecipe> getRecipesForJEI(
            ResourceManager resourceManager
    ) {
        List<AtomicAMRRecipe> jeiRecipes =
                new ArrayList<>();

        Map<ResourceLocation, Resource> resources =
                resourceManager.listResources(
                        "amr_recipes",
                        location ->
                                location.getNamespace().equals(
                                        AtomicAdditions.MODID
                                )
                                        && location.getPath().endsWith(".json")
                );

        for (Map.Entry<ResourceLocation, Resource> entry :
                resources.entrySet()) {

            ResourceLocation id = entry.getKey();

            try (
                    Reader reader =
                            entry.getValue().openAsReader()
            ) {
                JsonElement json =
                        GsonHelper.parse(
                                reader
                        );

                jeiRecipes.add(
                        parseRecipe(
                                json,
                                id
                        )
                );

            } catch (IOException | JsonSyntaxException exception) {

                System.err.println(
                        "[Atomic Additions] Erro ao carregar recipe AMR para JEI: "
                                + id
                );

                exception.printStackTrace();
            }
        }

        System.out.println(
                "[Atomic Additions] "
                        + jeiRecipes.size()
                        + " recipes do AMR carregadas para o JEI."
        );

        return Collections.unmodifiableList(
                jeiRecipes
        );
    }

    private AtomicAMRRecipe parseRecipe(
            JsonElement element,
            ResourceLocation id
    ) {
        JsonObject json =
                GsonHelper.convertToJsonObject(
                        element,
                        "AMR recipe"
                );

        Gas input1 =
                getGas(
                        GsonHelper.getAsString(
                                json,
                                "input1"
                        )
                );

        long input1Amount =
                GsonHelper.getAsLong(
                        json,
                        "input1_amount"
                );

        Gas input2 =
                getGas(
                        GsonHelper.getAsString(
                                json,
                                "input2"
                        )
                );

        long input2Amount =
                GsonHelper.getAsLong(
                        json,
                        "input2_amount"
                );

        Gas output =
                getGas(
                        GsonHelper.getAsString(
                                json,
                                "output"
                        )
                );

        long outputAmount =
                GsonHelper.getAsLong(
                        json,
                        "output_amount"
                );

        long energyPerTick =
                GsonHelper.getAsLong(
                        json,
                        "energy_per_tick"
                );

        int duration =
                GsonHelper.getAsInt(
                        json,
                        "duration"
                );

        if (input1Amount <= 0) {
            throw new JsonSyntaxException(
                    "input1_amount must be greater than zero"
            );
        }

        if (input2Amount <= 0) {
            throw new JsonSyntaxException(
                    "input2_amount must be greater than zero"
            );
        }

        if (outputAmount <= 0) {
            throw new JsonSyntaxException(
                    "output_amount must be greater than zero"
            );
        }

        if (energyPerTick <= 0) {
            throw new JsonSyntaxException(
                    "energy_per_tick must be greater than zero"
            );
        }

        if (duration <= 0) {
            throw new JsonSyntaxException(
                    "duration must be greater than zero"
            );
        }

        return new AtomicAMRRecipe(
                input1,
                input1Amount,
                input2,
                input2Amount,
                output,
                outputAmount,
                energyPerTick,
                duration
        );
    }

    private static Gas getGas(String id) {

        ResourceLocation location =
                ResourceLocation.tryParse(id);

        if (location == null) {
            throw new JsonSyntaxException(
                    "Invalid gas ID: " + id
            );
        }

        if (!location.getNamespace().equals(
                AtomicAdditions.MODID
        )) {
            throw new JsonSyntaxException(
                    "AMR currently only accepts Atomic Additions gases: "
                            + id
            );
        }

        String path =
                location.getPath();

        return switch (path) {

            case "niobium" ->
                    AtomicGases.NIOBIUM.get();

            case "germanium" ->
                    AtomicGases.GERMANIUM.get();

            case "tantalum" ->
                    AtomicGases.TANTALUM.get();

            case "palladium" ->
                    AtomicGases.PALLADIUM.get();

            case "copper" ->
                    AtomicGases.COPPER.get();

            case "rhenium" ->
                    AtomicGases.RHENIUM.get();

            default ->
                    throw new JsonSyntaxException(
                            "Unknown Atomic Additions gas: "
                                    + id
                    );
        };
    }

    public List<AtomicAMRRecipe> getRecipes() {
        return Collections.unmodifiableList(
                recipes
        );
    }

    public AtomicAMRRecipe findRecipe(
            Gas gas1,
            long amount1,
            Gas gas2,
            long amount2
    ) {
        for (AtomicAMRRecipe recipe : recipes) {

            if (recipe.matches(
                    gas1,
                    amount1,
                    gas2,
                    amount2
            )) {
                return recipe;
            }
        }

        return null;
    }
}