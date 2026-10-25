package com.mmorpg.network;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.*;
import java.util.UUID;

/** One OS lock per connected player. Only the lock holder may replace a profile. */
public final class ProfileStore implements AutoCloseable {
    private final Path profile;
    private final FileChannel channel;
    private final FileLock lock;
    private ProfileStore(Path profile, FileChannel channel, FileLock lock) {
        this.profile = profile; this.channel = channel; this.lock = lock;
    }
    public static ProfileStore acquire(Path dir, UUID id) throws IOException {
        Files.createDirectories(dir);
        FileChannel channel = FileChannel.open(dir.resolve(id + ".lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            FileLock lock;
            try { lock = channel.tryLock(); } catch (OverlappingFileLockException e) { lock = null; }
            if (lock == null) { channel.close(); return null; }
            return new ProfileStore(dir.resolve(id + ".dat"), channel, lock);
        } catch (IOException | RuntimeException e) { channel.close(); throw e; }
    }
    public byte[] read() throws IOException {
        return Files.exists(profile) ? Files.readAllBytes(profile) : null;
    }
    public void write(byte[] data) throws IOException {
        if (!lock.isValid()) throw new IOException("Player session lock lost");
        Path tmp = Files.createTempFile(profile.getParent(), "profile-", ".tmp");
        try {
            try (FileChannel out = FileChannel.open(tmp, StandardOpenOption.WRITE)) {
                var bytes = java.nio.ByteBuffer.wrap(data);
                while (bytes.hasRemaining()) out.write(bytes);
                out.force(true);
            }
            if (Files.exists(profile)) Files.copy(profile, profile.resolveSibling(profile.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            Files.move(tmp, profile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally { Files.deleteIfExists(tmp); }
    }
    @Override public void close() throws IOException { try { lock.release(); } finally { channel.close(); } }
}
