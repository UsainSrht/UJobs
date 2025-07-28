package me.usainsrht.ujobs.models;

import lombok.Getter;
import me.usainsrht.ujobs.utils.MathUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;

import java.util.HashMap;
import java.util.Map;

@Getter
public class Job {
    private final String id;
    private final Component name;
    private final Material icon;
    private final String levelEquation;
    private final Sound levelUpSound;
    private final BossBarConfig bossBarConfig;
    private final Map<String, Map<String, ActionReward>> actions;

    public Job(String id, Component name, Material icon, String levelEquation,
               Sound levelUpSound, BossBarConfig bossBarConfig) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.levelEquation = levelEquation;
        this.levelUpSound = levelUpSound;
        this.bossBarConfig = bossBarConfig;
        this.actions = new HashMap<>();
    }

    public void addAction(String actionType, String value, ActionReward reward) {
        actions.computeIfAbsent(actionType, k -> new HashMap<>()).put(value, reward);
    }

    public ActionReward getActionReward(String actionType, String value) {
        Map<String, ActionReward> actionMap = actions.get(actionType);
        return actionMap != null ? actionMap.get(value) : null;
    }

    public boolean hasAction(String actionType, String value) {
        return getActionReward(actionType, value) != null;
    }

    public long calculateExpForLevel(int level) {
        String equation = levelEquation.replace("<level>", String.valueOf(level))
                .replace("<nextlevel>", String.valueOf(level + 1));

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

    @Getter
    public static class BossBarConfig {
        private final String titleTemplate;
        private final String levelUpTemplate;
        private final BossBar.Color color;
        private final BossBar.Overlay overlay;

        public BossBarConfig(String titleTemplate, String levelUpTemplate,
                             BossBar.Color color, BossBar.Overlay overlay) {
            this.titleTemplate = titleTemplate;
            this.levelUpTemplate = levelUpTemplate;
            this.color = color;
            this.overlay = overlay;
        }

    }

    @Getter
    public static class ActionReward {
        private final double exp;
        private final double money;

        public ActionReward(double exp, double money) {
            this.exp = exp;
            this.money = money;
        }

    }
}