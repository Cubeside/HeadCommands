package de.iani.headcommands.commands;

import de.iani.cubesideutils.bukkit.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import de.iani.headcommands.HeadCommandsPlugin;
import de.iani.headcommands.model.CategoryResolution;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class SearchCommand extends SubCommand {
    private final HeadCommandsPlugin plugin;

    public SearchCommand(HeadCommandsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getRequiredPermission() {
        return "headcommands.search";
    }

    @Override
    public String getUsage() {
        return "<categoryId|categoryName> <query...>";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString, ArgsParser args) {
        String categoryInput = args.getNext(null);
        String query = args.getAll(null);
        if (categoryInput == null || query == null || query.isBlank()) {
            return false;
        }
        CategoryResolution resolution = plugin.snapshot().resolveCategory(categoryInput);
        if (resolution.status() == CategoryResolution.Status.NOT_FOUND) {
            HeadCommandsPlugin.sendFail(sender, "Kategorie nicht gefunden. Nutze /headcommands categories.");
            return true;
        }
        if (resolution.status() == CategoryResolution.Status.AMBIGUOUS) {
            HeadCommandsPlugin.sendFail(sender, "Kategorie ist nicht eindeutig: " + resolution.matches().stream().limit(5).map(c -> c.name() + " (" + c.id() + ")").reduce((a, b) -> a + ", " + b).orElse("-"));
            return true;
        }
        SearchResultRenderer.sendSearchResults(plugin, sender, resolution.category(), query, 1);
        return true;
    }

    @Override
    public Collection<String> onTabComplete(CommandSender sender, Command command, String alias, ArgsParser args) {
        if (args.remaining() == 1) {
            List<String> result = new ArrayList<>();
            plugin.snapshot().categories().forEach(category -> result.add(Integer.toString(category.id())));
            return result;
        }
        return List.of();
    }
}
