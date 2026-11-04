package com.mmorpg.economy;

import com.mmorpg.MMORPG;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Courrier d'Aeloria (enregistre avec le monde, partage par la base quand elle est active) : les lettres de chaque joueur
 * (message, or, objets), qu'il soit en ligne ou non, et l'annuaire des pseudonymes connus pour trouver un destinataire hors ligne.
 */
public class MailData extends SavedData {
    /** Une lettre : expediteur (nul pour une lettre du systeme, comme un achat de l'HDV), message, or et objets joints. */
    public record Letter(long id, UUID from, String fromName, String message, long gold, List<ItemStack> items, long sent, boolean system) {
        public static final Codec<Letter> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("id").forGetter(Letter::id),
                UUIDUtil.STRING_CODEC.optionalFieldOf("from", new UUID(0, 0)).forGetter(Letter::from),
                Codec.STRING.fieldOf("fromName").forGetter(Letter::fromName),
                Codec.STRING.optionalFieldOf("message", "").forGetter(Letter::message),
                Codec.LONG.optionalFieldOf("gold", 0L).forGetter(Letter::gold),
                Codec.list(ItemStack.OPTIONAL_CODEC).optionalFieldOf("items", List.of()).forGetter(Letter::items),
                Codec.LONG.fieldOf("sent").forGetter(Letter::sent),
                Codec.BOOL.optionalFieldOf("system", false).forGetter(Letter::system)
        ).apply(i, Letter::new));
    }

    private static final Codec<MailData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.optionalFieldOf("next", 1L).forGetter(d -> d.next),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.list(Letter.CODEC)).optionalFieldOf("boxes", Map.of()).forGetter(d -> d.boxes),
            Codec.unboundedMap(Codec.STRING, UUIDUtil.STRING_CODEC).optionalFieldOf("names", Map.of()).forGetter(d -> d.names)
    ).apply(i, MailData::new));
    public static final SavedDataType<MailData> TYPE = new SavedDataType<>(MMORPG.id("courrier"), MailData::new, CODEC);

    private long next;
    private final Map<UUID, List<Letter>> boxes = new HashMap<>();
    private final Map<String, UUID> names = new HashMap<>();

    public MailData() {
        this.next = 1;
    }

    private MailData(long next, Map<UUID, List<Letter>> boxes, Map<String, UUID> names) {
        this.next = next;
        boxes.forEach((k, v) -> this.boxes.put(k, new ArrayList<>(v)));
        this.names.putAll(names);
    }

    public static MailData get(MinecraftServer server) {
        MailData data = server.overworld().getDataStorage().computeIfAbsent(TYPE);
        if (com.mmorpg.db.SharedDocs.useDb() && (data.syncedWith != server || data.outdated())) data.syncWithDatabase(server);
        return data;
    }

    // ---- base de donnees : le document « mail » fait foi ; s'il n'existe pas encore, les donnees du monde y sont importees
    private MinecraftServer syncedWith;
    private MinecraftServer mirrorServer;
    private boolean loading;
    private long syncedRevision = -1;

    private boolean outdated() {
        return com.mmorpg.db.SharedDocs.revision("mail") > syncedRevision;
    }

    private void syncWithDatabase(MinecraftServer server) {
        syncedWith = server;
        var ops = server.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        var doc = com.mmorpg.db.SharedDocs.read("mail");
        String json = doc == null ? null : doc.data();
        if (json == null) {
            if (doc == null || !com.mmorpg.db.Database.ready()) { syncedWith = null; return; }
            mirrorServer = server;
            syncedRevision = doc.revision();
            push();
            MMORPG.LOGGER.info("[MMORPG] Base de donnees : courrier importe depuis le monde");
            return;
        }
        try {
            MailData read = CODEC.parse(ops, com.google.gson.JsonParser.parseString(json)).getOrThrow();
            loading = true;
            next = read.next;
            boxes.clear();
            boxes.putAll(read.boxes);
            names.clear();
            names.putAll(read.names);
            syncedRevision = doc.revision();
            super.setDirty();
        } catch (RuntimeException e) {
            syncedWith = null;
            MMORPG.LOGGER.error("[MMORPG] Base de donnees : courrier illisible, donnees du monde conservees", e);
        } finally {
            loading = false;
        }
        mirrorServer = server;
    }

    @Override
    public void setDirty() {
        super.setDirty();
        if (!loading && mirrorServer != null) push();
    }

    private void push() {
        try {
            var ops = mirrorServer.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
            com.mmorpg.db.SharedDocs.put("mail", CODEC.encodeStart(ops, this).getOrThrow().toString());
            syncedRevision++;
        } catch (RuntimeException e) {
            MMORPG.LOGGER.error("[MMORPG] Base de donnees : courrier non ecrit", e);
        }
    }

    // ---- lettres
    public List<Letter> box(UUID player) {
        return boxes.getOrDefault(player, List.of());
    }

    public int count(UUID player) {
        return box(player).size();
    }

    public long nextId() {
        setDirty();
        return next++;
    }

    public void add(UUID to, Letter letter) {
        boxes.computeIfAbsent(to, k -> new ArrayList<>()).add(letter);
        setDirty();
    }

    public Letter find(UUID player, long id) {
        for (Letter l : box(player)) if (l.id() == id) return l;
        return null;
    }

    public void remove(UUID player, Letter letter) {
        List<Letter> l = boxes.get(player);
        if (l == null) return;
        l.remove(letter);
        if (l.isEmpty()) boxes.remove(player);
        setDirty();
    }

    /** Remplace une lettre (apres recuperation partielle : l'or est pris, des objets restent faute de place). */
    public void replace(UUID player, Letter old, Letter now) {
        List<Letter> l = boxes.get(player);
        if (l == null) return;
        int i = l.indexOf(old);
        if (i >= 0) l.set(i, now);
        setDirty();
    }

    // ---- annuaire des pseudonymes
    public void remember(UUID id, String name) {
        if (name == null || name.isEmpty()) return;
        UUID old = names.put(name.toLowerCase(Locale.ROOT), id);
        if (!id.equals(old)) setDirty();
    }

    public UUID lookup(String name) {
        return names.get(name.toLowerCase(Locale.ROOT));
    }
}
