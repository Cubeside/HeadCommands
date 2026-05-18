package de.iani.headcommands;

import de.iani.cubesideutils.bukkit.commands.CommandRouter;
import de.iani.headcommands.commands.CategoriesCommand;
import de.iani.headcommands.commands.GiveCommand;
import de.iani.headcommands.commands.RefreshCommand;
import de.iani.headcommands.commands.ReloadCommand;
import de.iani.headcommands.commands.SearchCommand;
import de.iani.headcommands.commands.SearchPageCommand;
import de.iani.headcommands.model.HeadCacheSnapshot;
import de.iani.headcommands.service.HeadCacheService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class HeadCommandsPlugin extends JavaPlugin {
    private static final Component PREFIX = Component.text("[", NamedTextColor.DARK_AQUA)
            .append(Component.text("Heads", NamedTextColor.AQUA))
            .append(Component.text("]", NamedTextColor.DARK_AQUA));

    private HeadCommandsConfig config;
    private HeadCacheService cacheService;
    private CommandRouter commandRouter;

    @Override
    public void onEnable() {
        config = HeadCommandsConfig.load(this);
        cacheService = new HeadCacheService(this, config);
        cacheService.start();

        commandRouter = new CommandRouter(getCommand("headcommands"));
        commandRouter.addCommandMapping(new CategoriesCommand(this), "categories");
        commandRouter.addCommandMapping(new SearchCommand(this), "search");
        commandRouter.addCommandMapping(new SearchPageCommand(this), "searchpage");
        commandRouter.addCommandMapping(new GiveCommand(this), "give");
        commandRouter.addCommandMapping(new RefreshCommand(this), "refresh");
        commandRouter.addCommandMapping(new ReloadCommand(this), "reload");

        cacheService.reconfigureAndLoad(config, (snapshot, error) -> {
            if (error != null) {
                getLogger().severe("Could not initialize HeadCommands database: " + error.getMessage());
                return;
            }
            getLogger().info("Loaded " + snapshot.categoryCount() + " categories and " + snapshot.headCount() + " heads from database cache.");
            if (config.refreshOnEnable()) {
                queueRefreshOnEnable();
            }
        });

        String refreshValidation = config.validateForRefresh();
        if (refreshValidation != null) {
            getLogger().warning(refreshValidation);
        }
    }

    @Override
    public void onDisable() {
        if (cacheService != null) {
            cacheService.shutdown();
        }
    }

    public void reloadPluginConfig(CommandSender sender) {
        HeadCommandsConfig newConfig = HeadCommandsConfig.load(this);
        config = newConfig;
        cacheService.reconfigureAndLoad(newConfig, (snapshot, error) -> {
            if (error != null) {
                sendFail(sender, "Reload failed: " + error.getMessage());
                return;
            }
            sendSuccess(sender, "Reloaded config and loaded " + snapshot.categoryCount() + " categories / " + snapshot.headCount() + " heads.");
            String refreshValidation = newConfig.validateForRefresh();
            if (refreshValidation != null) {
                sendFail(sender, refreshValidation);
            }
        });
    }

    private void queueRefreshOnEnable() {
        cacheService.refresh((result, error) -> {
            if (error != null) {
                getLogger().severe("Refresh on enable failed: " + error.getMessage());
                return;
            }
            getLogger().info("Refresh on enable loaded " + result.categoryCount() + " categories and " + result.headCount() + " heads.");
        });
    }

    public HeadCommandsConfig getHeadCommandsConfig() {
        return config;
    }

    public HeadCacheService getCacheService() {
        return cacheService;
    }

    public HeadCacheSnapshot snapshot() {
        return cacheService.snapshot();
    }

    public static Component prefix() {
        return PREFIX;
    }

    public static void sendSuccess(CommandSender sender, String message) {
        sender.sendMessage(Component.empty().append(PREFIX).append(Component.space()).append(Component.text(message, NamedTextColor.GREEN)));
    }

    public static void sendFail(CommandSender sender, String message) {
        sender.sendMessage(Component.empty().append(PREFIX).append(Component.space()).append(Component.text(message, NamedTextColor.RED)));
    }

    public static void sendInfo(CommandSender sender, Component message) {
        sender.sendMessage(Component.empty().append(PREFIX).append(Component.space()).append(message));
    }
}
