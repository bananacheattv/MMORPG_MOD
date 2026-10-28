package com.mmorpg.store;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import java.nio.file.*;
import java.util.*;

/** Shared, editable catalogue; staff ranks are deliberately outside the sale catalogue. */
public final class StoreConfig {
    public static class Rank {public String id,label,cosmetic;public int color,auctionSlots;Rank(String i,String l,int c,int slots,String cosmetic){id=i;label=l;color=c;auctionSlots=slots;this.cosmetic=cosmetic;}}
    public static class Product {public String id,label,item,category;public int count=1,rank=-1;public long credits;Product(String id,String label,String item,String category,long credits,int count){this.id=id;this.label=label;this.item=item;this.category=category;this.credits=credits;this.count=count;}}
    public static class Config {public String websiteUrl="";public boolean paymentBridgeEnabled=false;public int paymentBridgePort=8099;public List<Rank> ranks=new ArrayList<>();public List<Product> products=new ArrayList<>();public Map<String,Long> creditPacks=new LinkedHashMap<>();}
    public static Config defaults(){var c=new Config();
        c.ranks.add(new Rank("aelorien","Aelorien",0xB9AFCC,12,""));c.ranks.add(new Rank("eclaireur","Éclaireur",0x74CF96,16,"trainee_notes"));c.ranks.add(new Rank("gardien","Gardien",0x79C9F2,20,"halo_dore"));c.ranks.add(new Rank("arcaniste","Arcaniste",0xBA8CFF,24,"aura_arcane"));c.ranks.add(new Rank("elu","Élu",0xF4CA69,30,"cape_royale"));
        String[] keys={"commune","rare","epique","legendaire","mythique","vote","quete"};for(int i=0;i<keys.length;i++)c.products.add(new Product("cle_"+keys[i],"Clé "+keys[i],"mmorpg:cle_"+keys[i],"Clés",50L*(i+1),1));
        for(String id:new String[]{"charme_chance","charme_experience"})c.products.add(new Product(id,id.replace('_',' '),"mmorpg:"+id,"Charmes",100,1));
        c.products.add(new Product("rune_classe","Rune de changement de classe","mmorpg:rune_changement_classe","Runes",300,1));
        for(String id:new String[]{"potion_soin_mineure","potion_soin","potion_soin_majeure","potion_mana_mineure","potion_mana","potion_mana_majeure"})c.products.add(new Product(id,id.replace('_',' '),"mmorpg:"+id,"Potions",30,5));
        for(var tier:com.mmorpg.block.LuckyTier.values())c.products.add(new Product(tier.id,"Lucky Block "+tier.label,"mmorpg:"+tier.id,"Lucky Blocks",100L*(tier.ordinal()+1),1));
        for(int i=1;i<5;i++){var p=new Product("grade_"+c.ranks.get(i).id,"Grade "+c.ranks.get(i).label,"","Grades",new long[]{0,500,1200,2500,5000}[i],1);p.rank=i;c.products.add(p);}
        c.creditPacks.put("decouverte",500L);c.creditPacks.put("aventurier",1200L);c.creditPacks.put("legende",3000L);return c;
    }
    public static Path path(MinecraftServer server){return com.mmorpg.guild.GuildStore.directory(server).resolveSibling("commerce").resolve("boutique.json");}
    public static synchronized Config load(MinecraftServer server){try{
        Path p=path(server);Files.createDirectories(p.getParent());try(var channel=java.nio.channels.FileChannel.open(p.resolveSibling("boutique.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);var lock=channel.lock()){if(!Files.exists(p))try{Files.writeString(p,new GsonBuilder().setPrettyPrinting().create().toJson(defaults()),StandardOpenOption.CREATE_NEW);}catch(FileAlreadyExistsException ignored){}
        var c=new com.google.gson.Gson().fromJson(Files.readString(p),Config.class);validate(c);return c;}
    }catch(java.io.IOException e){throw new IllegalStateException("Catalogue indisponible",e);}}
    public static void validate(Config c){
        if(c==null||c.ranks==null||c.ranks.size()!=5||!"aelorien".equals(c.ranks.getFirst().id)||c.products==null||c.products.size()>150||c.creditPacks==null)throw new IllegalArgumentException("Catalogue : cinq grades dont Aelorien sont requis.");
        Set<String> ids=new HashSet<>();for(var r:c.ranks)if(r==null||r.label==null||r.label.length()>24||r.auctionSlots<12||r.auctionSlots>60||r.cosmetic==null||!r.cosmetic.isEmpty()&&com.mmorpg.cosmetic.Cosmetic.byId(r.cosmetic)==null)throw new IllegalArgumentException("Avantage de grade invalide.");
        for(var p:c.products){if(p==null||p.id==null||!ids.add(p.id)||!p.id.matches("[a-z0-9_]{1,50}")||p.label==null||p.label.length()>64||p.credits<=0||p.credits>1000000||p.count<1||p.count>64||p.rank< -1||p.rank>4||p.rank==0)throw new IllegalArgumentException("Produit invalide.");
            if(p.rank<0&&(p.item==null||com.mmorpg.crafting.ForgeRecipes.resolveItem(p.item)==net.minecraft.world.item.Items.AIR||com.mmorpg.server.RetiredItems.id(p.item)))throw new IllegalArgumentException("Objet indisponible : "+p.item);}
        if(c.paymentBridgePort<1024||c.paymentBridgePort>65535||c.creditPacks.size()>30||c.creditPacks.values().stream().anyMatch(n->n==null||n<=0||n>1000000))throw new IllegalArgumentException("Configuration crédits invalide.");
        if(c.websiteUrl==null||!c.websiteUrl.isEmpty()&&!c.websiteUrl.startsWith("https://"))throw new IllegalArgumentException("Le site doit utiliser HTTPS.");
    }
}
