package de.iani.headcommands.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HeadCacheSnapshot {
    private static final HeadCacheSnapshot EMPTY = new HeadCacheSnapshot(List.of(), List.of(), null);

    private final Map<Integer, HeadCategory> categoriesById;
    private final List<HeadCategory> categories;
    private final Map<Integer, CachedHead> headsById;
    private final Map<Integer, List<CachedHead>> headsByCategory;
    private final String syncId;

    public HeadCacheSnapshot(Collection<HeadCategory> categories, Collection<CachedHead> heads, String syncId) {
        this.syncId = syncId;

        List<HeadCategory> sortedCategories = new ArrayList<>(categories);
        sortedCategories.sort(Comparator.comparing(HeadCategory::name, String.CASE_INSENSITIVE_ORDER).thenComparingInt(HeadCategory::id));
        this.categories = List.copyOf(sortedCategories);

        Map<Integer, HeadCategory> categoriesById = new HashMap<>();
        for (HeadCategory category : sortedCategories) {
            categoriesById.put(category.id(), category);
        }
        this.categoriesById = Map.copyOf(categoriesById);

        Map<Integer, CachedHead> headsById = new HashMap<>();
        Map<Integer, List<CachedHead>> byCategory = new HashMap<>();
        for (CachedHead head : heads) {
            headsById.put(head.id(), head);
            byCategory.computeIfAbsent(head.categoryId(), ignored -> new ArrayList<>()).add(head);
        }
        Comparator<CachedHead> headComparator = Comparator.comparing(CachedHead::name, String.CASE_INSENSITIVE_ORDER).thenComparingInt(CachedHead::id);
        Map<Integer, List<CachedHead>> immutableByCategory = new HashMap<>();
        for (Map.Entry<Integer, List<CachedHead>> entry : byCategory.entrySet()) {
            entry.getValue().sort(headComparator);
            immutableByCategory.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        this.headsById = Map.copyOf(headsById);
        this.headsByCategory = Map.copyOf(immutableByCategory);
    }

    public static HeadCacheSnapshot empty() {
        return EMPTY;
    }

    public String syncId() {
        return syncId;
    }

    public boolean isEmpty() {
        return categoriesById.isEmpty() && headsById.isEmpty();
    }

    public int categoryCount() {
        return categoriesById.size();
    }

    public int headCount() {
        return headsById.size();
    }

    public List<HeadCategory> categories() {
        return categories;
    }

    public HeadCategory category(int id) {
        return categoriesById.get(id);
    }

    public CachedHead head(int id) {
        return headsById.get(id);
    }

    public CategoryResolution resolveCategory(String input) {
        if (input == null || input.isBlank()) {
            return CategoryResolution.notFound();
        }
        try {
            int id = Integer.parseInt(input);
            HeadCategory category = categoriesById.get(id);
            return category == null ? CategoryResolution.notFound() : CategoryResolution.found(category);
        } catch (NumberFormatException ignored) {
            // match by name below
        }

        String normalizedInput = normalizeCategoryName(input);
        List<HeadCategory> prefixMatches = new ArrayList<>();
        for (HeadCategory category : categories) {
            String normalizedName = normalizeCategoryName(category.name());
            if (normalizedName.equals(normalizedInput)) {
                return CategoryResolution.found(category);
            }
            if (normalizedName.startsWith(normalizedInput)) {
                prefixMatches.add(category);
            }
        }
        if (prefixMatches.size() == 1) {
            return CategoryResolution.found(prefixMatches.get(0));
        }
        if (prefixMatches.size() > 1) {
            return CategoryResolution.ambiguous(prefixMatches);
        }
        return CategoryResolution.notFound();
    }

    public List<CachedHead> search(int categoryId, String query) {
        List<CachedHead> categoryHeads = headsByCategory.get(categoryId);
        if (categoryHeads == null || categoryHeads.isEmpty()) {
            return List.of();
        }
        String[] tokens = normalizeSearchQuery(query);
        if (tokens.length == 0) {
            return categoryHeads;
        }

        List<CachedHead> results = new ArrayList<>();
        for (CachedHead head : categoryHeads) {
            String name = head.name().toLowerCase(Locale.ROOT);
            boolean match = true;
            for (String token : tokens) {
                if (!name.contains(token)) {
                    match = false;
                    break;
                }
            }
            if (match) {
                results.add(head);
            }
        }
        return results;
    }

    private static String[] normalizeSearchQuery(String query) {
        if (query == null || query.isBlank()) {
            return new String[0];
        }
        return query.toLowerCase(Locale.ROOT).trim().split("\\s+");
    }

    private static String normalizeCategoryName(String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                result.append(c);
            }
        }
        return result.toString();
    }
}
