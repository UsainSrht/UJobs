package me.usainsrht.ujobs.models;

import lombok.Getter;
import me.usainsrht.ujobs.UJobsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.Locale;

@Getter
public class JobInfoLine {

    final Action action;
    final String value;
    final Job.ActionReward reward;
    final Component actionValue;

    public JobInfoLine(Action action, String value, Job.ActionReward reward) {
        this.action = action;
        this.value = value;
        this.reward = reward;
        String line = UJobsPlugin.instance.getConfig().getString("actions." + action.getName().toLowerCase(Locale.ROOT));
        Component parsed;
        if (action instanceof EntityAction) {
            EntityType entityType = EntityType.valueOf(value.toUpperCase(Locale.ROOT));
            parsed = Component.translatable(entityType);
        } else if (action instanceof MaterialAction) {
            String numbersRemoved = value.replaceAll("\\d+", "");
            Material material = Material.matchMaterial(numbersRemoved);
            if (material != null) parsed = Component.translatable(material);
            else parsed = Component.text(value);
        } else if (action instanceof SpecialAction) {
            String configValue = UJobsPlugin.instance.getConfig().getString("special_values." + value);
            if (configValue == null) parsed = Component.text(value);
            else parsed = UJobsPlugin.instance.getMiniMessage().deserialize(configValue);
        } else parsed = Component.text(value);
        this.actionValue = UJobsPlugin.instance.getMiniMessage().deserialize(line, Placeholder.component("value", parsed));
    }

}
