package me.usainsrht.ujobs.models;

public interface Action {

    String getName();

    static Action of(String name) {
        return ActionRegistry.getOrCreate(name);
    }

    static Action custom(String name) {
        return ActionRegistry.getOrCreate(name);
    }

}

