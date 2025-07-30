package me.usainsrht.ujobs.yaml;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import me.usainsrht.ujobs.UJobsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import org.bukkit.configuration.ConfigurationSection;

import java.time.Duration;
import java.util.List;

@Setter
@Getter
@Builder
public class YamlTitle  {

    private String title;
    private String subTitle;
    private String stay;
    private String fadeIn;
    private String fadeOut;

    //in ticks
    public static final int DEFAULT_FADE_IN = 10;
    public static final int DEFAULT_STAY = 70;
    public static final int DEFAULT_FADE_OUT = 20;

    private static final long MILLIS_IN_ONE_SECOND = 20;

    public Title parse() {
        return parse(UJobsPlugin.instance.getMiniMessage());
    }

    public Title parse(TagResolver... placeholders) {
        return parse(UJobsPlugin.instance.getMiniMessage(), placeholders);
    }

    public Title parse(MiniMessage miniMessage, TagResolver... placeholders) {
        int timeFadeIn = (fadeIn != null) ? Integer.parseInt(fadeIn) : DEFAULT_FADE_IN;
        int timeStay = (stay != null) ? Integer.parseInt(stay) : DEFAULT_STAY;
        int timeFadeOut = (fadeOut != null) ? Integer.parseInt(fadeOut) : DEFAULT_FADE_OUT;

        Title.Times times = Title.Times.times(Duration.ofMillis(timeFadeIn * MILLIS_IN_ONE_SECOND), Duration.ofMillis(timeStay * MILLIS_IN_ONE_SECOND), Duration.ofMillis(timeFadeOut * MILLIS_IN_ONE_SECOND));
        Component parsedTitle = miniMessage.deserialize((title != null) ? title : "", placeholders);
        Component parsedSubtitle = miniMessage.deserialize((subTitle != null) ? subTitle : "", placeholders);
        return Title.title(parsedTitle, parsedSubtitle, times);
    }

    public static YamlTitle getTitle(Object obj) {
        YamlTitle.YamlTitleBuilder builder = YamlTitle.builder();
        if (obj instanceof String string) builder.title(string);
        else if (obj instanceof List) {
            List<String> stringList = (List<String>) obj;
            builder.title(stringList.get(0));
            builder.subTitle(stringList.get(1));
        } else if (obj instanceof ConfigurationSection config) {
            if (config.isSet("title")) builder.title(config.getString("title"));
            if (config.isSet("subtitle")) builder.subTitle(config.getString("subtitle"));
            if (config.isSet("fade_in")) builder.fadeIn(config.getString("fade_in"));
            if (config.isSet("stay")) builder.stay(config.getString("stay"));
            if (config.isSet("fade_out")) builder.fadeOut(config.getString("fade_out"));
        }
        return builder.build();
    }

}