package com.mmorpg.client.model.imported;

import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class SkinLoadCacheTest {
    @Test void failedDownloadRetriesWithoutRestartAndFramesShareOneRequest() {
        var clock = new AtomicLong();
        var calls = new AtomicInteger();
        var cache = new SkinLoadCache<String>(clock::get, 30, 600);
        var failed = new CompletableFuture<String>();
        for (int i = 0; i < 50; i++)
            assertNull(cache.get("alice", () -> { calls.incrementAndGet(); return failed; }));
        assertEquals(1, calls.get());
        failed.completeExceptionally(new IllegalStateException("HTTP 503"));
        clock.set(29);
        assertNull(cache.get("alice", () -> { fail("retry must wait"); return null; }));
        clock.set(30);
        assertEquals("skin", cache.get("alice", () -> { calls.incrementAndGet(); return CompletableFuture.completedFuture("skin"); }));
        assertEquals(2, calls.get());
    }

    @Test void refreshingKeepsVisibleSkinAndSynchronousFailureDoesNotStick() {
        var clock = new AtomicLong();
        var cache = new SkinLoadCache<String>(clock::get, 30, 600);
        assertNull(cache.get("bob", () -> { throw new IllegalArgumentException("bad URI"); }));
        clock.set(30);
        assertEquals("first", cache.get("bob", () -> CompletableFuture.completedFuture("first")));
        clock.set(630);
        var refresh = new CompletableFuture<String>();
        assertEquals("first", cache.get("bob", () -> refresh));
        assertEquals("other", cache.get("carol", () -> CompletableFuture.completedFuture("other")));
        refresh.complete("second");
        assertEquals("second", cache.get("bob", () -> { fail("already loaded"); return null; }));
    }
}
