package de.iani.headcommands.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class HeadCacheSnapshotTest {
    @Test
    void resolvesCategoriesByIdExactNameAndNormalizedPrefix() {
        HeadCacheSnapshot snapshot = snapshot();

        assertEquals(CategoryResolution.Status.FOUND, snapshot.resolveCategory("1").status());
        assertEquals(2, snapshot.resolveCategory("Food & Drinks").category().id());
        assertEquals(2, snapshot.resolveCategory("fooddr").category().id());
        assertEquals(CategoryResolution.Status.NOT_FOUND, snapshot.resolveCategory("missing").status());
    }

    @Test
    void searchesByAllTokensInsideCategory() {
        HeadCacheSnapshot snapshot = snapshot();

        List<CachedHead> results = snapshot.search(2, "red apple");

        assertEquals(1, results.size());
        assertEquals(11, results.get(0).id());
    }

    private static HeadCacheSnapshot snapshot() {
        return new HeadCacheSnapshot(
                List.of(new HeadCategory(1, "Alphabet"), new HeadCategory(2, "Food & Drinks")),
                List.of(
                        new CachedHead(10, "00000000-0000-0000-0000-000000000010", 2, "Green Apple", "abc", LocalDate.of(2025, 1, 1)),
                        new CachedHead(11, "00000000-0000-0000-0000-000000000011", 2, "Red Apple", "def", LocalDate.of(2025, 1, 2)),
                        new CachedHead(12, "00000000-0000-0000-0000-000000000012", 1, "Red Letter", "ghi", null)),
                "sync");
    }
}
