package me.usainsrht.ujobs.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import me.usainsrht.ujobs.UJobsPlugin;
import me.usainsrht.ujobs.yaml.YamlCommand;
import org.bukkit.entity.Player;

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
                    if (context.getSource().getSender() instanceof Player player) plugin.getGuiManager().openJobGUI(player);
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("reload")
                        .requires(context -> context.getSender().hasPermission("ujobs.admin.reload"))
                        .executes(context -> {
                            plugin.getConfigManager().reload();
                            plugin.getJobManager().loadJobs();

                            return Command.SINGLE_SUCCESS;
                        })
                )
                .build();
    }


}
