package me.usainsrht.ujobs.models;

import java.util.Locale;

public class CustomAction implements Action {

    private final String name;

    public CustomAction(String name) {
        this.name = name != null ? name.toLowerCase(Locale.ROOT) : "";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof Action other)) return false;
        return name.equalsIgnoreCase(other.getName());
    }

    @Override
    public int hashCode() {
        return name.toLowerCase(Locale.ROOT).hashCode();
    }

    @Override
    public String toString() {
        return "Action[" + name + "]";
    }
}
