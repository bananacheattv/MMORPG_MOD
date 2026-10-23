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

    private static final Codec<AuctionData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.optionalFieldOf("next", 1L).forGetter(d -> d.next),
            Listing.CODEC.listOf().optionalFieldOf("listings", List.of()).forGetter(d -> d.listings),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.LONG).optionalFieldOf("earnings", Map.of()).forGetter(d -> d.earnings),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, ItemStack.CODEC.listOf()).optionalFieldOf("returns", Map.of()).forGetter(d -> d.returns)
    ).apply(i, AuctionData::new));
    public static final SavedDataType<AuctionData> TYPE = new SavedDataType<>(MMORPG.id("hdv"), AuctionData::new, CODEC);

    private long next;
    private final List<Listing> listings;
    private final Map<UUID, Long> earnings;
    private final Map<UUID, List<ItemStack>> returns;

    public AuctionData() {
        this.next = 1;
        this.listings = new ArrayList<>();
        this.earnings = new HashMap<>();
        this.returns = new HashMap<>();
    }

    private AuctionData(long next, List<Listing> listings, Map<UUID, Long> earnings, Map<UUID, List<ItemStack>> returns) {
        this.next = next;
        this.listings = new ArrayList<>(listings);
        this.earnings = new HashMap<>(earnings);
        this.returns = new HashMap<>();
        returns.forEach((k, v) -> this.returns.put(k, new ArrayList<>(v)));
    }

    public static AuctionData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
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
