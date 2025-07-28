package me.usainsrht.ujobs.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.yaml.YamlCommand;

public class MainCommand {

    public static LiteralCommandNode<CommandSourceStack> create(UJobsPlugin plugin, YamlCommand yamlCommand) {
        return Commands.literal(yamlCommand.getName())
                .requires(context -> {
                    // Check if the player has the required permission
                    if (yamlCommand.getPermission() != null) {
                        return context.getSender().hasPermission(yamlCommand.getPermission());
                    }
                    return true;
                })
                .executes(context -> {
                    plugin.getGuiManager().openJobGUI();
                    return Command.SINGLE_SUCCESS;
                })
                .build();
    }


}
