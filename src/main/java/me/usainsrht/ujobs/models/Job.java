package me.usainsrht.ujobs.models;

import lombok.Getter;
import lombok.Setter;
import me.usainsrht.ujobs.utils.MathUtil;
import me.usainsrht.ujobs.yaml.YamlMessage;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentIteratorType;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class Job {
    private final String id;
    private Component name;
    private TextColor firstColor;
    private TextColor lastColor;

    @Setter
    private Material icon;

    @Setter
    private String levelEquation;

    @Setter
    private YamlMessage levelUpMessage;

    @Setter
    private List<String> levelUpCommands;

    @Setter
    private BossBarConfig bossBarConfig;

    private final Map<Action, Map<String, ActionReward>> actions;
    private final List<JobInfoLine> infoLines;

    public Job(String id, Component name, Material icon, String levelEquation,
               YamlMessage levelUpMessage, List<String> levelUpCommands, BossBarConfig bossBarConfig) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.levelEquation = levelEquation;
        this.levelUpMessage = levelUpMessage;
        this.levelUpCommands = levelUpCommands != null ? levelUpCommands : new ArrayList<>();
        this.bossBarConfig = bossBarConfig;
        this.actions = new ConcurrentHashMap<>();
        this.infoLines = Collections.synchronizedList(new ArrayList<>());
    }

    public void setName(Component name) {
        this.name = name;
        this.firstColor = null;
        this.lastColor = null;
    }

    public void update(Job otherJobInstance) {
        if (otherJobInstance == null || !this.id.equalsIgnoreCase(otherJobInstance.id)) return;

        setName(otherJobInstance.name);
        this.icon = otherJobInstance.icon;
        this.levelEquation = otherJobInstance.levelEquation;
        this.levelUpCommands = otherJobInstance.levelUpCommands;
        this.levelUpMessage = otherJobInstance.levelUpMessage;
        this.bossBarConfig = otherJobInstance.bossBarConfig;
        this.actions.clear();
        this.actions.putAll(otherJobInstance.actions);
        this.infoLines.clear();
        this.infoLines.addAll(otherJobInstance.infoLines);
    }

    public void setColors() {
        if (name == null) {
            this.firstColor = TextColor.color(0xFFFFFF);
            this.lastColor = TextColor.color(0xFFFFFF);
            return;
        }

        TextColor firstColor = null;
        TextColor lastColor = null;

        // Iterate through all parts of the component tree
        for (Component part : name.iterable(ComponentIteratorType.DEPTH_FIRST)) {
            if (part instanceof TextComponent text && !text.content().isEmpty()) {
                TextColor currentColor = text.color();

                // The first non-empty text component we hit
                if (firstColor == null) {
                    firstColor = currentColor;
                }

                // Keep updating lastColor; the last one found will be the final one
                lastColor = currentColor;
            }
        }
        this.firstColor = firstColor;
        this.lastColor = lastColor;
    }

    public TextColor getFirstColor() {
        if (firstColor == null) setColors();
        return firstColor != null ? firstColor : TextColor.color(0xFFFFFF);
    }

    public TextColor getLastColor() {
        if (lastColor == null) setColors();
        return lastColor != null ? lastColor : TextColor.color(0xFFFFFF);
    }

    public void addAction(Action action, String value, ActionReward reward) {
        if (action == null || value == null || reward == null) return;
        actions.computeIfAbsent(action, k -> new ConcurrentHashMap<>()).put(value.toLowerCase(Locale.ROOT), reward);
    }

    public boolean removeAction(Action action, String value) {
        if (action == null || value == null) return false;
        Map<String, ActionReward> actionMap = actions.get(action);
        if (actionMap != null) {
            boolean removed = actionMap.remove(value.toLowerCase(Locale.ROOT)) != null;
            if (actionMap.isEmpty()) {
                actions.remove(action);
            }
            return removed;
        }
        return false;
    }

    public boolean removeAction(Action action) {
        if (action == null) return false;
        return actions.remove(action) != null;
    }

    public boolean hasAction(Action action) {
        return action != null && actions.containsKey(action);
    }

    public boolean hasAction(Action action, String value) {
        if (action == null || value == null) return false;
        Map<String, ActionReward> actionMap = actions.get(action);
        return actionMap != null && actionMap.containsKey(value.toLowerCase(Locale.ROOT));
    }

    public void clearActions() {
        actions.clear();
        infoLines.clear();
    }

    public ActionReward getActionReward(Action action, String value) {
        if (action == null || value == null) return null;
        Map<String, ActionReward> actionMap = actions.get(action);
        return actionMap != null ? actionMap.get(value.toLowerCase(Locale.ROOT)) : null;
    }

    public void addInfoLine(JobInfoLine line) {
        if (line != null) {
            infoLines.add(line);
        }
    }

    public boolean removeInfoLine(JobInfoLine line) {
        return infoLines.remove(line);
    }

    public long calculateExpForLevel(int level) {
        String equation = levelEquation.replace("<level>", String.valueOf(level))
                .replace("<next_level>", String.valueOf(level + 1));

        // Simple equation evaluator for basic math
        try {
            return (long) evaluateExpression(equation);
        } catch (Exception e) {
            // Fallback to default calculation
            return (long) Math.pow(level + 1, 2) * 5;
        }
    }

    private double evaluateExpression(String expression) {
        try {
            return MathUtil.eval(expression);
        } catch (Exception e) {
            throw new RuntimeException("Invalid expression: " + expression, e);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private Component name;
        private Material icon = Material.STONE;
        private String levelEquation = "<next_level>*<next_level>*5";
        private YamlMessage levelUpMessage = new YamlMessage("");
        private List<String> levelUpCommands = new ArrayList<>();
        private BossBarConfig bossBarConfig = new BossBarConfig(
                "<yellow><level> <gray>level <job> <gradient:#6D6666:#938B8B:#777676>+<gained_exp><symbol_exp> <exp>/<next_exp>",
                "<gradient:#5DFF00:#E8FF00:{phase}>You have reached <bold><level></bold>. level in <job> skill.",
                BossBar.Color.BLUE,
                BossBar.Overlay.NOTCHED_10
        );

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(Component name) {
            this.name = name;
            return this;
        }

        public Builder icon(Material icon) {
            this.icon = icon;
            return this;
        }

        public Builder levelEquation(String levelEquation) {
            this.levelEquation = levelEquation;
            return this;
        }

        public Builder levelUpMessage(YamlMessage levelUpMessage) {
            this.levelUpMessage = levelUpMessage;
            return this;
        }

        public Builder levelUpCommands(List<String> levelUpCommands) {
            this.levelUpCommands = levelUpCommands;
            return this;
        }

        public Builder bossBarConfig(BossBarConfig bossBarConfig) {
            this.bossBarConfig = bossBarConfig;
            return this;
        }

        public Job build() {
            if (id == null || id.isBlank()) {
                throw new IllegalStateException("Job id cannot be null or blank");
            }
            if (name == null) {
                name = Component.text(id);
            }
            return new Job(id, name, icon, levelEquation, levelUpMessage, levelUpCommands, bossBarConfig);
        }
    }

    @Getter
    @Setter
    public static class BossBarConfig {
        private String titleTemplate;
        private String levelUpTemplate;
        private BossBar.Color color;
        private BossBar.Overlay overlay;

        public BossBarConfig(String titleTemplate, String levelUpTemplate,
                             BossBar.Color color, BossBar.Overlay overlay) {
            this.titleTemplate = titleTemplate;
            this.levelUpTemplate = levelUpTemplate;
            this.color = color;
            this.overlay = overlay;
        }
    }

    @Getter
    @Setter
    public static class ActionReward {
        private double exp;
        private double money;

        public ActionReward(double exp, double money) {
            this.exp = exp;
            this.money = money;
        }
    }
}