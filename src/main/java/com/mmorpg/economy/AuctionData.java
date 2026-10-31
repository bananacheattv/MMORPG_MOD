package com.mmorpg.economy;

import com.mmorpg.MMORPG;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Hotel des ventes d'Aeloria (enregistre avec le monde) : annonces en cours, or des ventes a recuperer et objets rendus
 * (annonces annulees ou expirees), par vendeur.
 */
public class AuctionData extends SavedData {
    /** Une annonce : objet, prix total (or), vendeur et date de fin (millisecondes). */
    public record Listing(long id, UUID seller, String sellerName, ItemStack stack, long price, long expires) {
        static final Codec<Listing> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("id").forGetter(Listing::id),
                UUIDUtil.STRING_CODEC.fieldOf("seller").forGetter(Listing::seller),
                Codec.STRING.fieldOf("sellerName").forGetter(Listing::sellerName),
                ItemStack.CODEC.fieldOf("stack").forGetter(Listing::stack),
                Codec.LONG.fieldOf("price").forGetter(Listing::price),
                Codec.LONG.fieldOf("expires").forGetter(Listing::expires)
        ).apply(i, Listing::new));
    }

    /** Liste lue element par element : un element illisible (objet retire, format change) est garde tel quel au lieu de faire perdre toute la liste. */
    private record Lenient<T>(List<T> ok, List<Dynamic<?>> lost) {
    }

    private static <T> Codec<Lenient<T>> lenient(Codec<T> element, String what) {
        return new Codec<>() {
            @Override
            public <O> DataResult<Pair<Lenient<T>, O>> decode(DynamicOps<O> ops, O input) {
                var items = ops.getStream(input);
                if (items.error().isPresent()) return DataResult.error(() -> "liste attendue");
                List<T> ok = new ArrayList<>();
                List<Dynamic<?>> lost = new ArrayList<>();
                items.getOrThrow().forEach(e -> {
                    var r = element.parse(ops, e);
                    if (r.result().isPresent()) ok.add(r.result().get());
                    else {
                        lost.add(new Dynamic<>(ops, e));
                        MMORPG.LOGGER.error("[MMORPG] Hotel des ventes : {} illisible, conserve tel quel ({})", what, r.error().map(DataResult.Error::message).orElse("?"));
                    }
                });
                return DataResult.success(Pair.of(new Lenient<>(ok, lost), ops.empty()));
            }

            @Override
            public <O> DataResult<O> encode(Lenient<T> in, DynamicOps<O> ops, O prefix) {
                List<O> out = new ArrayList<>();
                for (T t : in.ok()) {
                    var r = element.encodeStart(ops, t);
                    if (r.result().isPresent()) out.add(r.result().get());
                }
                for (Dynamic<?> d : in.lost()) out.add(d.convert(ops).getValue());
                return DataResult.success(ops.createList(out.stream()));
            }
        };
    }

    private static final Codec<AuctionData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.optionalFieldOf("next", 1L).forGetter(d -> d.next),
            lenient(Listing.CODEC, "annonce").optionalFieldOf("listings", new Lenient<>(List.of(), List.of())).forGetter(d -> new Lenient<>(d.listings, d.lostListings)),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.LONG).optionalFieldOf("earnings", Map.of()).forGetter(d -> d.earnings),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, lenient(ItemStack.CODEC, "objet rendu")).optionalFieldOf("returns", Map.of()).forGetter(d -> {
                Map<UUID, Lenient<ItemStack>> m = new HashMap<>();
                d.returns.forEach((k, v) -> m.put(k, new Lenient<>(v, d.lostReturns.getOrDefault(k, List.of()))));
                d.lostReturns.forEach((k, v) -> m.putIfAbsent(k, new Lenient<>(List.of(), v)));
                return m;
            })
    ).apply(i, AuctionData::new));
    public static final SavedDataType<AuctionData> TYPE = new SavedDataType<>(MMORPG.id("hdv"), AuctionData::new, CODEC);

    private long next;
    private final List<Listing> listings;
    private final Map<UUID, Long> earnings;
    private final Map<UUID, List<ItemStack>> returns;
    /** Elements illisibles gardes tels quels (jamais perdus) : annonces et objets rendus. */
    private final List<Dynamic<?>> lostListings = new ArrayList<>();
    private final Map<UUID, List<Dynamic<?>>> lostReturns = new HashMap<>();

    public AuctionData() {
        this.next = 1;
        this.listings = new ArrayList<>();
        this.earnings = new HashMap<>();
        this.returns = new HashMap<>();
    }

    private AuctionData(long next, Lenient<Listing> listings, Map<UUID, Long> earnings, Map<UUID, Lenient<ItemStack>> returns) {
        this.next = next;
        this.listings = new ArrayList<>(listings.ok());
        this.lostListings.addAll(listings.lost());
        this.earnings = new HashMap<>(earnings);
        this.returns = new HashMap<>();
        returns.forEach((k, v) -> {
            this.returns.put(k, new ArrayList<>(v.ok()));
            if (!v.lost().isEmpty()) lostReturns.put(k, new ArrayList<>(v.lost()));
        });
    }

    public static AuctionData get(MinecraftServer server) {
        AuctionData data = server.overworld().getDataStorage().computeIfAbsent(TYPE);
        if (com.mmorpg.db.SharedDocs.useDb() && (data.syncedWith != server || data.outdated())) data.syncWithDatabase(server);
        return data;
    }

    /** Un autre serveur (menu d'administration, autre hotel des ventes) a ecrit le document depuis notre derniere lecture ou ecriture. */
    private boolean outdated() {
        return com.mmorpg.db.SharedDocs.revision("auction") > syncedRevision;
    }

    // ---- base de donnees : le document « auction » fait foi ; s'il n'existe pas encore, les donnees du monde y sont importees
    private MinecraftServer syncedWith;
    private MinecraftServer mirrorServer;
    private boolean loading;
    /** Revision du document en base correspondant a notre copie (chaque ecriture l'augmente de 1). */
    private long syncedRevision = -1;

    /** Relit l'hotel des ventes depuis la base (verifications et reprise manuelle). */
    public void reloadFromDatabase(MinecraftServer server) {
        mirrorServer = null;
        syncWithDatabase(server);
    }

    private void syncWithDatabase(MinecraftServer server) {
        syncedWith = server;
        var ops = server.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        var doc = com.mmorpg.db.SharedDocs.read("auction");
        String json = doc == null ? null : doc.data();
        if (json == null) {
            if (doc == null || !com.mmorpg.db.Database.ready()) { syncedWith = null; return; }   // base injoignable : on reessaiera, les donnees du monde restent valides
            mirrorServer = server;
            syncedRevision = doc.revision();
            push();
            MMORPG.LOGGER.info("[MMORPG] Base de donnees : hotel des ventes importe depuis le monde ({} annonce(s))", listings.size());
            return;
        }
        try {
            AuctionData read = CODEC.parse(ops, com.google.gson.JsonParser.parseString(json)).getOrThrow();
            loading = true;
            next = read.next;
            listings.clear();
            listings.addAll(read.listings);
            earnings.clear();
            earnings.putAll(read.earnings);
            returns.clear();
            returns.putAll(read.returns);
            lostListings.clear();
            lostListings.addAll(read.lostListings);
            lostReturns.clear();
            lostReturns.putAll(read.lostReturns);
            syncedRevision = doc.revision();
            super.setDirty();
            MMORPG.LOGGER.info("[MMORPG] Base de donnees : hotel des ventes lu ({} annonce(s))", listings.size());
        } catch (RuntimeException e) {
            syncedWith = null;
            MMORPG.LOGGER.error("[MMORPG] Base de donnees : hotel des ventes illisible, donnees du monde conservees", e);
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
            com.mmorpg.db.SharedDocs.put("auction", CODEC.encodeStart(ops, this).getOrThrow().toString());
            syncedRevision++;                                     // notre propre ecriture ne doit pas declencher de relecture
        } catch (RuntimeException e) {
            MMORPG.LOGGER.error("[MMORPG] Base de donnees : hotel des ventes non ecrit", e);
        }
    }

    public List<Listing> listings() {
        return listings;
    }

    public Listing find(long id) {
        for (Listing l : listings) if (l.id() == id) return l;
        return null;
    }

    public long nextId() {
        setDirty();
        return next++;
    }

    public void add(Listing l) {
        listings.add(l);
        setDirty();
    }

    public void remove(Listing l) {
        listings.remove(l);
        setDirty();
    }

    public long earnings(UUID player) {
        return earnings.getOrDefault(player, 0L);
    }

    public void addEarnings(UUID player, long gold) {
        earnings.merge(player, gold, Long::sum);
        setDirty();
    }

    public long takeEarnings(UUID player) {
        Long v = earnings.remove(player);
        if (v != null) setDirty();
        return v == null ? 0 : v;
    }

    public void addReturn(UUID player, ItemStack stack) {
        returns.computeIfAbsent(player, k -> new ArrayList<>()).add(stack.copy());
        setDirty();
    }

    public List<ItemStack> takeReturns(UUID player) {
        List<ItemStack> l = returns.remove(player);
        if (l != null) setDirty();
        return l == null ? List.of() : l;
    }

    public int returnCount(UUID player) {
        List<ItemStack> l = returns.get(player);
        return l == null ? 0 : l.size();
    }
}
