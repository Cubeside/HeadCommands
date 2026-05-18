package de.iani.headcommands.model;

import java.time.LocalDate;

public record CachedHead(int id, String websiteUuid, int categoryId, String name, String textureUrl, LocalDate publishedAt) {
}
