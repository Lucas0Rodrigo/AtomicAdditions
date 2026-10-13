package com.lucas.atomicadditions.recipes;

import net.neoforged.neoforge.event.AddReloadListenerEvent;

public class AtomicRecipes {

    public static final AtomicRecipeManager AMR_RECIPES =
            new AtomicRecipeManager();

    private AtomicRecipes() {
    }

    public static void addReloadListener(
            AddReloadListenerEvent event
    ) {
        event.addListener(AMR_RECIPES);
    }
}