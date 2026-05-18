package de.iani.headcommands.model;

import java.util.List;

public record ApiResponse<T>(ApiMeta meta, ApiPagination pagination, List<String> warnings, List<T> data) {
}
