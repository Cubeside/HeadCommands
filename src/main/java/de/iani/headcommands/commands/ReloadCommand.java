package de.iani.headcommands.commands;

import de.iani.cubesideutils.bukkit.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import de.iani.headcommands.HeadCommandsPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class ReloadCommand extends SubCommand {
    private final HeadCommandsPlugin plugin;

    public ReloadCommand(HeadCommandsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getRequiredPermission() {
        return "headcommands.admin";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString, ArgsParser args) {
        HeadCommandsPlugin.sendSuccess(sender, "Reload gestartet.");
        plugin.reloadPluginConfig(sender);
        return true;
    }
}
