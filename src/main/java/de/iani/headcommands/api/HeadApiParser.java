package de.iani.headcommands.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.iani.headcommands.model.ApiMeta;
import de.iani.headcommands.model.ApiPagination;
import de.iani.headcommands.model.ApiResponse;
import de.iani.headcommands.model.CachedHead;
import de.iani.headcommands.model.HeadCategory;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class HeadApiParser {
    public ApiResponse<HeadCategory> parseCategories(String json) throws HeadApiException {
        JsonObject root = parseRoot(json);
        List<HeadCategory> categories = new ArrayList<>();
        for (JsonObject object : dataObjects(root)) {
            Integer id = getInt(object, "id");
            String name = getString(object, "n");
            if (id == null || name == null || name.isBlank()) {
                throw new HeadApiException("Category response contains an entry without id or name.");
            }
            categories.add(new HeadCategory(id, name));
        }
        return new ApiResponse<>(parseMeta(root), parsePagination(root), parseWarnings(root), List.copyOf(categories));
    }

    public ApiResponse<CachedHead> parseHeads(String json, boolean requireFreeFields) throws HeadApiException {
        JsonObject root = parseRoot(json);
        ApiMeta meta = parseMeta(root);
        if (requireFreeFields && !meta.hasFreeFields()) {
            throw new HeadApiException("Minecraft-Heads response did not use a free-or-higher license. Check api.apiKey.");
        }

        List<CachedHead> heads = new ArrayList<>();
        for (JsonObject object : dataObjects(root)) {
            Integer id = getInt(object, "id");
            String name = getString(object, "n");
            Integer categoryId = getInt(object, "c");
            String uuid = getString(object, "i");
            String url = getString(object, "u");
            LocalDate publishedAt = parseDate(getString(object, "p"));

            if (requireFreeFields && (id == null || uuid == null || uuid.isBlank())) {
                throw new HeadApiException("Head response is missing id or uuid. Check that api.apiKey has at least free access.");
            }
            if (id == null || name == null || categoryId == null || url == null || name.isBlank() || url.isBlank()) {
                throw new HeadApiException("Head response contains an entry without required cache fields.");
            }
            heads.add(new CachedHead(id, uuid, categoryId, name, url, publishedAt));
        }
        return new ApiResponse<>(meta, parsePagination(root), parseWarnings(root), List.copyOf(heads));
    }

    private static JsonObject parseRoot(String json) throws HeadApiException {
        try {
            JsonElement element = JsonParser.parseString(json);
            if (!element.isJsonObject()) {
                throw new HeadApiException("API response is not a JSON object.");
            }
            return element.getAsJsonObject();
        } catch (RuntimeException e) {
            throw new HeadApiException("Could not parse API JSON response.", e);
        }
    }

    private static ApiMeta parseMeta(JsonObject root) {
        JsonObject meta = getObject(root, "meta");
        if (meta == null) {
            return new ApiMeta(null, false, false, "none", 0);
        }
        return new ApiMeta(
                getString(meta, "api_version"),
                getBoolean(meta, "demo_mode"),
                getBoolean(meta, "data_limited"),
                getString(meta, "license", "none"),
                getInt(meta, "records", 0));
    }

    private static List<String> parseWarnings(JsonObject root) {
        JsonArray warnings = getArray(root, "warnings");
        if (warnings == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>(warnings.size());
        for (JsonElement warning : warnings) {
            if (warning.isJsonPrimitive()) {
                result.add(warning.getAsString());
            }
        }
        return List.copyOf(result);
    }

    private static ApiPagination parsePagination(JsonObject root) {
        JsonObject pagination = getObject(root, "pagination");
        if (pagination == null) {
            return null;
        }
        return new ApiPagination(
                getInt(pagination, "total", 0),
                getInt(pagination, "per_page", 0),
                getInt(pagination, "current_page", 1),
                getInt(pagination, "last_page", 1));
    }

    private static List<JsonObject> dataObjects(JsonObject root) throws HeadApiException {
        JsonArray data = getArray(root, "data");
        if (data == null) {
            return List.of();
        }
        List<JsonObject> result = new ArrayList<>();
        for (JsonElement element : data) {
            collectObjects(element, result);
        }
        return result;
    }

    private static void collectObjects(JsonElement element, List<JsonObject> result) throws HeadApiException {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonObject()) {
            result.add(element.getAsJsonObject());
            return;
        }
        if (element.isJsonArray()) {
            for (JsonElement nested : element.getAsJsonArray()) {
                collectObjects(nested, result);
            }
            return;
        }
        throw new HeadApiException("API data array contains an unsupported item.");
    }

    private static LocalDate parseDate(String value) throws HeadApiException {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new HeadApiException("Invalid published_at date: " + value, e);
        }
    }

    private static JsonObject getObject(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static JsonArray getArray(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private static String getString(JsonObject object, String key) {
        return getString(object, key, null);
    }

    private static String getString(JsonObject object, String key, String def) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return def;
        }
        return element.getAsString();
    }

    private static Integer getInt(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        return element.getAsInt();
    }

    private static int getInt(JsonObject object, String key, int def) {
        Integer result = getInt(object, key);
        return result == null ? def : result;
    }

    private static boolean getBoolean(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && !element.isJsonNull() && element.getAsBoolean();
    }
}
