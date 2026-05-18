package de.iani.headcommands;

import de.iani.cubesideutils.bukkit.sql.SQLConfigBukkit;
import de.iani.cubesideutils.sql.SQLConfig;
import java.net.URI;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class HeadCommandsConfig {
    private static final String APP_UUID = "31984614-770a-47b6-928c-d8fe0b5a25d7";
    private static final int DEFAULT_RESULTS_PER_PAGE = 10;
    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 10;
    private static final int DEFAULT_REQUEST_TIMEOUT_SECONDS = 60;
    private static final int DEFAULT_MAX_PAGES_PER_CATEGORY = 200;

    private final SQLConfig sqlConfig;
    private final URI apiBaseUri;
    private final String apiKey;
    private final boolean refreshOnEnable;
    private final int resultsPerPage;
    private final int connectTimeoutSeconds;
    private final int requestTimeoutSeconds;
    private final int maxPagesPerCategory;

    public HeadCommandsConfig(SQLConfig sqlConfig, URI apiBaseUri, String apiKey, boolean refreshOnEnable, int resultsPerPage, int connectTimeoutSeconds, int requestTimeoutSeconds, int maxPagesPerCategory) {
        this.sqlConfig = sqlConfig;
        this.apiBaseUri = apiBaseUri;
        this.apiKey = apiKey;
        this.refreshOnEnable = refreshOnEnable;
        this.resultsPerPage = resultsPerPage;
        this.connectTimeoutSeconds = connectTimeoutSeconds;
        this.requestTimeoutSeconds = requestTimeoutSeconds;
        this.maxPagesPerCategory = maxPagesPerCategory;
    }

    public static HeadCommandsConfig load(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        SQLConfig sqlConfig = new SQLConfigBukkit(config.getConfigurationSection("database"));

        String baseUrl = config.getString("api.baseUrl", "https://minecraft-heads.com").trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        return new HeadCommandsConfig(
                sqlConfig,
                URI.create(baseUrl),
                config.getString("api.apiKey", "").trim(),
                config.getBoolean("settings.refreshOnEnable", false),
                readBounded(config, "settings.resultsPerPage", DEFAULT_RESULTS_PER_PAGE, 1, 50),
                readBounded(config, "settings.connectTimeoutSeconds", DEFAULT_CONNECT_TIMEOUT_SECONDS, 1, 120),
                readBounded(config, "settings.requestTimeoutSeconds", DEFAULT_REQUEST_TIMEOUT_SECONDS, 1, 300),
                readBounded(config, "settings.maxPagesPerCategory", DEFAULT_MAX_PAGES_PER_CATEGORY, 1, 1000));
    }

    private static int readBounded(FileConfiguration config, String path, int def, int min, int max) {
        int value = config.getInt(path, def);
        return Math.max(min, Math.min(max, value));
    }

    public SQLConfig sqlConfig() {
        return sqlConfig;
    }

    public URI apiBaseUri() {
        return apiBaseUri;
    }

    public String appUuid() {
        return APP_UUID;
    }

    public String apiKey() {
        return apiKey;
    }

    public boolean refreshOnEnable() {
        return refreshOnEnable;
    }

    public int resultsPerPage() {
        return resultsPerPage;
    }

    public int connectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public int requestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public int maxPagesPerCategory() {
        return maxPagesPerCategory;
    }

    public boolean hasUsableApiCredentials() {
        return isConfigured(apiKey);
    }

    public String validateForDatabase() {
        String tablePrefix = sqlConfig.getTablePrefix();
        if (tablePrefix == null || !tablePrefix.matches("[A-Za-z0-9_]+")) {
            return "database.tableprefix may only contain letters, digits and underscores.";
        }
        return null;
    }

    public String validateForRefresh() {
        if (!isConfigured(apiKey)) {
            return "api.apiKey must be configured. A free Minecraft-Heads API key is required.";
        }
        return null;
    }

    private static boolean isConfigured(String value) {
        return value != null && !value.isBlank() && !"CHANGETHIS".equalsIgnoreCase(value);
    }
}
