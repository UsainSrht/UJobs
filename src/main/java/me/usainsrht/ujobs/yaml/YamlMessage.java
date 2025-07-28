package me.usainsrht.ujobs.yaml;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collection;

@Setter
@Getter
public class YamlMessage {

    private Collection<String> chatMessages;
    private String actionBarMessage;
    private YamlTitle title;

    private Collection<Sound> sounds;

    public YamlMessage(Object obj) {
        if (obj instanceof String || obj instanceof Collection) chatMessages = getMessages(obj);
        else if (obj instanceof ConfigurationSection config) {
            if (config.isSet("chat")) chatMessages = getMessages(config.get("chat"));

            if (config.isSet("action_bar")) actionBarMessage = config.getString("action_bar");

            if (config.isSet("title")) title = YamlTitle.getTitle(config.get("title"));

            if (config.isSet("sound")) sounds = getSounds(config.get("sound"));
        } else if (obj instanceof YamlMessage other) {
            chatMessages = other.chatMessages;
            actionBarMessage = other.actionBarMessage;
            title = other.title;
            sounds = other.sounds;
        }
    }

    public static Collection<String> getMessages(Object object) {
        Collection<String> messages = new ArrayList<>();
        if (object instanceof String) {
            messages.add((String) object);
        } else if (object instanceof Collection) {
            messages.addAll((Collection<String>) object);
        }
        return messages;
    }

    public static Collection<Sound> getSounds(Object object) {
        Collection<Sound> sounds = new ArrayList<>();
        if (object instanceof String string) {
            sounds.add(getSound(string));
        } else if (object instanceof Collection) {
            Collection<String> strings = (Collection<String>) object;
            sounds.addAll(strings.stream().map(YamlMessage::getSound).toList());
        } else if (object instanceof ConfigurationSection config) {
            Sound.Builder builder = Sound.sound();
            if (config.isSet("name")) builder.type(Key.key(config.getString("name")));
            if (config.isSet("volume")) builder.volume((float) config.getDouble("volume"));
            if (config.isSet("pitch")) builder.pitch((float) config.getDouble("pitch"));
            if (config.isSet("seed")) builder.seed(config.getLong("seed"));
            if (config.isSet("source")) builder.source(Sound.Source.valueOf(config.getString("source")));
            sounds.add(builder.build());
        }
        return sounds;
    }

    public static Sound getSound(String string) {
        Sound.Builder sound = Sound.sound();
        String[] splitted = string.split(",");
        sound.type(Key.key(splitted[0]));
        if (splitted.length > 1) {
            sound.volume(Float.parseFloat(splitted[1]));
            if (splitted.length > 2) {
                sound.pitch(Float.parseFloat(splitted[2]));
                if (splitted.length > 3) {
                    sound.seed(Long.parseLong(splitted[3]));
                    if (splitted.length > 4) {
                        sound.source(Sound.Source.valueOf(splitted[4]));
                    }
                }
            }
        }
        return sound.build();
    }


}