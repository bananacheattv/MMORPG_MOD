package com.mmorpg.character;

import com.mmorpg.network.NetworkConfig;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Fiche des personnages d'un compte : resume de chaque emplacement et sauvegarde complete des personnages non joues.
 * Le personnage joue reste dans les donnees du joueur ; son instantane ici (s'il existe) est perime et ignore.
 * Un fichier illisible n'est jamais remplace par un fichier vide.
 */
public final class CharacterStore {
    public static final class Slot {
        public int slot;
        public String name = "";
        public int cls;
        public int level = 1;
        public String place = "";
        public String role = "";
        public String dim = "";
        public double x, y, z;
        public long saved;
        public CompoundTag equip = new CompoundTag();
        public CompoundTag snapshot;

        CompoundTag save() {
            var t = new CompoundTag();
            t.putInt("slot", slot); t.putString("name", name); t.putInt("cls", cls); t.putInt("level", level);
            t.putString("place", place); t.putString("role", role); t.putString("dim", dim);
            t.putDouble("x", x); t.putDouble("y", y); t.putDouble("z", z); t.putLong("saved", saved);
            t.put("equip", equip.copy());
            if (snapshot != null) t.put("snapshot", snapshot.copy());
            return t;
        }

        static Slot load(CompoundTag t) {
            var s = new Slot();
            s.slot = t.getIntOr("slot", 0); s.name = t.getStringOr("name", ""); s.cls = t.getIntOr("cls", 0); s.level = t.getIntOr("level", 1);
            s.place = t.getStringOr("place", ""); s.role = t.getStringOr("role", ""); s.dim = t.getStringOr("dim", "");
            s.x = t.getDoubleOr("x", 0); s.y = t.getDoubleOr("y", 0); s.z = t.getDoubleOr("z", 0); s.saved = t.getLongOr("saved", 0);
            s.equip = t.getCompoundOrEmpty("equip").copy();
            s.snapshot = t.getCompound("snapshot").map(CompoundTag::copy).orElse(null);
            return s;
        }
    }

    public final UUID account;
    public final TreeMap<Integer, Slot> slots = new TreeMap<>();
    private final Path file;

    private CharacterStore(UUID account, Path file) {
        this.account = account;
        this.file = file;
    }

    public static Path directory(MinecraftServer server) {
        return NetworkConfig.current.enabled
                ? Path.of(NetworkConfig.current.sharedDirectory).toAbsolutePath().resolveSibling("characters")
                : server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/aeloria-characters");
    }

    public static CharacterStore load(MinecraftServer server, UUID account) throws IOException {
        Path dir = directory(server);
        Files.createDirectories(dir);
        var store = new CharacterStore(account, dir.resolve(account + ".dat"));
        if (Files.exists(store.file)) {
            try {
                CompoundTag root = NbtIo.readCompressed(new java.io.ByteArrayInputStream(Files.readAllBytes(store.file)), NbtAccounter.create(256L * 1024 * 1024));
                var list = root.getListOrEmpty("slots");
                for (int i = 0; i < list.size(); i++) {
                    var s = Slot.load(list.getCompoundOrEmpty(i));
                    if (s.slot < 0 || s.slot >= 12) throw new IOException("Emplacement invalide");
                    store.slots.put(s.slot, s);
                }
            } catch (RuntimeException e) {
                throw new IOException("Fiche de personnages illisible : intervention administrateur necessaire", e);
            }
        }
        return store;
    }

    public void write() throws IOException {
        var root = new CompoundTag();
        root.putInt("version", 1);
        var list = new ListTag();
        slots.values().forEach(s -> list.add(s.save()));
        root.put("slots", list);
        var bytes = new ByteArrayOutputStream();
        NbtIo.writeCompressed(root, bytes);
        Path tmp = Files.createTempFile(file.getParent(), "chars-", ".tmp");
        try {
            Files.write(tmp, bytes.toByteArray());
            if (Files.exists(file)) Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
