package me.usainsrht.ujobs.models;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ActionRegistry {

    private static final Map<String, Action> ACTIONS = new ConcurrentHashMap<>();

    static {
        for (BuiltInActions.Entity action : BuiltInActions.Entity.values()) {
            register(action);
        }
        for (BuiltInActions.Material action : BuiltInActions.Material.values()) {
            register(action);
        }
        for (BuiltInActions.Special action : BuiltInActions.Special.values()) {
            register(action);
        }
    }

    public static Action register(Action action) {
        if (action == null || action.getName() == null) {
            return null;
        }
        ACTIONS.put(action.getName().toLowerCase(Locale.ROOT), action);
        return action;
    }

    public static Action get(String name) {
        if (name == null) {
            return null;
        }
        return ACTIONS.get(name.toLowerCase(Locale.ROOT));
    }

    public static Action getOrCreate(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return ACTIONS.computeIfAbsent(name.toLowerCase(Locale.ROOT), CustomAction::new);
    }

    public static Collection<Action> getAll() {
        return Collections.unmodifiableCollection(ACTIONS.values());
    }

    public static boolean isRegistered(String name) {
        if (name == null) {
            return false;
        }
        return ACTIONS.containsKey(name.toLowerCase(Locale.ROOT));
    }
}
