package de.iani.headcommands.commands;

import de.iani.headcommands.HeadCommandsPlugin;
import de.iani.headcommands.model.CachedHead;
import de.iani.headcommands.model.HeadCategory;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

class SearchResultRenderer {
    private SearchResultRenderer() {
        throw new UnsupportedOperationException();
    }

    static void sendSearchResults(HeadCommandsPlugin plugin, CommandSender sender, HeadCategory category, String query, int page) {
        List<CachedHead> results = plugin.snapshot().search(category.id(), query);
        int pageSize = plugin.getHeadCommandsConfig().resultsPerPage();
        int pageCount = Math.max(1, (int) Math.ceil(results.size() / (double) pageSize));
        if (page < 1 || page > pageCount) {
            HeadCommandsPlugin.sendFail(sender, "Diese Seite existiert nicht.");
            return;
        }

        HeadCommandsPlugin.sendInfo(sender, Component.text("Suche in " + category.name() + " nach \"" + query + "\" (Seite " + page + "/" + pageCount + "):", NamedTextColor.GREEN));
        if (results.isEmpty()) {
            sender.sendMessage(Component.text(" -- keine Treffer --", NamedTextColor.GRAY));
            return;
        }

        int start = (page - 1) * pageSize;
        int end = Math.min(results.size(), start + pageSize);
        for (int i = start; i < end; i++) {
            CachedHead head = results.get(i);
            Component hover = Component.text()
                    .append(Component.text("ID: " + head.id(), NamedTextColor.GRAY))
                    .append(Component.newline())
                    .append(Component.text("UUID: " + head.websiteUuid(), NamedTextColor.GRAY))
                    .append(Component.newline())
                    .append(Component.text("Kategorie: " + category.name() + " (" + category.id() + ")", NamedTextColor.GRAY))
                    .append(Component.newline())
                    .append(Component.text("Datum: " + (head.publishedAt() == null ? "-" : head.publishedAt()), NamedTextColor.GRAY))
                    .build();
            Component give = Component.text("[Geben]", NamedTextColor.BLUE)
                    .hoverEvent(HoverEvent.showText(hover))
                    .clickEvent(ClickEvent.runCommand("/headcommands give " + head.id()));
            Component name = Component.text(head.name(), NamedTextColor.AQUA)
                    .hoverEvent(HoverEvent.showText(hover))
                    .clickEvent(ClickEvent.runCommand("/headcommands give " + head.id()));
            sender.sendMessage(Component.text(" - ", NamedTextColor.GRAY)
                    .append(name)
                    .append(Component.text(" #" + head.id() + " ", NamedTextColor.DARK_GRAY))
                    .append(give));
        }

        if (pageCount > 1) {
            sendPageLinks(sender, category.id(), query, page, pageCount);
        }
    }

    private static void sendPageLinks(CommandSender sender, int categoryId, String query, int page, int pageCount) {
        Component previous = Component.text("<< vorherige", page > 1 ? NamedTextColor.BLUE : NamedTextColor.GRAY);
        if (page > 1) {
            previous = previous.clickEvent(ClickEvent.runCommand("/headcommands searchpage " + categoryId + " " + (page - 1) + " " + query))
                    .hoverEvent(HoverEvent.showText(Component.text("Seite " + (page - 1))));
        }
        Component next = Component.text("naechste >>", page < pageCount ? NamedTextColor.BLUE : NamedTextColor.GRAY);
        if (page < pageCount) {
            next = next.clickEvent(ClickEvent.runCommand("/headcommands searchpage " + categoryId + " " + (page + 1) + " " + query))
                    .hoverEvent(HoverEvent.showText(Component.text("Seite " + (page + 1))));
        }
        sender.sendMessage(previous.append(Component.text("   ")).append(next));
    }
}
