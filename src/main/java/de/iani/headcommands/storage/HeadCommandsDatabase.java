package de.iani.headcommands.storage;

import de.iani.cubesideutils.sql.MySQLConnection;
import de.iani.cubesideutils.sql.SQLConfig;
import de.iani.cubesideutils.sql.SQLConnection;
import de.iani.headcommands.model.CachedHead;
import de.iani.headcommands.model.HeadCacheSnapshot;
import de.iani.headcommands.model.HeadCategory;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HeadCommandsDatabase {
    private static final String META_LAST_SUCCESSFUL_SYNC = "last_successful_sync";

    private final SQLConnection connection;
    private final String categoriesTable;
    private final String headsTable;
    private final String metaTable;

    public HeadCommandsDatabase(SQLConfig config) throws SQLException {
        this.connection = new MySQLConnection(config);
        String tablePrefix = config.getTablePrefix();
        this.categoriesTable = tablePrefix + "_categories";
        this.headsTable = tablePrefix + "_heads";
        this.metaTable = tablePrefix + "_meta";
        createTables();
    }

    public void disconnect() {
        connection.disconnect();
    }

    private void createTables() throws SQLException {
        connection.runCommands((connection, sqlConnection) -> {
            try (Statement smt = connection.createStatement()) {
                smt.executeUpdate("CREATE TABLE IF NOT EXISTS `" + categoriesTable + "` ("
                        + "`id` INT NOT NULL,"
                        + "`name` VARCHAR(128) NOT NULL,"
                        + "`sync_id` CHAR(36) NOT NULL,"
                        + "PRIMARY KEY (`id`, `sync_id`),"
                        + "INDEX (`sync_id`),"
                        + "INDEX (`name`)"
                        + ") ENGINE = innodb");
                smt.executeUpdate("CREATE TABLE IF NOT EXISTS `" + headsTable + "` ("
                        + "`id` INT NOT NULL,"
                        + "`website_uuid` CHAR(36) NULL,"
                        + "`category_id` INT NOT NULL,"
                        + "`name` VARCHAR(255) NOT NULL,"
                        + "`texture_url` VARCHAR(128) NOT NULL,"
                        + "`published_at` DATE NULL,"
                        + "`sync_id` CHAR(36) NOT NULL,"
                        + "PRIMARY KEY (`id`, `sync_id`),"
                        + "INDEX (`sync_id`),"
                        + "INDEX (`category_id`, `name`),"
                        + "INDEX (`name`)"
                        + ") ENGINE = innodb");
                smt.executeUpdate("CREATE TABLE IF NOT EXISTS `" + metaTable + "` ("
                        + "`key` VARCHAR(64) NOT NULL,"
                        + "`value` MEDIUMTEXT NULL,"
                        + "PRIMARY KEY (`key`)"
                        + ") ENGINE = innodb");
            }
            return null;
        });
    }

    public HeadCacheSnapshot loadSnapshot() throws SQLException {
        return connection.runCommands((connection, sqlConnection) -> {
            String syncId = getMeta(connection, sqlConnection, META_LAST_SUCCESSFUL_SYNC);
            if (syncId == null || syncId.isBlank()) {
                return HeadCacheSnapshot.empty();
            }

            List<HeadCategory> categories = new ArrayList<>();
            PreparedStatement categoriesStatement = sqlConnection.getOrCreateStatement("SELECT id, name FROM `" + categoriesTable + "` WHERE sync_id = ?");
            categoriesStatement.setString(1, syncId);
            try (ResultSet rs = categoriesStatement.executeQuery()) {
                while (rs.next()) {
                    categories.add(new HeadCategory(rs.getInt("id"), rs.getString("name")));
                }
            }

            List<CachedHead> heads = new ArrayList<>();
            PreparedStatement headsStatement = sqlConnection.getOrCreateStatement("SELECT id, website_uuid, category_id, name, texture_url, published_at FROM `" + headsTable + "` WHERE sync_id = ?");
            headsStatement.setString(1, syncId);
            try (ResultSet rs = headsStatement.executeQuery()) {
                while (rs.next()) {
                    Date publishedAt = rs.getDate("published_at");
                    heads.add(new CachedHead(
                            rs.getInt("id"),
                            rs.getString("website_uuid"),
                            rs.getInt("category_id"),
                            rs.getString("name"),
                            rs.getString("texture_url"),
                            publishedAt == null ? null : publishedAt.toLocalDate()));
                }
            }
            return new HeadCacheSnapshot(categories, heads, syncId);
        });
    }

    public void storeSync(String syncId, List<HeadCategory> categories, List<CachedHead> heads, Map<String, String> metaValues) throws SQLException {
        connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement categoryStatement = sqlConnection.getOrCreateStatement("INSERT INTO `" + categoriesTable + "` (`id`, `name`, `sync_id`) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `name` = VALUES(`name`)");
            for (HeadCategory category : categories) {
                categoryStatement.setInt(1, category.id());
                categoryStatement.setString(2, category.name());
                categoryStatement.setString(3, syncId);
                categoryStatement.addBatch();
            }
            categoryStatement.executeBatch();

            PreparedStatement headStatement = sqlConnection.getOrCreateStatement("INSERT INTO `" + headsTable + "` (`id`, `website_uuid`, `category_id`, `name`, `texture_url`, `published_at`, `sync_id`) VALUES (?, ?, ?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE `website_uuid` = VALUES(`website_uuid`), `category_id` = VALUES(`category_id`), `name` = VALUES(`name`), `texture_url` = VALUES(`texture_url`), `published_at` = VALUES(`published_at`)");
            for (CachedHead head : heads) {
                headStatement.setInt(1, head.id());
                headStatement.setString(2, head.websiteUuid());
                headStatement.setInt(3, head.categoryId());
                headStatement.setString(4, head.name());
                headStatement.setString(5, head.textureUrl());
                setDate(headStatement, 6, head.publishedAt());
                headStatement.setString(7, syncId);
                headStatement.addBatch();
            }
            headStatement.executeBatch();

            for (Map.Entry<String, String> entry : metaValues.entrySet()) {
                setMeta(connection, sqlConnection, entry.getKey(), entry.getValue());
            }
            setMeta(connection, sqlConnection, META_LAST_SUCCESSFUL_SYNC, syncId);

            PreparedStatement deleteCategories = sqlConnection.getOrCreateStatement("DELETE FROM `" + categoriesTable + "` WHERE sync_id <> ?");
            deleteCategories.setString(1, syncId);
            deleteCategories.executeUpdate();

            PreparedStatement deleteHeads = sqlConnection.getOrCreateStatement("DELETE FROM `" + headsTable + "` WHERE sync_id <> ?");
            deleteHeads.setString(1, syncId);
            deleteHeads.executeUpdate();
            return null;
        });
    }

    private static void setDate(PreparedStatement statement, int index, LocalDate date) throws SQLException {
        if (date == null) {
            statement.setDate(index, null);
        } else {
            statement.setDate(index, Date.valueOf(date));
        }
    }

    private String getMeta(Connection connection, SQLConnection sqlConnection, String key) throws SQLException {
        PreparedStatement statement = sqlConnection.getOrCreateStatement("SELECT `value` FROM `" + metaTable + "` WHERE `key` = ?");
        statement.setString(1, key);
        try (ResultSet rs = statement.executeQuery()) {
            return rs.next() ? rs.getString("value") : null;
        }
    }

    private void setMeta(Connection connection, SQLConnection sqlConnection, String key, String value) throws SQLException {
        PreparedStatement statement = sqlConnection.getOrCreateStatement("INSERT INTO `" + metaTable + "` (`key`, `value`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `value` = VALUES(`value`)");
        statement.setString(1, key);
        statement.setString(2, value);
        statement.executeUpdate();
    }
}
