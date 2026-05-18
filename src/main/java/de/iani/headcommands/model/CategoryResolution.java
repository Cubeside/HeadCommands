package de.iani.headcommands.model;

import java.util.List;

public record CategoryResolution(Status status, HeadCategory category, List<HeadCategory> matches) {
    public enum Status {
        FOUND,
        NOT_FOUND,
        AMBIGUOUS
    }

    public static CategoryResolution found(HeadCategory category) {
        return new CategoryResolution(Status.FOUND, category, List.of(category));
    }

    public static CategoryResolution notFound() {
        return new CategoryResolution(Status.NOT_FOUND, null, List.of());
    }

    public static CategoryResolution ambiguous(List<HeadCategory> matches) {
        return new CategoryResolution(Status.AMBIGUOUS, null, List.copyOf(matches));
    }
}
