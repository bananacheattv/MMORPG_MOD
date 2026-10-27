package com.mmorpg.client.screen;

import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.*;
import com.mmorpg.config.ShopConfig;
import com.mmorpg.network.Payloads;
import com.mmorpg.entity.NpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.*;

/** Item icons and numeric controls, with four real NPC model previews. No item IDs to type. */
public final class MerchantEditorScreen extends MmoScreen {
    private final int npc;private final String revision;private final ShopConfig shop;
    private final NpcEntity[] previews=new NpcEntity[4];
    private String merchantName,status,selected="";private int skin,tab,page;private boolean selling=true,buying=true;
    private EditBox search,name,quantity,price,resale,level;
    private record Entry(Item item,String id,String label,String match) { }
    private List<Entry> entries;
    public MerchantEditorScreen(CompoundTag data) {
        super("Configurer le marchand");npc=data.getIntOr("npc",-1);revision=data.getStringOr("revision","");
        merchantName=data.getStringOr("name","Marchand");skin=data.getIntOr("skin",0);status=data.getStringOr("status","");
        shop=new com.google.gson.Gson().fromJson(data.getStringOr("shop","{}"),ShopConfig.class);
    }
    public void showTab(int value) {tab=Math.clamp(value,0,2);page=0;}
    @Override protected int panelWidth(){return Math.min(width-12,720);}
    @Override protected int panelHeight(){return Math.min(height-12,340);}
    private int catalogueWidth(){return pw*53/100;}
    private EditBox field(int x,int y,int w,String label,String value,boolean numeric) {
        var box=new EditBox(font,x,y,w,16,Component.literal(label));box.setMaxLength(numeric?8:80);
        box.setValue(value);addRenderableWidget(box);return box;
    }
    @Override protected void init() {
        super.init();int cw=catalogueWidth(),rx=left+cw+8,rw=pw-cw-20;
        search=field(left+10,top+50,cw-18,"Rechercher un objet",search==null?"":search.getValue(),false);search.setResponder(s->page=0);
        name=field(left+12,top+57,pw-24,"Nom du marchand",name==null?merchantName:name.getValue(),false);
        quantity=field(rx+rw-55,top+72,55,"Quantité",quantity==null?"1":quantity.getValue(),true);
        price=field(rx+rw-55,top+92,55,"Prix de vente",price==null?"1":price.getValue(),true);
        resale=field(rx+rw-55,top+112,55,"Prix de rachat",resale==null?"1":resale.getValue(),true);
        level=field(rx+rw-55,top+132,55,"Niveau minimum",level==null?"1":level.getValue(),true);
    }
    private List<Entry> catalogue() {
        if(entries==null) {
            var list=new ArrayList<Entry>();for(Item item:BuiltInRegistries.ITEM)if(item!=Items.AIR&&!com.mmorpg.server.RetiredItems.item(item)) {
                String id=BuiltInRegistries.ITEM.getKey(item).toString(),label=new ItemStack(item).getHoverName().getString();
                if(com.mmorpg.server.CrateOnly.is(item)||item instanceof com.mmorpg.item.NpcEditorItem||item instanceof com.mmorpg.item.SpawnEditorItem)continue;
                list.add(new Entry(item,id,label,(label+" "+id).toLowerCase(Locale.ROOT)));
            }
            list.sort(Comparator.comparingInt((Entry e)->com.mmorpg.economy.ShopSorting.category(e.item()))
                .thenComparingInt(e->com.mmorpg.economy.ShopSorting.level(e.item())).thenComparing(Entry::label));entries=List.copyOf(list);
        }
        String filter=search.getValue().toLowerCase(Locale.ROOT);
        return entries.stream().filter(e->e.match().contains(filter)&&(tab==0||shop.buy.stream().anyMatch(o->o.item.equals(e.id()))||shop.sellPrices.containsKey(e.id()))).toList();
    }
    private void select(Entry entry) {
        if(!selected.isEmpty()&&!applyItem())return;
        selected=entry.id();var offer=shop.buy.stream().filter(o->o.item.equals(selected)).findFirst().orElse(null);
        selling=offer!=null;buying=shop.sellPrices.getOrDefault(selected,0)>0;
        if(offer==null&&!shop.sellPrices.containsKey(selected)){selling=true;buying=true;}
        quantity.setValue(offer==null?"1":Integer.toString(offer.count));price.setValue(offer==null?"1":Integer.toString(offer.price));
        resale.setValue(Integer.toString(Math.max(1,shop.sellPrices.getOrDefault(selected,1))));level.setValue(offer==null?"1":Integer.toString(offer.minLevel));status="";
    }
    private boolean applyItem() {
        if(selected.isEmpty())return true;
        try {
            int count=Integer.parseInt(quantity.getValue()),cost=Integer.parseInt(price.getValue()),sell=Integer.parseInt(resale.getValue()),min=Integer.parseInt(level.getValue());
            if(count<1||count>64||cost<1||cost>10000000||sell<1||sell>10000000||min<1||min>100)throw new IllegalArgumentException();
            shop.buy.removeIf(o->o.item.equals(selected));if(selling)shop.buy.add(new ShopConfig.Offer(selected,count,cost,min));
            // Explicit zero disables fallback rarity pricing for an item which must not be bought back.
            shop.sellPrices.put(selected,buying?sell:0);status="Article mis à jour dans le brouillon.";return true;
        } catch(Exception e){status="Vérifie les nombres : quantité 1–64, niveau 1–100.";return false;}
    }
    private void save() {
        if(!applyItem())return;
        var data=new CompoundTag();data.putString("action","save");data.putInt("npc",npc);data.putString("revision",revision);
        data.putString("name",name.getValue());data.putInt("skin",skin);data.putString("shop",new com.google.gson.Gson().toJson(shop));
        if(data.getStringOr("shop","").length()>22000){status="Boutique trop grande.";return;}
        ClientNet.send(new Payloads.MerchantEdit(data));
    }
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy){page=Math.max(0,page-(int)Math.signum(sy));return true;}
    @Override protected void renderContent(GuiGraphicsExtractor g,int mx,int my,float a) {
        Ui.frame(g,left,top,pw,ph,Ui.GOLD,false);
        g.centeredText(font,Component.literal(font.plainSubstrByWidth(status.isBlank()?"CONFIGURER LE MARCHAND":status,pw-24)),left+pw/2,top+8,Ui.GOLD);
        String[] tabs={"Catalogue d'objets","Articles du marchand","Apparence"};int tw=(pw-24)/3;
        for(int i=0;i<3;i++){final int value=i;button(g,left+12+i*tw,top+27,tw-3,17,tabs[i],true,tab==i?Ui.GOLD:Ui.MUTED,()->{tab=value;page=0;});}
        boolean items=tab!=2;search.visible=items;search.active=items;name.visible=!items;name.active=!items;
        for(var field:List.of(quantity,price,resale,level)){field.visible=items&&!selected.isEmpty();field.active=field.visible;}
        if(items)drawItems(g);else drawSkins(g);
        button(g,left+12,top+ph-25,pw-24,18,"Enregistrer ce marchand",true,Ui.GREEN,this::save);
    }
    private void drawItems(GuiGraphicsExtractor g) {
        int cw=catalogueWidth(),rx=left+cw+8,rw=pw-cw-20,cols=Math.max(1,(cw-18)/23),rows=Math.max(1,(ph-152)/23),per=cols*rows;
        var list=catalogue();int pages=Math.max(1,(list.size()+per-1)/per);page=Math.min(page,pages-1);
        for(int i=0;i<per&&page*per+i<list.size();i++) {
            Entry e=list.get(page*per+i);int x=left+10+i%cols*23,y=top+73+i/cols*23;
            Ui.frame(g,x,y,21,21,e.id().equals(selected)?Ui.GOLD:Ui.MUTED,hovered(x,y,21,21));g.item(new ItemStack(e.item()),x+2,y+2);
            click(x,y,21,21,()->select(e));if(hovered(x,y,21,21))g.setTooltipForNextFrame(font,new ItemStack(e.item()),mouseX,mouseY);
        }
        button(g,left+10,top+ph-50,30,16,"‹",page>0,Ui.GOLD,()->page--);
        button(g,left+10,top+ph-73,cw-20,16,"Boutique vide (brouillon)",true,Ui.MUTED,()->{
            shop.buy.clear();shop.sellPrices.clear();shop.automaticBuyback=false;selected="";page=0;status="Ajoute tes objets puis enregistre ce marchand.";
        });
        button(g,left+cw-40,top+ph-50,30,16,"›",page+1<pages,Ui.GOLD,()->page++);
        g.centeredText(font,Component.literal((page+1)+" / "+pages),left+cw/2,top+ph-46,Ui.MUTED);
        if(selected.isEmpty()){g.text(font,"Clique sur un objet.",rx,top+77,Ui.MUTED,false);return;}
        Item item=com.mmorpg.crafting.ForgeRecipes.resolveItem(selected);var stack=new ItemStack(item);g.item(stack,rx,top+50);
        g.text(font,font.plainSubstrByWidth(stack.getHoverName().getString(),rw-24),rx+22,top+54,Ui.GOLD,false);
        String[] labels={"Quantité","Prix de vente","Prix de rachat","Niveau mini"};
        for(int i=0;i<4;i++)g.text(font,labels[i],rx,top+76+i*20,Ui.TEXT,false);
        int toggleY=Math.min(top+153,top+ph-75);
        button(g,rx,toggleY,rw/2-2,16,"Vente : "+(selling?"oui":"non"),true,Ui.GOLD,()->selling=!selling);
        button(g,rx+rw/2+2,toggleY,rw/2-2,16,"Rachat : "+(buying?"oui":"non"),true,Ui.GOLD,()->buying=!buying);
        button(g,rx,top+ph-50,rw,16,"Appliquer l'article",true,Ui.GOLD,this::applyItem);
    }
    private void drawSkins(GuiGraphicsExtractor g) {
        g.text(font,"Nom du marchand",left+12,top+47,Ui.TEXT,false);
        int cardW=(pw-32)/4,y=top+83,h=Math.max(75,ph-122);
        for(int i=0;i<4;i++) {
            final int choice=i;int x=left+10+i*(cardW+4);
            Ui.frame(g,x,y,cardW,h,skin==i?Ui.GOLD:Ui.MUTED,hovered(x,y,cardW,h));
            if(previews[i]==null&&Minecraft.getInstance().level!=null) {
                previews[i]=com.mmorpg.registry.ModEntities.NPC.get().create(Minecraft.getInstance().level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                if(previews[i]!=null){previews[i].setId(-700-i);previews[i].setup(NpcEntity.Role.MARCHAND,"",i,"");}
            }
            if(previews[i]!=null)InventoryScreen.extractEntityInInventoryFollowsMouse(g,x+2,y+2,x+cardW-2,y+h-19,Math.max(20,Math.min(42,(h-22)/2)),.0625f,mouseX,mouseY,previews[i]);
            g.centeredText(font,Component.literal("Apparence "+(i+1)),x+cardW/2,y+h-13,skin==i?Ui.GOLD:Ui.TEXT);
            click(x,y,cardW,h,()->skin=choice);
        }
    }
}
