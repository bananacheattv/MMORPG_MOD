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

/** Comptes de la Banque d'Aeloria (enregistres avec le monde) : un solde d'or et trois pages de coffre par joueur. */
public class BankData extends SavedData {
    public static final int PAGE_SLOTS = 54;
    public static final int PAGES = 3;

    /** Compte d'un joueur : or en banque et contenu des pages du coffre (PAGES x PAGE_SLOTS cases). */
    public static final class Account {
        public long gold;
        public final List<ItemStack> items = new ArrayList<>();

        Account() {
            for (int i = 0; i < PAGES * PAGE_SLOTS; i++) items.add(ItemStack.EMPTY);
        }

        Account(long gold, List<ItemStack> saved) {
            this();
            this.gold = gold;
            for (int i = 0; i < Math.min(saved.size(), items.size()); i++) items.set(i, saved.get(i));
        }

        public static final Codec<Account> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.optionalFieldOf("gold", 0L).forGetter(a -> a.gold),
                ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(a -> a.items)
        ).apply(i, Account::new));
    }

    private static final Codec<BankData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Account.CODEC).optionalFieldOf("accounts", Map.of()).forGetter(d -> d.accounts)
    ).apply(i, BankData::new));
    public static final SavedDataType<BankData> TYPE = new SavedDataType<>(MMORPG.id("banque"), BankData::new, CODEC);

    private final Map<UUID, Account> accounts;

    public BankData() {
        this.accounts = new HashMap<>();
    }

    private BankData(Map<UUID, Account> accounts) {
        this.accounts = new HashMap<>(accounts);
    }

    public static BankData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** Le compte du joueur s'il existe (sans en creer un). */
    public Account peek(UUID player) {
        return accounts.get(player);
    }

    /** Compte en texte (SNBT) pour la base de donnees du reseau. */
    public static String encode(Account a, net.minecraft.core.HolderLookup.Provider registries) {
        var ops = net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, registries);
        return Account.CODEC.encodeStart(ops, a).getOrThrow().toString();
    }

    /** Remplace le compte du joueur par celui lu dans la base ; faux si le texte est illisible (le compte local est alors garde). */
    public boolean replace(UUID player, String snbt, net.minecraft.core.HolderLookup.Provider registries) {
        try {
            var tag = net.minecraft.nbt.TagParser.parseCompoundFully(snbt);
            var ops = net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, registries);
            Account a = Account.CODEC.parse(ops, tag).getOrThrow();
            accounts.put(player, a);
            setDirty();
            return true;
        } catch (Exception e) {
            MMORPG.LOGGER.warn("[MMORPG] Compte bancaire de {} illisible dans la base : {}", player, e.getMessage());
            return false;
        }
    }

    public Account account(UUID player) {
        return accounts.computeIfAbsent(player, k -> new Account());
    }

    public void replaceAccount(UUID player, Account account) {
        accounts.put(player, account);
        setDirty();
    }
}
