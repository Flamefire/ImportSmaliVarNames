package com.flamefire.importsmalinames.refactoring;

public class LocalVar{
    public final String name;
    public final String type;

    public LocalVar(String name, String type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString() {
        return type + " " + name;
    }
}