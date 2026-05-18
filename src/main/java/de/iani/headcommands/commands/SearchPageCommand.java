package de.iani.headcommands.commands;

import de.iani.cubesideutils.bukkit.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import de.iani.headcommands.HeadCommandsPlugin;
import de.iani.headcommands.model.CategoryResolution;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class SearchPageCommand extends SubCommand {
    private final HeadCommandsPlugin plugin;

    public SearchPageCommand(HeadCommandsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getRequiredPermission() {
        return "headcommands.search";
    }

    @Override
    public boolean isVisible(CommandSender sender) {
        return false;
    }

    @Override
    public String getUsage() {
        return "<categoryId> <page> <query...>";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString, ArgsParser args) {
        String categoryInput = args.getNext(null);
        String pageInput = args.getNext(null);
        String query = args.getAll(null);
        if (categoryInput == null || pageInput == null || query == null || query.isBlank()) {
            return false;
        }
        int page;
        try {
            page = Integer.parseInt(pageInput);
        } catch (NumberFormatException e) {
            return false;
        }
        CategoryResolution resolution = plugin.snapshot().resolveCategory(categoryInput);
        if (resolution.status() != CategoryResolution.Status.FOUND) {
            HeadCommandsPlugin.sendFail(sender, "Kategorie nicht gefunden.");
            return true;
        }
        SearchResultRenderer.sendSearchResults(plugin, sender, resolution.category(), query, page);
        return true;
    }
}
