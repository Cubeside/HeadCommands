package de.iani.headcommands.commands;

import de.iani.cubesideutils.bukkit.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import de.iani.headcommands.HeadCommandsPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class RefreshCommand extends SubCommand {
    private final HeadCommandsPlugin plugin;

    public RefreshCommand(HeadCommandsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getRequiredPermission() {
        return "headcommands.admin";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString, ArgsParser args) {
        String validationError = plugin.getHeadCommandsConfig().validateForRefresh();
        if (validationError != null) {
            HeadCommandsPlugin.sendFail(sender, validationError);
            return true;
        }
        HeadCommandsPlugin.sendSuccess(sender, "Refresh gestartet.");
        plugin.getCacheService().refresh((result, error) -> {
            if (error != null) {
                HeadCommandsPlugin.sendFail(sender, "Refresh fehlgeschlagen: " + error.getMessage());
                return;
            }
            HeadCommandsPlugin.sendSuccess(sender, "Refresh fertig: " + result.categoryCount() + " Kategorien, " + result.headCount() + " Koepfe.");
        });
        return true;
    }
}
