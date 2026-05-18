package de.iani.headcommands.model;

public record ApiPagination(int total, int perPage, int currentPage, int lastPage) {
}
