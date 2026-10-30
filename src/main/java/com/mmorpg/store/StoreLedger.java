package com.mmorpg.store;
import com.mmorpg.network.SharedLedger;
import net.minecraft.server.MinecraftServer;
import java.util.*;

/** Wallet debits and delivery intents commit together under the cross-backend ledger lock. */
public final class StoreLedger {
    public static class Delivery {public String id=UUID.randomUUID().toString(),item;public int count;}
    public static class Account {public long credits;public int rank;public String staff="";public List<Delivery> pending=new ArrayList<>();}
    public static class CreditReceipt {public String player;public long amount;}
    public static class State {public Map<String,Account> accounts=new HashMap<>();public Map<String,CreditReceipt> credits=new HashMap<>();public Set<String> purchases=new HashSet<>();}
    public static Account account(State s,UUID player){return s.accounts.computeIfAbsent(player.toString(),k->new Account());}
    public static <T>T access(MinecraftServer server,java.util.function.Function<State,T> work){return SharedLedger.access(server,"credit-store",State.class,State::new,checked(work));}
    /** Lecture seule (grade, livraisons en attente) : rien n'est ecrit, a utiliser pour les consultations periodiques. */
    public static <T>T read(MinecraftServer server,java.util.function.Function<State,T> work){return SharedLedger.read(server,"credit-store",State.class,State::new,checked(work));}
    private static <T>java.util.function.Function<State,T> checked(java.util.function.Function<State,T> work){return s->{if(s.accounts==null||s.credits==null||s.purchases==null)throw new IllegalStateException("Registre crédits corrompu.");for(var entry:s.accounts.entrySet()){UUID.fromString(entry.getKey());var a=entry.getValue();if(a==null||a.credits<0||a.rank<0||a.rank>4||a.staff==null||!a.staff.isEmpty()&&!PremiumStore.STAFF.contains(a.staff)||a.pending==null||a.pending.size()>1000)throw new IllegalStateException("Compte crédits corrompu.");
            Set<String> ids=new HashSet<>();for(var delivery:a.pending)if(delivery==null||delivery.id==null||!ids.add(delivery.id)||delivery.item==null||delivery.count<1||delivery.count>64)throw new IllegalStateException("Livraison corrompue.");}
        return work.apply(s);};}
    public static void credit(MinecraftServer server,UUID player,long amount,String reference){
        if(amount<=0||amount>1000000||reference==null||!reference.matches("[A-Za-z0-9:_-]{3,120}"))throw new IllegalArgumentException("Crédit ou référence invalide.");
        access(server,s->{var old=s.credits.get(reference);if(old!=null){if(!old.player.equals(player.toString())||old.amount!=amount)throw new IllegalArgumentException("Référence déjà utilisée pour une autre transaction.");return null;}
            var a=account(s,player);a.credits=Math.addExact(a.credits,amount);var receipt=new CreditReceipt();receipt.player=player.toString();receipt.amount=amount;s.credits.put(reference,receipt);return null;});
    }
    public static long price(StoreConfig.Config cfg,StoreConfig.Product product,Account account){
        if(product.rank<0)return product.credits;if(product.rank<=account.rank)return 0;
        long paid=cfg.products.stream().filter(p->p.rank==account.rank).mapToLong(p->p.credits).findFirst().orElse(0);return Math.max(1,product.credits-paid);
    }
    public static void buy(MinecraftServer server,UUID player,String productId,UUID request){buy(server,player,productId,request,-1);}
    public static void buy(MinecraftServer server,UUID player,String productId,UUID request,long expectedPrice){
        var cfg=StoreConfig.load(server);var product=cfg.products.stream().filter(p->p.id.equals(productId)).findFirst().orElseThrow(()->new IllegalArgumentException("Produit inconnu."));
        access(server,s->{String receipt=player+":"+request;if(s.purchases.contains(receipt))return null;var a=account(s,player);long price=price(cfg,product,a);if(expectedPrice>=0&&price!=expectedPrice)throw new IllegalArgumentException("Le prix a changé. Vérifie le catalogue et confirme à nouveau.");
            if(product.rank>=0&&product.rank<=a.rank)throw new IllegalArgumentException("Grade déjà acquis ou supérieur.");if(a.credits<price)throw new IllegalArgumentException("Crédits insuffisants.");
            if(product.rank<0&&a.pending.size()>=100)throw new IllegalArgumentException("Récupère tes livraisons en attente avant de commander.");
            a.credits-=price;if(product.rank>=0)a.rank=product.rank;else{var d=new Delivery();d.item=product.item;d.count=product.count;a.pending.add(d);}s.purchases.add(receipt);return null;});
    }
}
