package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.block.structure.*;
import com.mmorpg.hub.HubManager;
import com.mmorpg.network.*;
import com.mmorpg.registry.ModItems;
import com.mojang.brigadier.arguments.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import java.util.*;

/** Server-authoritative, non-destructive placement catalogue for operators. */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class SpawnEditor {
    public record Entry(String id, String label, int tab) { }
    public static List<Entry> catalogue() {
        List<Entry> all = new ArrayList<>();
        for (var k : MultiblockKind.values()) all.add(new Entry(k.key(), switch(k) {
            case FORGE -> "Forge arcanique"; case BANQUE -> "Banque"; case HDV -> "Hôtel des ventes";
            case PRESSE -> "Presse de compression"; case TELEPORTEUR -> "Téléporteur"; case AUTEL -> "Autel";
        }, 0));
        for (var t : com.mmorpg.block.crate.CrateTier.values()) all.add(new Entry(t.blockId(), t.label, 0));
        all.add(new Entry("portail:hub", "Vers le hub", 1)); all.add(new Entry("portail:spawn", "Vers le spawn", 1));
        all.add(new Entry("portail:mine", "Mines : menu", 1)); all.add(new Entry("portail:plaines", "Plaines : menu", 1));
        for (int i=0;i<5;i++) all.add(new Entry("portail:mine:"+i, "Mine · "+(i+1), 1));
        for (int i=0;i<6;i++) all.add(new Entry("portail:plaines:"+i, "Plaine · "+(i+1), 1));
        for (var d : com.mmorpg.world.DungeonBuilder.DungeonType.values())
            for (int i=0;i<3;i++) all.add(new Entry("portail:donjon:"+d.id+":"+i, d.label+" · "+new String[]{"normal","héroïque","mythique"}[i], 1));
        for (String role : new String[]{"MARCHAND","QUETES","MINES","DONJON"}) all.add(new Entry("pnj:"+role, "PNJ · "+role.toLowerCase(Locale.ROOT), 2));
        all.add(new Entry("maitre", "Maître des classes", 2));
        return List.copyOf(all);
    }
    private record Target(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim, BlockPos pos) { }
    private interface Undo { boolean run(ServerPlayer player); }
    private record Edit(long order, Undo undo) { }
    private static final Map<UUID, Target> TARGETS = new HashMap<>();
    private static final Map<UUID, Deque<Edit>> HISTORY = new HashMap<>();
    private static long sequence;
    public static long nextOrder() { return ++sequence; }
    public static long lastOrder(ServerPlayer p) { var h=HISTORY.get(p.getUUID()); return h==null||h.isEmpty()?0:h.peek().order(); }
    public static boolean undoOwn(ServerPlayer p) {
        var h=HISTORY.get(p.getUUID()); if(h==null||h.isEmpty()) return fail(p,"Aucun aménagement à annuler dans cette session.");
        if(!h.peek().undo().run(p)) return fail(p,"Annulation refusée : l'élément a changé depuis ou sa zone n'est pas chargée.");
        h.pop(); return true;
    }
    private static void remember(ServerPlayer p, Undo undo) {
        var h=HISTORY.computeIfAbsent(p.getUUID(), x->new ArrayDeque<>()); h.push(new Edit(nextOrder(),undo));
        while(h.size()>20) h.removeLast();
    }
    private static boolean fail(ServerPlayer p,String message) { p.sendSystemMessage(Component.literal(message).withColor(0xFF8080)); return false; }
    public static void open(ServerPlayer p, BlockPos clicked) {
        if(!HubManager.isOperator(p)) { fail(p,"Outil réservé aux opérateurs."); return; }
        if(clicked==null) {
            HitResult hit=p.pick(8,0,false);
            clicked=hit instanceof BlockHitResult b && hit.getType()==HitResult.Type.BLOCK ? b.getBlockPos().relative(b.getDirection()) : p.blockPosition().relative(p.getDirection(),4);
        }
        TARGETS.put(p.getUUID(),new Target(p.level().dimension(),clicked.immutable()));
        CompoundTag data=new CompoundTag();data.putString("position",clicked.toShortString());
        Net.toPlayer(p,new S2COpenScreen(S2COpenScreen.SPAWN_EDITOR,data));
    }
    private static BlockPos target(ServerPlayer p) {
        Target t=TARGETS.get(p.getUUID());
        if(t==null||!t.dim().equals(p.level().dimension())||t.pos().distSqr(p.blockPosition())>256) return null;
        return t.pos();
    }
    public static boolean placeOriented(ServerPlayer p,String id,String orientation) {
        Direction cardinal=Direction.byName(orientation);
        if(cardinal!=null&&cardinal.getAxis()!=Direction.Axis.Y)return place(p,id,cardinal);
        if(id.startsWith("portail:"))return fail(p,"Les portails gardent une orientation cardinale.");
        BlockPos at=target(p);if(at==null)return fail(p,"Rouvre l’outil près du point de placement.");
        float yaw;
        try {yaw=orientation.equals("moi")?FreeOrientation.towards(Vec3.atCenterOf(at),p.position()):Float.parseFloat(orientation);}
        catch(NumberFormatException e){return fail(p,"Orientation inconnue.");}
        if(!Float.isFinite(yaw))return false;
        return place(p,id,Direction.NORTH,net.minecraft.util.Mth.wrapDegrees(yaw));
    }
    public static boolean place(ServerPlayer p,String id,Direction facing) {return place(p,id,facing,Float.NaN);}
    private static boolean place(ServerPlayer p,String id,Direction facing,float yaw) {
        if(!HubManager.isOperator(p)) return false;
        BlockPos at=target(p); if(at==null) return fail(p,"Rouvre l'outil près du point de placement.");
        if(catalogue().stream().noneMatch(e->e.id().equals(id))) return fail(p,"Élément inconnu.");
        ServerLevel level=p.level();
        if(id.startsWith("portail:")) {
            String[] a=id.split(":"); int region=a.length>2&&!a[1].equals("donjon")?Integer.parseInt(a[2]):-1;
            return AdventurePortals.place(p,new com.mmorpg.world.AdventurePortalData.Portal(UUID.randomUUID(),level.dimension(),at,
                    facing.getAxis()==Direction.Axis.Z,a[1],region,a[1].equals("donjon")?a[2]:"",a[1].equals("donjon")?Integer.parseInt(a[3]):0));
        }
        if(id.startsWith("pnj:")||id.equals("maitre")) {
            if(!level.getBlockState(at).isAir()||!level.getBlockState(at.above()).isAir()) return fail(p,"Il faut deux blocs d'air.");
            Entity npc;
            if(id.equals("maitre")) {
                var master=com.mmorpg.registry.ModEntities.CLASS_MASTER.get().create(level,EntitySpawnReason.COMMAND);
                if(master==null) return false; npc=master; npc.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,(Float.isFinite(yaw)?yaw:facing.toYRot()),0);level.addFreshEntity(npc);
            } else {
                var role=com.mmorpg.entity.NpcEntity.Role.valueOf(id.substring(4));
                npc=com.mmorpg.world.DungeonBuilder.spawnNpc(level,at,role,"PNJ "+id.substring(4).toLowerCase(Locale.ROOT),0,Float.isFinite(yaw)?yaw:facing.toYRot());
            }
            if(npc==null) return false; npc.addTag("aeloria_editor"); rememberEntity(p,level,npc); return true;
        }
        Block block=BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("mmorpg",id));
        if(block==null||block==Blocks.AIR) return false;
        var kind=MultiblockKind.of(block); List<BlockPos> cells=new ArrayList<>();cells.add(at);
        if(Float.isFinite(yaw)) {
            for(BlockPos cell:FreeOrientation.shapes(block,yaw).keySet())if(!cell.equals(BlockPos.ZERO)) {
                if(Math.abs(cell.getX())>5||Math.abs(cell.getZ())>5||cell.getY()<0||cell.getY()>MultiblockKind.RANGE_Y)return fail(p,"Emprise hors limites.");
                cells.add(at.offset(cell));
            }
        } else if(kind!=null) for(int[] c:kind.cells()) cells.add(kind.cellPos(at,c,facing));
        Map<BlockPos,BlockState> before=new LinkedHashMap<>();
        for(BlockPos pos:cells) {
            if(level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(pos)||!level.hasChunkAt(pos)||!level.getBlockState(pos).isAir())
                return fail(p,"Emplacement occupé ou hors limites : aucun bloc remplacé.");
            before.put(pos,level.getBlockState(pos));
        }
        if(level.getBlockState(at.below()).getCollisionShape(level,at.below()).isEmpty()) return fail(p,"Pose la structure sur un sol solide.");
        BlockState state=block.defaultBlockState();
        if(state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) state=state.setValue(BlockStateProperties.HORIZONTAL_FACING,facing);
        if(state.hasProperty(FreeOrientation.FREE))state=state.setValue(FreeOrientation.FREE,Float.isFinite(yaw));
        level.setBlockAndUpdate(at,state);
        if(Float.isFinite(yaw)&&level.getBlockEntity(at) instanceof OrientedBlockEntity be)be.setYaw(yaw);
        if(kind!=null) Multiblocks.placeParts(level,at,kind,facing);
        Map<BlockPos,BlockState> after=new LinkedHashMap<>();for(BlockPos pos:cells) after.put(pos,level.getBlockState(pos));
        remember(p, player->{
            if(Float.isFinite(yaw)&&(!(level.getBlockEntity(at) instanceof OrientedBlockEntity be)||Float.compare(be.yaw,net.minecraft.util.Mth.wrapDegrees(yaw))!=0))return false;
            for(var e:after.entrySet()) if(!level.hasChunkAt(e.getKey())||!level.getBlockState(e.getKey()).equals(e.getValue())) return false;
            for(BlockPos pos:cells) if(level.getBlockEntity(pos) instanceof net.minecraft.world.Container c&&!c.isEmpty()) return false;
            // Restore peripheral cells before the master, whose removal also clears its parts.
            for(int i=cells.size()-1;i>=0;i--) level.setBlockAndUpdate(cells.get(i),before.get(cells.get(i)));
            return true;
        });
        p.sendSystemMessage(Component.literal("Structure posée. /undo pour annuler."));return true;
    }
    private static void rememberEntity(ServerPlayer p,ServerLevel level,Entity entity) {
        UUID id=entity.getUUID(); Vec3 position=entity.position();
        remember(p, player->{ Entity current=level.getEntity(id); if(current==null||!current.position().equals(position))return false;current.discard();return true; });
    }
    private static Display.TextDisplay nearestText(ServerPlayer p) {
        return p.level().getEntitiesOfClass(Display.TextDisplay.class,p.getBoundingBox().inflate(10),e->e.entityTags().contains("aeloria_editor"))
                .stream().min(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
    }
    public static boolean text(ServerPlayer p,String text,float scale,float height) {return textOriented(p,text,scale,height,"mobile");}
    public static boolean textOriented(ServerPlayer p,String text,float scale,float height,String orientation) {
        BlockPos at=target(p);if(at==null||text.isBlank()||text.length()>240)return fail(p,"Texte vide, trop long ou point de placement périmé.");
        UUID id=UUID.randomUUID();
        String tag="{text:{text:"+new com.google.gson.Gson().toJson(text)+",color:\"gold\"},billboard:\"center\",background:0,shadow:true,line_width:600,Tags:[\"aeloria_editor\"],UUID:[I;"
                +(int)(id.getMostSignificantBits()>>32)+","+(int)id.getMostSignificantBits()+","+(int)(id.getLeastSignificantBits()>>32)+","+(int)id.getLeastSignificantBits()+"],transformation:{translation:[0f,0f,0f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],scale:["+scale+"f,"+scale+"f,"+scale+"f]}}";
        if(!orientation.equals("mobile")) {
            float yaw;
            try {yaw=orientation.equals("moi")?FreeOrientation.towards(Vec3.atCenterOf(at),p.position()):Float.parseFloat(orientation);}catch(NumberFormatException e){return false;}
            if(!Float.isFinite(yaw))return false;
            tag=tag.replace("billboard:\"center\"","billboard:\"fixed\"");
            tag=tag.substring(0,tag.length()-1)+",Rotation:["+net.minecraft.util.Mth.wrapDegrees(yaw)+"f,0f]}";
        }
        ServerLevel level=p.level();
        level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack().withLevel(level).withSuppressedOutput(),
                "summon minecraft:text_display "+(at.getX()+.5)+" "+(at.getY()+height)+" "+(at.getZ()+.5)+" "+tag);
        Entity entity=level.getEntity(id);if(entity==null)return fail(p,"Impossible de créer le texte.");rememberEntity(p,level,entity);return true;
    }
    private static boolean removeText(ServerPlayer p) {
        var entity=nearestText(p);if(entity==null)return fail(p,"Aucun texte de l'éditeur à moins de 10 blocs.");
        var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,p.registryAccess());
        entity.saveWithoutId(out);String tag=out.buildResult().toString();Vec3 at=entity.position();UUID id=entity.getUUID();ServerLevel level=p.level();
        entity.discard();remember(p,player->{if(level.getEntity(id)!=null)return false;
            level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack().withLevel(level).withSuppressedOutput(),"summon minecraft:text_display "+at.x+" "+at.y+" "+at.z+" "+tag);
            return level.getEntity(id)!=null;});return true;
    }
    public static int setArrival(ServerPlayer p) {
        if(!HubManager.isOperator(p))return 0;
        if(!p.level().dimension().equals(com.mmorpg.hub.SpawnBuilder.DIM)) {fail(p,"Place-toi dans le monde du spawn avant de définir son arrivée.");return 0;}
        var data=com.mmorpg.hub.SpawnData.get(p.level().getServer());
        if(!data.exists()){fail(p,"Le monde du spawn n’est pas initialisé.");return 0;}
        data.setArrival(p.position(),p.getYRot(),p.getXRot());
        p.sendSystemMessage(Component.literal("Point du spawn enregistré ici, avec ton orientation."));return 1;
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        var root=Commands.literal("amenagement").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c->{open(c.getSource().getPlayerOrException(),null);return 1;});
        root.then(Commands.literal("outil").executes(c->{var p=c.getSource().getPlayerOrException();if(!p.getInventory().add(new ItemStack(ModItems.SPAWN_EDITOR.get())))return 0;return 1;}));
        root.then(Commands.literal("outilpnj").executes(c->{var p=c.getSource().getPlayerOrException();return p.getInventory().add(new ItemStack(ModItems.NPC_EDITOR.get()))?1:0;}));
        // Namespaced catalogue IDs contain ':'; Brigadier's word() stops at the colon.
        root.then(Commands.literal("poser").then(Commands.argument("placement",StringArgumentType.greedyString()).executes(c->{
            var p=c.getSource().getPlayerOrException();String[] parts=StringArgumentType.getString(c,"placement").trim().split("\\s+");
            if(parts.length!=2)return fail(p,"Usage : /amenagement poser élément orientation")?1:0;
            return placeOriented(p,parts[0],parts[1])?1:0;})));
        root.then(Commands.literal("texte").then(Commands.argument("taille",FloatArgumentType.floatArg(.25f,8)).then(Commands.argument("hauteur",FloatArgumentType.floatArg(0,12)).then(Commands.argument("texte",StringArgumentType.greedyString()).executes(c->text(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"texte"),FloatArgumentType.getFloat(c,"taille"),FloatArgumentType.getFloat(c,"hauteur"))?1:0)))));
        root.then(Commands.literal("texteoriente").then(Commands.argument("taille",FloatArgumentType.floatArg(.25f,8)).then(Commands.argument("hauteur",FloatArgumentType.floatArg(0,12)).then(Commands.argument("orientation",StringArgumentType.word()).then(Commands.argument("texte",StringArgumentType.greedyString()).executes(c->textOriented(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"texte"),FloatArgumentType.getFloat(c,"taille"),FloatArgumentType.getFloat(c,"hauteur"),StringArgumentType.getString(c,"orientation"))?1:0))))));
        root.then(Commands.literal("supprimertexte").executes(c->removeText(c.getSource().getPlayerOrException())?1:0));
        root.then(Commands.literal("annuler").executes(c->AdventurePortals.undo(c.getSource().getPlayerOrException())?1:0));
        root.then(Commands.literal("pointspawn").executes(c->setArrival(c.getSource().getPlayerOrException())));
        event.getDispatcher().register(root);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { TARGETS.clear();HISTORY.clear();sequence=0; }
}
