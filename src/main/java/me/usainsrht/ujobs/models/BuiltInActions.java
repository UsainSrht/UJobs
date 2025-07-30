package me.usainsrht.ujobs.models;

import java.util.Locale;

public enum BuiltInActions implements Action {

    KILL("kill"),
    BREAK("break"),
    PLACE("place"),
    BREED("breed"),
    TAME("tame"),
    FISH("fish"),
    ENCHANT("enchant"),
    ANVIL_ENCHANT("anvil_enchant"),
    GENERATE_LOOT("generate_loot"),
    RAID("raid"),
    CRAFT("craft"),
    TRADE("trade");

    final String name;

    BuiltInActions(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public static Action get(String name) {
        return valueOf(name.toUpperCase(Locale.ROOT));
    }
}
