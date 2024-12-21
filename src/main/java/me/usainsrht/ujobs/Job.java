package me.usainsrht.ujobs;

import dev.dejvokep.boostedyaml.block.implementation.Section;
import lombok.Getter;
import me.usainsrht.ujobs.reward.Reward;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

import java.util.*;

@Getter
public class Job {

    String name;
    Component displayName;
    ItemStack icon;
    Collection<Sound> levelUpSounds;
    String levelUpEquation;
    int maxLevel;
    String bossBarTitle;
    BossBar.Overlay bossBarOverlay;
    BossBar.Color bossBarColor;
    Map<Action, Map<String, List<Reward>>> rewards;

    public Job(String name, Section config) {
        this.name = name;
        displayName = UJobs.miniMessage.deserialize(config.getString("name"));
        //icon =
        //levelUpSounds
        bossBarTitle = config.getString("bossbar.title");
        bossBarOverlay = BossBar.Overlay.valueOf(config.getString("bossbar.overlay").toUpperCase(Locale.ROOT));
        bossBarColor = BossBar.Color.valueOf(config.getString("bossbar.color").toUpperCase(Locale.ROOT));

        rewards = new HashMap<>();
        if (config.isSection("rewards")) {
            Section rewardsSection = config.getSection("rewards");
            for (String actionStr : rewardsSection.getRoutesAsStrings(false)) {
                Action action = Action.valueOf(actionStr);
                Map<String, List<Reward>> actionRewards = new HashMap<>();
                Section actionSection = rewardsSection.getSection(actionStr);
                for (String value : actionSection.getRoutesAsStrings(false)) {

                    actionRewards.put(value, );
                }
                rewards.put(action, actionRewards);
            }
        }
    }

}
