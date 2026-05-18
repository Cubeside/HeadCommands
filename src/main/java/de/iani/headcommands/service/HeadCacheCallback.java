package de.iani.headcommands.service;

@FunctionalInterface
public interface HeadCacheCallback<T> {
    void onComplete(T result, Throwable error);
}
