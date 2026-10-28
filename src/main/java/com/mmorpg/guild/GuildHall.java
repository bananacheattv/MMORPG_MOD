package com.mmorpg.guild;
import com.mmorpg.MMORPG;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.item.DyeColor;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import java.util.*;

@EventBusSubscriber(modid=MMORPG.MODID)
public final class GuildHall {
    public static final ResourceKey<Level> DIM=ResourceKey.create(Registries.DIMENSION,MMORPG.id("guildes"));
    private static final Map<UUID,UUID> VISITORS=new HashMap<>();
    private static final Map<UUID,com.mmorpg.competition.CompetitionWorld.Job> BUILDS=new HashMap<>();
    public static BlockPos center(GuildStore.Guild g){return new BlockPos(500+g.hallIndex*128,100,500);}
    public static DyeColor color(String key){return switch(key){case "AZUR"->DyeColor.LIGHT_BLUE;case "EMERAUDE"->DyeColor.GREEN;case "CARMIN"->DyeColor.RED;case "OR"->DyeColor.YELLOW;default->DyeColor.PURPLE;};}
    public static void enter(ServerPlayer p){
        if(com.mmorpg.competition.ArenaManager.active(p)||com.mmorpg.competition.GuildRaids.of(p.getUUID())!=null||com.mmorpg.dungeon.InstanceManager.containsPlayer(p))throw new IllegalArgumentException("Termine ton activité avant de rejoindre le hall.");
        if(com.mmorpg.competition.CompetitionTravel.route(p,"guild-hall"))return;
        var g=GuildStore.access(p.level().getServer(),true,s->{var found=GuildStore.of(s,p.getUUID());if(found==null)throw new IllegalArgumentException("Rejoins une guilde.");if(found.hallIndex<0)found.hallIndex=s.nextHall++;return found;});
        var level=p.level().getServer().getLevel(DIM);if(level==null)throw new IllegalArgumentException("Hall indisponible.");
        BUILDS.computeIfAbsent(g.id,id->build(g));VISITORS.put(p.getUUID(),g.id);p.sendSystemMessage(Component.literal("Préparation du hall de "+g.name+"…"));
    }
    public static com.mmorpg.competition.CompetitionWorld.Job build(GuildStore.Guild g){
        var j=new com.mmorpg.competition.CompetitionWorld.Job();var c=center(g);int r=18+g.hallLevel*3;
        for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++)for(int y=-2;y<=10;y++){
            Block block=y<0?Blocks.POLISHED_DEEPSLATE:y==10?Blocks.DARK_OAK_PLANKS:Math.abs(x)==r||Math.abs(z)==r?Blocks.DEEPSLATE_BRICKS:Blocks.AIR;
            if(y>=2&&y<=6&&(Math.abs(x)==r||Math.abs(z)==r)&&Math.floorMod(x+z,5)<=2)block=Blocks.STAINED_GLASS.pick(color(g.banner));
            j.add(c.getX()+x,c.getY()+y,c.getZ()+z,block);
        }
        // Recessed light and suspended chandeliers keep every part of the hall usable without a shader.
        for(int x=-r+4;x<=r-4;x+=8)for(int z=-r+4;z<=r-4;z+=8)j.add(c.getX()+x,99,c.getZ()+z,Blocks.SEA_LANTERN);
        for(int x:new int[]{-6,6})for(int z:new int[]{-6,6}){for(int y=107;y<=109;y++)j.add(c.getX()+x,y,c.getZ()+z,Blocks.IRON_CHAIN);for(int dx=-1;dx<=1;dx++)j.add(c.getX()+x+dx,106,c.getZ()+z,Blocks.LANTERN);}
        for(int x=-r+3;x<=r-3;x+=6)for(int z:new int[]{-r+3,r-3})for(int y=0;y<=9;y++)j.add(c.getX()+x,c.getY()+y,c.getZ()+z,y==8?Blocks.SHROOMLIGHT:Blocks.DARK_OAK_LOG);
        for(int z=-4;z<=4;z++)for(int x=-1;x<=1;x++)j.add(c.getX()+x,99,c.getZ()+z,Blocks.CONCRETE.pick(color(g.banner)));
        // A pair of raised, coloured banners forms the guild crest above its council table.
        for(int x:new int[]{-3,3}){for(int y=0;y<7;y++)j.add(c.getX()+x,100+y,c.getZ()-10,Blocks.DARK_OAK_FENCE);for(int z=-10;z<=-8;z++)for(int y=104;y<=106;y++)j.add(c.getX()+x,y,c.getZ()+z,Blocks.CONCRETE.pick(color(g.banner)));}
        for(int x=-3;x<=3;x++)for(int y=0;y<=4;y++){
            boolean crest=switch(g.bannerPattern){case "SOLEIL"->x*x+(y-2)*(y-2)<=5;case "LUNE"->x*x+(y-2)*(y-2)<=6&&x<=0;case "ARBRE"->y<2?x==0:Math.abs(x)<=4-y;case "DRAGON"->Math.abs(x)==y/2||y==1;default->Math.abs(x)+Math.abs(y-2)<=2;};
            if(crest)j.add(c.getX()+x,103+y,c.getZ()-17,Blocks.CONCRETE.pick(color(g.banner)));
        }
        for(int x=-4;x<=4;x++)for(int z=-2;z<=2;z++)j.add(c.getX()+x,100,c.getZ()-8+z,Blocks.DARK_OAK_PLANKS);
        for(int i=0;i<10;i++){int x=-14+(i%5)*7,z=i<5?12:-14;for(int y=0;y<2;y++)j.add(c.getX()+x,100+y,c.getZ()+z,Blocks.CHISELED_DEEPSLATE);
            if(Yggdrasil.cleared(g,i)){j.add(c.getX()+x,102,c.getZ()+z,i==9?Blocks.DRAGON_HEAD:Blocks.SKELETON_SKULL);j.add(c.getX()+x,99,c.getZ()+z,Blocks.SEA_LANTERN);}}
        if(g.hallLevel>=1)for(int x=-4;x<=4;x++)j.add(c.getX()+x,100,c.getZ()+r-4,Blocks.BOOKSHELF);
        if(g.hallLevel>=2)for(int x:new int[]{-r+5,r-5})j.add(c.getX()+x,100,c.getZ(),Blocks.ENCHANTING_TABLE);
        if(g.hallLevel>=3)for(int z=-4;z<=4;z++)j.add(c.getX()+r-5,100,c.getZ()+z,Blocks.CHISELED_QUARTZ_BLOCK);
        return j;
    }
    public static void labels(ServerLevel level,GuildStore.Guild guild){
        var c=center(guild);String tag="aeloria_guild_"+guild.id;
        for(var entity:level.getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class,new net.minecraft.world.phys.AABB(c).inflate(40),e->e.entityTags().contains(tag)))entity.discard();
        text(level,c.offset(0,7,-16),guild.name+" · "+guild.bannerPattern,tag,.8f);
        for(int i=0;i<10;i++)text(level,c.offset(-14+(i%5)*7,4,i<5?12:-14),Yggdrasil.cleared(guild,i)?Yggdrasil.ALL[i].name():"Gardien "+(i+1)+" · À conquérir",tag,.35f);
    }
    private static void text(ServerLevel level,BlockPos pos,String label,String tag,float scale){
        String data="{text:{text:"+new com.google.gson.Gson().toJson(label)+",color:\"gold\"},billboard:\"center\",background:0,shadow:true,line_width:400,Tags:[\""+tag+"\"],transformation:{translation:[0f,0f,0f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],scale:["+scale+"f,"+scale+"f,"+scale+"f]}}";
        level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack().withLevel(level).withSuppressedOutput(),"summon minecraft:text_display "+(pos.getX()+.5)+" "+pos.getY()+" "+(pos.getZ()+.5)+" "+data);
    }
    @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post e){
        var level=e.getServer().getLevel(DIM);if(level==null)return;
        for(var id:new ArrayList<>(BUILDS.keySet())){var job=BUILDS.get(id);job.tick(level);if(job.done()){
            BUILDS.remove(id);var finished=GuildStore.access(e.getServer(),false,state->GuildStore.byId(state,id));if(finished!=null)labels(level,finished);for(var visitor:new HashMap<>(VISITORS).entrySet())if(visitor.getValue().equals(id)){
                var p=e.getServer().getPlayerList().getPlayer(visitor.getKey());VISITORS.remove(visitor.getKey());if(p==null)continue;
                var g=GuildStore.access(e.getServer(),false,s->GuildStore.of(s,p.getUUID()));if(g==null||!g.id.equals(id))continue;var c=center(g);
                p.teleportTo(level,c.getX()+.5,100,c.getZ()+5.5,Set.of(),180,0,true);p.sendSystemMessage(Component.literal("Hall de "+g.name+" · /guilde pour les améliorations · /reseau spawn pour sortir"));
            }
        }}
        if(e.getServer().getTickCount()%40==0)for(var p:new ArrayList<>(level.players())){
            if(Boolean.getBoolean("mmorpg.expansionVisual"))continue;
            var g=GuildStore.access(e.getServer(),false,s->GuildStore.of(s,p.getUUID()));if(g==null||g.hallIndex<0||Math.abs(p.getX()-center(g).getX())>34||Math.abs(p.getZ()-center(g).getZ())>34||p.getY()<95)com.mmorpg.competition.CompetitionTravel.home(p);
        }
    }
    @SubscribeEvent public static void damage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent e){if(e.getEntity().level().dimension().equals(DIM))e.setCanceled(true);}
    @SubscribeEvent public static void breaking(net.neoforged.neoforge.event.level.block.BreakBlockEvent e){if(e.getPlayer().level().dimension().equals(DIM)&&!e.getPlayer().isCreative())e.setCanceled(true);}
    @SubscribeEvent public static void placing(net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent e){if(e.getLevel() instanceof ServerLevel l&&l.dimension().equals(DIM))e.setCanceled(true);}
    @SubscribeEvent public static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent e){VISITORS.clear();BUILDS.clear();}
}
