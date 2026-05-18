package de.iani.headcommands.commands;

import de.iani.cubesideutils.bukkit.commands.SubCommand;
import de.iani.cubesideutils.commands.ArgsParser;
import de.iani.headcommands.HeadCommandsPlugin;
import de.iani.headcommands.model.CachedHead;
import de.iani.headcommands.model.HeadCategory;
import de.iani.headcommands.util.HeadItemFactory;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GiveCommand extends SubCommand {
    private static final int MAX_AMOUNT = 2304;

    private final HeadCommandsPlugin plugin;

    public GiveCommand(HeadCommandsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getRequiredPermission() {
        return "headcommands.give";
    }

    @Override
    public boolean requiresPlayer() {
        return true;
    }

    @Override
    public String getUsage() {
        return "<id> [amount]";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String commandString, ArgsParser args) {
        String idInput = args.getNext(null);
        if (idInput == null) {
            return false;
        }
        int id;
        try {
            id = Integer.parseInt(idInput);
        } catch (NumberFormatException e) {
            return false;
        }
        int amount = 1;
        if (args.hasNext()) {
            try {
                amount = Integer.parseInt(args.getNext());
            } catch (NumberFormatException e) {
                return false;
            }
        }
        if (amount < 1 || amount > MAX_AMOUNT) {
            HeadCommandsPlugin.sendFail(sender, "Anzahl muss zwischen 1 und " + MAX_AMOUNT + " liegen.");
            return true;
        }

        CachedHead head = plugin.snapshot().head(id);
        if (head == null) {
            HeadCommandsPlugin.sendFail(sender, "Kein Kopf mit ID " + id + " im Cache.");
            return true;
        }

        Player player = (Player) sender;
        HeadCategory category = plugin.snapshot().category(head.categoryId());
        int remaining = amount;
        int dropped = 0;
        while (remaining > 0) {
            int stackAmount = Math.min(64, remaining);
            ItemStack stack = HeadItemFactory.createHead(head, category, stackAmount);
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
            for (ItemStack leftover : leftovers.values()) {
                dropped += leftover.getAmount();
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
            remaining -= stackAmount;
        }

        if (dropped > 0) {
            HeadCommandsPlugin.sendSuccess(sender, "Kopf erhalten; " + dropped + " wurden wegen vollem Inventar gedroppt.");
        } else {
            HeadCommandsPlugin.sendSuccess(sender, "Kopf erhalten: " + head.name() + " x" + amount);
        }
        return true;
    }
}
