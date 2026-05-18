package de.iani.headcommands.commands;

import de.iani.cubesideutils.bukkit.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import de.iani.headcommands.HeadCommandsPlugin;
import de.iani.headcommands.model.HeadCategory;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CategoriesCommand extends SubCommand {
    private final HeadCommandsPlugin plugin;

    public CategoriesCommand(HeadCommandsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getRequiredPermission() {
        return "headcommands.use";
    }

    @Override
    public String getUsage() {
        return "[page]";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString, ArgsParser args) {
        int page = args.hasNext() ? args.getNext(1) : 1;
        if (page < 1) {
            return false;
        }
        List<HeadCategory> categories = plugin.snapshot().categories();
        int pageSize = plugin.getHeadCommandsConfig().resultsPerPage();
        int pageCount = Math.max(1, (int) Math.ceil(categories.size() / (double) pageSize));
        if (page > pageCount) {
            HeadCommandsPlugin.sendFail(sender, "Diese Seite existiert nicht.");
            return true;
        }

        HeadCommandsPlugin.sendInfo(sender, Component.text("Kategorien (Seite " + page + "/" + pageCount + "):", NamedTextColor.GREEN));
        if (categories.isEmpty()) {
            sender.sendMessage(Component.text(" -- keine Daten im Cache --", NamedTextColor.GRAY));
            return true;
        }
        int start = (page - 1) * pageSize;
        int end = Math.min(categories.size(), start + pageSize);
        for (int i = start; i < end; i++) {
            HeadCategory category = categories.get(i);
            sender.sendMessage(Component.text(" - ", NamedTextColor.GRAY)
                    .append(Component.text(category.id(), NamedTextColor.DARK_AQUA))
                    .append(Component.text(": ", NamedTextColor.GRAY))
                    .append(Component.text(category.name(), NamedTextColor.AQUA)
                            .hoverEvent(HoverEvent.showText(Component.text("/heads search " + category.id() + " <query>")))
                            .clickEvent(ClickEvent.suggestCommand("/heads search " + category.id() + " "))));
        }
        sendPageLinks(sender, page, pageCount);
        return true;
    }

    private void sendPageLinks(CommandSender sender, int page, int pageCount) {
        if (pageCount <= 1) {
            return;
        }
        Component previous = Component.text("<< vorherige", page > 1 ? NamedTextColor.BLUE : NamedTextColor.GRAY);
        if (page > 1) {
            previous = previous.clickEvent(ClickEvent.runCommand("/heads categories " + (page - 1))).hoverEvent(HoverEvent.showText(Component.text("Seite " + (page - 1))));
        }
        Component next = Component.text("naechste >>", page < pageCount ? NamedTextColor.BLUE : NamedTextColor.GRAY);
        if (page < pageCount) {
            next = next.clickEvent(ClickEvent.runCommand("/heads categories " + (page + 1))).hoverEvent(HoverEvent.showText(Component.text("Seite " + (page + 1))));
        }
        sender.sendMessage(previous.append(Component.text("   ")).append(next));
    }
}
