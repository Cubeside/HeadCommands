package de.iani.headcommands.model;

import java.util.List;

public record ApiResponse<T>(ApiMeta meta, List<String> warnings, List<T> data) {
}
