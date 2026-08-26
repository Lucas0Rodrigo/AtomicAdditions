package com.lucas.atomicadditions.multiblock;

import net.minecraft.util.StringRepresentable;

public enum PortMode implements StringRepresentable {

    INPUT("input"),
    OUTPUT("output");

    private final String name;

    PortMode(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}