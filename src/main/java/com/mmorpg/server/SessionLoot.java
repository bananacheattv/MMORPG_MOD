package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.dungeon.InstanceManager;
import com.mmorpg.item.RpgEquipment;
import com.mmorpg.mine.MineManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Butin de session : dans les mines et dans les donjons, mourir fait perdre ce qu'on y a recupere depuis son arrivee (jamais
 * l'equipement ni ce qu'on avait deja). Dans les mines on ne perd que les lingots extraits ; dans un donjon, tout le butin ramasse
 * (hors armes et armures). Une « session » commence quand on entre dans la mine ou dans un donjon et se termine quand on en sort.
 * La comparaison porte sur l'inventaire et les sacs a dos : peu importe d'ou vient un objet (minage, coffre, monstre), il compte s'il est
 * arrive depuis l'entree. Se deconnecter ne ferme pas la session (sinon une reconnexion sur place mettrait le butin a l'abri).
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class SessionLoot {
    public enum Kind { NONE, MINE, DUNGEON }

    private record Session(Kind kind, Map<String, Integer> base) {
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    /** Les deux premieres cases du conteneur des sacs portent les sacs eux-memes. */
    private static final int BAG_FIRST = 2;

    private SessionLoot() {
    }

    private static Kind kindOf(ServerPlayer p) {
        if (!p.level().dimension().equals(InstanceManager.DIM)) return Kind.NONE;
        return MineManager.inMine(p) ? Kind.MINE : Kind.DUNGEON;
    }

    private static boolean counts(Kind kind, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (kind == Kind.MINE) return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().startsWith("lingot_");
        return !(stack.getItem() instanceof RpgEquipment);
    }

    private static Map<String, Integer> snapshot(ServerPlayer p, Kind kind) {
        Map<String, Integer> m = new HashMap<>();
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (counts(kind, s)) m.merge(BuiltInRegistries.ITEM.getKey(s.getItem()).toString(), s.getCount(), Integer::sum);
        }
        var bag = Backpacks.inventory(p);                                  // ranger le butin dans un sac ne le protege pas
        for (int i = BAG_FIRST; i < bag.getContainerSize(); i++) {
            ItemStack s = bag.getItem(i);
            if (counts(kind, s)) m.merge(BuiltInRegistries.ITEM.getKey(s.getItem()).toString(), s.getCount(), Integer::sum);
        }
        return m;
    }

    /** Verifie une fois par seconde si le joueur entre dans une zone a risque (photo du sac) ou en sort (fin de session). */
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 7) return;
        Kind k = kindOf(p);
        Session s = SESSIONS.get(p.getUUID());
        if (s != null && s.kind() != k) {
            SESSIONS.remove(p.getUUID());
            s = null;
        }
        if (s == null && k != Kind.NONE && p.isAlive()) SESSIONS.put(p.getUUID(), new Session(k, snapshot(p, k)));
    }

    /** Deconnexion : la session reste ouverte tant qu'on est dans la zone, sauf depart vers un autre serveur. */
    public static void logout(ServerPlayer p) {
        if (com.mmorpg.network.NetworkPlayers.inTransit(p) || kindOf(p) == Kind.NONE) SESSIONS.remove(p.getUUID());
    }

    /** Arrivee depuis un autre serveur : une session laissee ici auparavant ne vaut plus. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && com.mmorpg.network.NetworkPlayers.isArriving(p)) SESSIONS.remove(p.getUUID());
    }

    /** Appelee a la mort : retire du sac et laisse tomber sur place le butin de la session. @return « id*quantite;... » pour l'ecran de mort (vide si rien) */
    public static String onDeath(ServerPlayer p) {
        Session s = SESSIONS.remove(p.getUUID());
        if (s == null) return "";
        Map<String, Integer> now = snapshot(p, s.kind());
        Map<String, Integer> lost = new LinkedHashMap<>();
        for (var e : now.entrySet()) {
            int diff = e.getValue() - s.base().getOrDefault(e.getKey(), 0);
            if (diff > 0) lost.put(e.getKey(), diff);
        }
        if (lost.isEmpty()) return "";
        var inv = p.getInventory();
        var bag = Backpacks.inventory(p);
        boolean bagChanged = false;
        StringBuilder sb = new StringBuilder();
        for (var e : lost.entrySet()) {
            int left = e.getValue(), dropped = 0;
            for (Container c : new Container[]{inv, bag}) {                    // l'inventaire d'abord, puis les sacs
                for (int i = c == bag ? BAG_FIRST : 0; i < c.getContainerSize() && left > 0; i++) {
                    ItemStack st = c.getItem(i);
                    if (st.isEmpty() || !BuiltInRegistries.ITEM.getKey(st.getItem()).toString().equals(e.getKey())) continue;
                    int take = Math.min(left, st.getCount());
                    // une pile par case : jamais plus qu'une pile pleine, et chaque objet garde ses propres donnees
                    var item = new ItemEntity(p.level(), p.getX(), p.getY() + 0.5, p.getZ(), st.copyWithCount(take));
                    item.setDefaultPickUpDelay();
                    p.level().addFreshEntity(item);
                    st.shrink(take);
                    left -= take;
                    dropped += take;
                    bagChanged |= c == bag;
                }
            }
            if (dropped > 0) {
                if (sb.length() > 0) sb.append(';');
                sb.append(e.getKey()).append('*').append(dropped);
            }
        }
        if (bagChanged) bag.setChanged();                                  // reecrit les sacs dans la fiche du joueur
        return sb.toString();
    }
}
