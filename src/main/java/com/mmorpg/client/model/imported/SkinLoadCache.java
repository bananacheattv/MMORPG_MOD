package com.mmorpg.client.model.imported;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** Coalesces render-frame requests without making transient failures permanent. */
final class SkinLoadCache<T> {
    private final Map<String, Entry<T>> entries = new HashMap<>();
    private final LongSupplier clock;
    private final long retryMillis, refreshMillis;

    private static final class Entry<T> {
        T value;
        boolean loading;
        long nextAttempt;
    }

    SkinLoadCache(LongSupplier clock, long retryMillis, long refreshMillis) {
        this.clock = clock;
        this.retryMillis = retryMillis;
        this.refreshMillis = refreshMillis;
    }

    synchronized T get(String key, Supplier<CompletableFuture<T>> loader) {
        var entry = entries.computeIfAbsent(key, unused -> new Entry<>());
        if (!entry.loading && clock.getAsLong() >= entry.nextAttempt) {
            entry.loading = true;
            CompletableFuture<T> request;
            try { request = loader.get(); }
            catch (Exception failure) { request = CompletableFuture.failedFuture(failure); }
            request.whenComplete((value, failure) -> {
                synchronized (this) {
                    entry.loading = false;
                    if (failure == null && value != null) entry.value = value;
                    entry.nextAttempt = clock.getAsLong() + (failure == null && value != null ? refreshMillis : retryMillis);
                }
            });
        }
        return entry.value;
    }
}
