package me.usainsrht.ujobs.utils;

import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.yaml.YamlMessage;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.List;

public class MessageUtil {

    public static void send(Audience audience, YamlMessage message) {
        send(audience, message, UJobsPlugin.instance.getMiniMessage());
    }

    public static void send(Audience audience, YamlMessage message, TagResolver... placeholders) {
        send(audience, message, UJobsPlugin.instance.getMiniMessage(), placeholders);
    }

    public static void send(Audience audience, YamlMessage message, MiniMessage miniMessage, TagResolver... placeholders) {
        if (message.getChatMessages() != null && !message.getChatMessages().isEmpty()) {
            //didn't join all messages in single component on purpose
            //the same behaviour can be achieved by using <nl> in yml but sending individual lines can't with joining
            List<Component> components = message.getChatMessages().stream().map(line -> miniMessage.deserialize(line, placeholders)).toList();
            components.forEach(audience::sendMessage);
        }

        if (message.getActionBarMessage() != null) audience.sendActionBar(miniMessage.deserialize(message.getActionBarMessage(), placeholders));

        if (message.getTitle() != null) audience.showTitle(message.getTitle().parse(miniMessage, placeholders));

        if (message.getSounds() != null && !message.getSounds().isEmpty()) message.getSounds().forEach(audience::playSound);
    }

}
