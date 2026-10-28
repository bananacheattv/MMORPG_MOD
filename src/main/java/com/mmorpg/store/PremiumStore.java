package com.mmorpg.store;
import com.mmorpg.MMORPG;
import com.mmorpg.network.*;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.*;

@EventBusSubscriber(modid=MMORPG.MODID)
public final class PremiumStore {
    public static final Set<String> STAFF=Set.of("aucun","fondateur","administrateur","moderateur","builder");
    public static void refresh(ServerPlayer p){
        var cfg=StoreConfig.load(p.level().getServer());var account=StoreLedger.access(p.level().getServer(),s->StoreLedger.account(s,p.getUUID()));var rank=cfg.ranks.get(Math.clamp(account.rank,0,4));var d=RpgPlayers.get(p);
        String label=account.staff.isEmpty()?rank.label:switch(account.staff){case "fondateur"->"Fondateur";case "administrateur"->"Administrateur";case "moderateur"->"Modérateur";case "builder"->"Builder";default->rank.label;};
        int color=account.staff.isEmpty()?rank.color:0xF08078;boolean changed=!d.rankLabel.equals(label)||d.rankColor!=color||d.rankTier!=account.rank;
        d.rankLabel=label;d.rankColor=color;d.rankTier=account.rank;d.auctionSlots=rank.auctionSlots;
        for(int i=1;i<=account.rank;i++)if(!cfg.ranks.get(i).cosmetic.isEmpty())d.cosmetics.add(cfg.ranks.get(i).cosmetic);
        if(changed){d.dirty=true;p.refreshDisplayName();p.refreshTabListName();RpgPlayers.updatePublic(p);}
    }
    public static void deliver(ServerPlayer p){
        var list=StoreLedger.access(p.level().getServer(),s->new ArrayList<>(StoreLedger.account(s,p.getUUID()).pending));
        for(var delivery:list){var d=RpgPlayers.get(p);String receipt="store:"+delivery.id;
            if(!d.guildReceipts.getBooleanOr(receipt,false)){
                var item=com.mmorpg.crafting.ForgeRecipes.resolveItem(delivery.item);if(item==net.minecraft.world.item.Items.AIR)throw new IllegalArgumentException("Livraison : objet inconnu.");
                // A free slot guarantees that delivery never drops a paid object into the world.
                if(p.getInventory().getFreeSlot()<0)return;
                var inventory=p.getInventory();var before=new ArrayList<ItemStack>();for(int i=0;i<inventory.getContainerSize();i++)before.add(inventory.getItem(i).copy());
                var stack=new ItemStack(item,delivery.count);if(!inventory.add(stack)||!stack.isEmpty()){for(int i=0;i<before.size();i++)inventory.setItem(i,before.get(i));return;}
                d.guildReceipts.putBoolean(receipt,true);d.dirty=true;
                boolean saved;if(NetworkConfig.current.enabled)saved=NetworkPlayers.save(p);else{p.level().getServer().getPlayerList().saveAll();saved=true;}
                if(!saved){for(int i=0;i<before.size();i++)inventory.setItem(i,before.get(i));d.guildReceipts.remove(receipt);throw new IllegalStateException("Livraison conservée : sauvegarde indisponible.");}
            }
            StoreLedger.access(p.level().getServer(),s->{StoreLedger.account(s,p.getUUID()).pending.removeIf(x->x.id.equals(delivery.id));return null;});
        }
    }
    public static void open(ServerPlayer p,String message){try{
        refresh(p);deliver(p);var cfg=StoreConfig.load(p.level().getServer());var a=StoreLedger.access(p.level().getServer(),s->StoreLedger.account(s,p.getUUID()));var t=new CompoundTag();
        t.putLong("credits",a.credits);t.putInt("rank",a.rank);t.putInt("pending",a.pending.size());t.putString("status",message);t.putString("website",cfg.websiteUrl);t.putBoolean("payments",cfg.paymentBridgeEnabled&&!cfg.websiteUrl.isEmpty());
        var products=new ListTag();for(var product:cfg.products){var v=new CompoundTag();v.putString("id",product.id);v.putString("label",product.label);v.putString("item",product.item);v.putString("category",product.category);v.putLong("price",StoreLedger.price(cfg,product,a));v.putInt("count",product.count);v.putBoolean("owned",product.rank>=0&&a.rank>=product.rank);
            if(product.rank>0){var r=cfg.ranks.get(product.rank);v.putString("benefits","Préfixe coloré · "+r.auctionSlots+" annonces HDV · Cosmétique de grade");}products.add(v);}t.put("products",products);
        Net.toPlayer(p,new S2COpenScreen(S2COpenScreen.PREMIUM_STORE,t));
    }catch(Exception e){p.sendSystemMessage(Component.literal("Boutique indisponible : "+e.getMessage()));}}
    public static void action(ServerPlayer p,CompoundTag data){
        if(NetworkPlayers.inTransit(p)||com.mmorpg.hub.ClassTrial.active(p)||!p.isAlive()||p.isSpectator())return;
        try{String action=data.getStringOr("action","");if(action.equals("buy")){StoreLedger.buy(p.level().getServer(),p.getUUID(),data.getStringOr("product",""),UUID.fromString(data.getStringOr("request","")),data.getLongOr("price",-1));open(p,"Achat enregistré. Les objets sans place restent en attente.");}else if(action.equals("open")||action.equals("claim"))open(p,"");}catch(Exception e){open(p,e.getMessage());}
    }
    @SubscribeEvent public static void login(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)com.mmorpg.server.Scheduler.later(p.level(),30,()->{try{refresh(p);p.refreshDisplayName();p.refreshTabListName();}catch(Exception ex){MMORPG.LOGGER.warn("Grade temporairement indisponible : {}",ex.getMessage());}});}
    @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post e){if(e.getServer().getTickCount()%100==0)for(var p:e.getServer().getPlayerList().getPlayers())try{refresh(p);deliver(p);}catch(Exception ex){MMORPG.LOGGER.warn("Boutique : {}",ex.getMessage());}}
    @SubscribeEvent public static void name(net.neoforged.neoforge.event.entity.player.PlayerEvent.NameFormat e){if(e.getEntity() instanceof ServerPlayer p){var d=RpgPlayers.get(p);e.setDisplayname(Component.literal("["+d.rankLabel+"] ").withColor(d.rankColor).append(e.getUsername().copy().withColor(0xFFFFFF)));}}
    @SubscribeEvent public static void tab(net.neoforged.neoforge.event.entity.player.PlayerEvent.TabListNameFormat e){if(e.getEntity() instanceof ServerPlayer p){var d=RpgPlayers.get(p);e.setDisplayName(Component.literal("["+d.rankLabel+"] ").withColor(d.rankColor).append(p.getGameProfile().name()));}}
    @SubscribeEvent public static void commands(net.neoforged.neoforge.event.RegisterCommandsEvent e){var d=e.getDispatcher();
        d.register(net.minecraft.commands.Commands.literal("boutique").executes(c->{open(c.getSource().getPlayerOrException(),"");return 1;}));
        d.register(net.minecraft.commands.Commands.literal("credits").executes(c->{open(c.getSource().getPlayerOrException(),"");return 1;}).then(net.minecraft.commands.Commands.literal("ajouter").requires(net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_OWNERS))
            .then(net.minecraft.commands.Commands.argument("uuid",com.mojang.brigadier.arguments.StringArgumentType.word()).then(net.minecraft.commands.Commands.argument("montant",com.mojang.brigadier.arguments.LongArgumentType.longArg(1,1000000)).then(net.minecraft.commands.Commands.argument("reference",com.mojang.brigadier.arguments.StringArgumentType.word()).executes(c->{
                StoreLedger.credit(c.getSource().getServer(),UUID.fromString(com.mojang.brigadier.arguments.StringArgumentType.getString(c,"uuid")),com.mojang.brigadier.arguments.LongArgumentType.getLong(c,"montant"),com.mojang.brigadier.arguments.StringArgumentType.getString(c,"reference"));c.getSource().sendSuccess(()->Component.literal("Crédits enregistrés ; référence protégée contre les doublons."),false);return 1;}))))));
        d.register(net.minecraft.commands.Commands.literal("grade").requires(net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_OWNERS))
            .then(net.minecraft.commands.Commands.argument("joueur",net.minecraft.commands.arguments.EntityArgument.player()).then(net.minecraft.commands.Commands.argument("staff",com.mojang.brigadier.arguments.StringArgumentType.word()).suggests((c,b)->net.minecraft.commands.SharedSuggestionProvider.suggest(STAFF,b)).executes(c->{
                var p=net.minecraft.commands.arguments.EntityArgument.getPlayer(c,"joueur");String rank=com.mojang.brigadier.arguments.StringArgumentType.getString(c,"staff");var cfg=StoreConfig.load(p.level().getServer());int tier=-1;for(int i=0;i<cfg.ranks.size();i++)if(cfg.ranks.get(i).id.equals(rank))tier=i;final int selected=tier;
                if(!STAFF.contains(rank)&&tier<0)throw new IllegalArgumentException("Grade inconnu.");StoreLedger.access(p.level().getServer(),s->{var account=StoreLedger.account(s,p.getUUID());if(selected>=0)account.rank=selected;else account.staff=rank.equals("aucun")?"":rank;return null;});refresh(p);c.getSource().sendSuccess(()->Component.literal("Grade affiché mis à jour. Les permissions opérateur restent administrées séparément."),false);return 1;}))));
    }
}
