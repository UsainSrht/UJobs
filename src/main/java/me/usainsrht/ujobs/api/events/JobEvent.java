package me.usainsrht.ujobs.api.events;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;

public abstract class JobEvent extends Event {

    public JobEvent() {
        super(!Bukkit.isPrimaryThread());
    }

    public JobEvent(boolean isAsync) {
        super(isAsync);
    }

}
