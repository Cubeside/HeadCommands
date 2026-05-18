package de.iani.headcommands.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.iani.headcommands.model.ApiResponse;
import de.iani.headcommands.model.CachedHead;
import de.iani.headcommands.model.HeadCategory;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class HeadApiParserTest {
    private final HeadApiParser parser = new HeadApiParser();

    @Test
    void parsesCategories() throws Exception {
        String json = """
                {
                  "meta": {"api_version":"2.2.0","demo_mode":false,"data_limited":false,"license":"none","records":2},
                  "data": [
                    {"id": 1, "n": "Alphabet"},
                    {"id": 2, "n": "Food & Drinks"}
                  ]
                }
                """;

        ApiResponse<HeadCategory> response = parser.parseCategories(json);

        assertEquals("2.2.0", response.meta().apiVersion());
        assertEquals(2, response.data().size());
        assertEquals(new HeadCategory(2, "Food & Drinks"), response.data().get(1));
    }

    @Test
    void parsesHeadsWithFreeFieldsAndNullableDate() throws Exception {
        String json = """
                {
                  "meta": {"api_version":"2.2.0","demo_mode":false,"data_limited":false,"license":"free","records":2},
                  "warnings": ["test warning"],
                  "data": [
                    {"id": 10, "n": "Apple", "c": 4, "i": "00000000-0000-0000-0000-000000000010", "u": "abc", "p": "2025-07-13"},
                    {"id": 11, "n": "Pear", "c": 4, "i": "00000000-0000-0000-0000-000000000011", "u": "def", "p": null}
                  ]
                }
                """;

        ApiResponse<CachedHead> response = parser.parseHeads(json, true);

        assertEquals(2, response.data().size());
        assertEquals(LocalDate.of(2025, 7, 13), response.data().get(0).publishedAt());
        assertNull(response.data().get(1).publishedAt());
        assertEquals("test warning", response.warnings().get(0));
    }

    @Test
    void flattensNestedDataArraysFromDocumentationExamples() throws Exception {
        String json = """
                {
                  "meta": {"api_version":"2.2.0","demo_mode":false,"data_limited":false,"license":"free","records":1},
                  "data": [
                    [
                      {"id": 10, "n": "Apple", "c": 4, "i": "00000000-0000-0000-0000-000000000010", "u": "abc"}
                    ]
                  ]
                }
                """;

        ApiResponse<CachedHead> response = parser.parseHeads(json, true);

        assertEquals(1, response.data().size());
        assertEquals("Apple", response.data().get(0).name());
    }

    @Test
    void rejectsMissingFreeFieldsWhenRequired() {
        String json = """
                {
                  "meta": {"api_version":"2.2.0","demo_mode":false,"data_limited":false,"license":"none","records":1},
                  "data": [
                    {"n": "Apple", "c": 4, "u": "abc"}
                  ]
                }
                """;

        assertThrows(HeadApiException.class, () -> parser.parseHeads(json, true));
    }
}
