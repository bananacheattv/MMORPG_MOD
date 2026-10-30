package com.mmorpg.client.replay;

import com.google.common.reflect.TypeToken;
import com.mmorpg.MMORPG;
import com.mmorpg.client.ClientNet;
import com.mmorpg.replay.*;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Local-only visual actors; no historical entity is ever spawned on the server. */
@EventBusSubscriber(modid=MMORPG.MODID,value=Dist.CLIENT)
public final class ReplayClient {
    private static Minecraft mc(){return Minecraft.getInstance();}
    private static final ContextKey<Boolean> HIDE=new ContextKey<>(MMORPG.id("replay_hide"));
    private static final ExecutorService IO=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"Aeloria replay IO");t.setDaemon(true);return t;});
    private static long lease;
    public static ReplayTake take;
    public static boolean recording,playing,paused=true,pathCamera,clean;
    public static double cursor,speed=1;
    public static int framing; // 0 landscape, 1 vertical guide, 2 square guide
    public static String message="/replay pour ouvrir l'atelier";
    private static boolean busy;
    private static long lastFrame;
    private static String levelId="";
    private static final Map<UUID,Integer> tracks=new LinkedHashMap<>();
    private static final Map<Integer,Map<Integer,SynchedEntityData.DataValue<?>>> metadata=new HashMap<>();
    private static final Map<Integer,Integer> lastState=new HashMap<>();
    private static final Map<Integer,LivingEntity> ghosts=new HashMap<>();
    private static final Map<Integer,ReplayTake.Pose> ghostPoses=new HashMap<>();
    private static final Set<Integer> failed=new HashSet<>();
    private static ArmorStand camera;
    private static int nextGhost=-1_000_000;
    private static Path file;

    @SubscribeEvent public static void register(RegisterClientPayloadHandlersEvent e){e.register(ReplayWire.Reply.TYPE,(p,c)->receive(p.action()));}
    public static boolean authorized(){return mc().player!=null&&mc().level!=null&&System.nanoTime()<lease;}
    static int ghostCount(){return ghosts.size();}
    public static void request(String op){if(mc().getConnection()!=null)ClientNet.send(new ReplayWire.Request(op));}
    private static void receive(String op){
        if(op.equals("stop")||op.equals("revoked")){stop(false);lease=0;return;}
        lease=System.nanoTime()+5_000_000_000L;
        switch(op){
            case "open" -> mc().gui.setScreen(new ReplayScreen());
            case "record" -> startRecord();
            case "play" -> startPlay();
        }
    }
    private static String dimension(){return mc().level.dimension().identifier().toString();}
    private static String server(){return mc().getCurrentServer()==null?"singleplayer:"+mc().level.dimension().identifier():mc().getCurrentServer().ip;}
    private static String registry(){
        // Entity metadata/item codecs use negotiated registry IDs. Refuse files from a different mapping.
        StringBuilder b=new StringBuilder("26.3/");
        for(var key:BuiltInRegistries.ENTITY_TYPE.keySet())b.append(key).append(';');
        for(var key:BuiltInRegistries.ITEM.keySet())b.append(key).append(';');
        return Integer.toHexString(b.toString().hashCode());
    }
    private static Path folder(){return mc().gameDirectory.toPath().resolve("aeloria-replays");}
    public static List<Path> files(){
        try{if(!Files.isDirectory(folder()))return List.of();try(var stream=Files.list(folder())){return stream.filter(p->p.getFileName().toString().endsWith(".aereplay")).sorted(Comparator.reverseOrder()).limit(100).toList();}}
        catch(IOException e){message=e.getMessage();return List.of();}
    }
    public static void load(Path path){
        if(!authorized()||playing||recording||busy)return;
        busy=true;message="Chargement de la prise…";
        IO.execute(()->{
            try{ReplayTake loaded=ReplayTake.read(path);mc().execute(()->{if(authorized()){take=loaded;file=path;cursor=0;message="Prise chargée : "+loaded.name;}busy=false;});}
            catch(Exception e){mc().execute(()->{message="Lecture impossible : "+e.getMessage();busy=false;});}
        });
    }
    private static void startRecord(){
        if(!authorized()||busy)return;
        if(playing||recording){message="Arrêtez la prise en cours avant d'en créer une autre.";return;}
        take=new ReplayTake();take.name="prise-"+java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        take.server=server();take.dimension=dimension();take.registry=registry();
        tracks.clear();metadata.clear();lastState.clear();failed.clear();file=folder().resolve(take.name+"-"+UUID.randomUUID().toString().substring(0,8)+".aereplay");
        levelId=dimension();cursor=0;recording=true;message="REC — acteurs visibles, rayon 96 blocs, maximum 3 minutes";
        mc().gui.setScreen(null);
    }
    private static byte[] tagBytes(CompoundTag tag)throws IOException{
        var bytes=new ByteArrayOutputStream();try(var out=new DataOutputStream(bytes)){NbtIo.write(tag,out);}return bytes.toByteArray();
    }
    private static CompoundTag tag(byte[] bytes)throws IOException{
        return NbtIo.read(new DataInputStream(new ByteArrayInputStream(bytes)),NbtAccounter.create(262144));
    }
    private static CompoundTag initial(LivingEntity e){
        if(e instanceof Player)return new CompoundTag();
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,mc().level.registryAccess());e.saveWithoutId(out);
        CompoundTag t=out.buildResult();
        // Retain presentation/custom-model fields; discard brains, cargo, persistent user data and identity.
        for(String key:List.of("UUID","Passengers","Brain","Inventory","EnderItems","Items","NeoForgeData","ForgeData","Pos","Motion","Rotation"))t.remove(key);
        return t;
    }
    private static <T> SynchedEntityData.DataValue<T> current(LivingEntity e,SynchedEntityData.DataValue<T> v){
        return new SynchedEntityData.DataValue<>(v.id(),v.serializer(),v.serializer().copy(e.getEntityData().get(new EntityDataAccessor<>(v.id(),v.serializer()))));
    }
    private static byte[] synced(LivingEntity entity,int actor){
        Map<Integer,SynchedEntityData.DataValue<?>> known=metadata.computeIfAbsent(actor,k->new TreeMap<>());
        var values=entity.getEntityData().getNonDefaultValues();if(values!=null)for(var v:values)known.put(v.id(),v);
        var buf=new RegistryFriendlyByteBuf(Unpooled.buffer(),mc().level.registryAccess());
        try{for(var v:known.values())current(entity,v).write(buf);buf.writeByte(255);byte[] b=new byte[buf.readableBytes()];buf.readBytes(b);return b;}finally{buf.release();}
    }
    private static byte[] gear(LivingEntity entity)throws IOException{
        CompoundTag result=new CompoundTag();var ops=mc().level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        for(var slot:EquipmentSlot.values())result.put(slot.name(),ItemStack.OPTIONAL_CODEC.encodeStart(ops,entity.getItemBySlot(slot)).getOrThrow());
        return tagBytes(result);
    }
    private static int state(LivingEntity e,int actor)throws IOException{
        byte[] data=synced(e,actor),gear=gear(e);
        Integer old=lastState.get(actor);
        if(old!=null){var s=take.states.get(old);if(Arrays.equals(s.synced(),data)&&Arrays.equals(s.gear(),gear))return old;}
        if(take.states.size()>=ReplayTake.MAX_STATES||take.bytes+data.length+gear.length>ReplayTake.MAX_BYTES)throw new IOException("Limite de mémoire de la prise atteinte");
        int id=take.states.size();take.states.add(new ReplayTake.State(data,gear));take.bytes+=data.length+gear.length;lastState.put(actor,id);return id;
    }
    private static void capture(){
        if(take.frames.size()>=ReplayTake.MAX_FRAMES){stop(true);return;}
        List<ReplayTake.Pose> frame=new ArrayList<>();
        for(var entity:mc().level.entitiesForRendering()){
            if(!(entity instanceof LivingEntity e)||ghosts.containsValue(e)||e.distanceToSqr(mc().player)>96*96)continue;
            if(failed.contains(e.getId()))continue;
            try{
                Integer actor=tracks.get(e.getUUID());
                if(actor==null){
                    if(tracks.size()>=ReplayTake.MAX_ACTORS)continue;
                    byte[] initial=tagBytes(initial(e));if(initial.length>65536)continue;
                    actor=take.actors.size();tracks.put(e.getUUID(),actor);
                    take.actors.add(new ReplayTake.Actor(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString(),e.getUUID(),e instanceof Player p?p.getGameProfile().name():e.getName().getString(),initial));
                }
                int state=state(e,actor);
                frame.add(new ReplayTake.Pose(actor,state,e.getX(),e.getY(),e.getZ(),e.getYRot(),e.getXRot(),e.getYHeadRot(),e.yBodyRot,
                        e.walkAnimation.position(),e.walkAnimation.speed(),e.getSwingAnimation(1),e.hurtTime,e.deathTime,e.tickCount));
            }catch(Exception ex){
                if(ex instanceof IOException){message=ex.getMessage();stop(true);return;}
                failed.add(e.getId());MMORPG.LOGGER.warn("Replay : acteur {} ignoré",e.getType(),ex);
            }
        }
        take.frames.add(frame);cursor=take.duration();
    }
    public static void save(){
        if(take==null||file==null||recording||busy)return;
        busy=true;ReplayTake snapshot=take;
        IO.execute(()->{try{snapshot.write(file);mc().execute(()->{message="Sauvegardé : "+snapshot.name;busy=false;});}catch(Exception e){mc().execute(()->{message="Sauvegarde impossible : "+e.getMessage();busy=false;});}});
    }
    private static void startPlay(){
        if(!authorized())return;
        if(busy||recording||take==null||take.frames.isEmpty()||!take.dimension.equals(dimension())||!take.server.equals(server())||!take.registry.equals(registry())){
            message="Chargez une prise compatible, sur le serveur et dans la dimension d'origine.";request("stop");return;
        }
        var first=take.frames.stream().filter(f->!f.isEmpty()).findFirst();
        if(first.isPresent()&&first.get().stream().allMatch(p->mc().player.distanceToSqr(p.x(),p.y(),p.z())>128*128)){
            message="Retournez près du lieu du tournage avant de lancer la lecture.";request("stop");return;
        }
        clearGhosts();playing=true;paused=true;pathCamera=false;levelId=dimension();cursor=0;lastFrame=System.nanoTime();
        message="Lecture prête — caméra spectateur, /replay pour les contrôles";
        mc().gui.setScreen(new ReplayScreen());
    }
    private static LivingEntity spawn(int index)throws Exception{
        var a=take.actors.get(index);LivingEntity e;
        if(a.type().equals("minecraft:player")){
            var info=mc().getConnection().getPlayerInfo(a.uuid());var profile=info==null?new GameProfile(a.uuid(),a.name()):info.getProfile();
            e=new RemotePlayer(mc().level,profile){@Override protected net.minecraft.client.multiplayer.PlayerInfo getPlayerInfo(){return info;}};
        }else{
            var type=BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(a.type()));
            Entity created=type.create(mc().level,EntitySpawnReason.LOAD);if(!(created instanceof LivingEntity l))throw new IOException("Type d'acteur incompatible");e=l;
            e.load(TagValueInput.create(ProblemReporter.DISCARDING,mc().level.registryAccess(),tag(a.initial())));
        }
        while(mc().level.getEntity(nextGhost)!=null)nextGhost--;
        e.setId(nextGhost--);e.setUUID(UUID.randomUUID());e.noPhysics=true;e.setNoGravity(true);if(e instanceof Mob m)m.setNoAi(true);
        mc().level.addEntity(e);ghosts.put(index,e);return e;
    }
    private static void applyState(LivingEntity e,int index)throws IOException{
        var s=take.states.get(index);var buf=new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(s.synced()),mc().level.registryAccess());
        try{List<SynchedEntityData.DataValue<?>> values=new ArrayList<>();int id;while((id=buf.readUnsignedByte())!=255)values.add(SynchedEntityData.DataValue.read(buf,id));e.getEntityData().assignValues(values);}finally{buf.release();}
        CompoundTag gear=tag(s.gear());var ops=mc().level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        for(var slot:EquipmentSlot.values()){var v=gear.get(slot.name());e.setItemSlot(slot,v==null?ItemStack.EMPTY:ItemStack.OPTIONAL_CODEC.parse(ops,v).getOrThrow());}
    }
    private static void actors(){
        int frame=Math.min(take.frames.size()-1,(int)(cursor*20));double alpha=cursor*20-frame;
        Map<Integer,ReplayTake.Pose> next=new HashMap<>();if(frame+1<take.frames.size())for(var p:take.frames.get(frame+1))next.put(p.actor(),p);
        Set<Integer> visible=new HashSet<>();ghostPoses.clear();
        for(var p:take.frames.get(frame)){
            visible.add(p.actor());if(failed.contains(p.actor()))continue;
            try{
                LivingEntity e=ghosts.get(p.actor());if(e==null)e=spawn(p.actor());
                if(!Objects.equals(lastState.get(p.actor()),p.state())){applyState(e,p.state());lastState.put(p.actor(),p.state());}
                var q=next.getOrDefault(p.actor(),p);double t=e.position().distanceToSqr(q.x(),q.y(),q.z())>64*64?0:alpha;
                double x=p.x()+(q.x()-p.x())*t,y=p.y()+(q.y()-p.y())*t,z=p.z()+(q.z()-p.z())*t;
                e.setPos(x,y,z);e.xo=e.xOld=x;e.yo=e.yOld=y;e.zo=e.zOld=z;
                e.setYRot(ReplayTake.angle(p.yaw(),q.yaw(),t));e.yRotO=e.getYRot();e.setXRot((float)(p.pitch()+(q.pitch()-p.pitch())*t));e.xRotO=e.getXRot();
                e.setYHeadRot(ReplayTake.angle(p.head(),q.head(),t));e.yHeadRotO=e.getYHeadRot();e.yBodyRot=ReplayTake.angle(p.body(),q.body(),t);e.yBodyRotO=e.yBodyRot;
                e.tickCount=p.age();e.hurtTime=p.hurt();e.deathTime=p.death();
                ghostPoses.put(e.getId(),p);
            }catch(Exception ex){failed.add(p.actor());MMORPG.LOGGER.warn("Replay : relecture de l'acteur {} impossible",p.actor(),ex);message="Certains acteurs ne sont pas compatibles ; voir le journal.";}
        }
        for(var entry:new ArrayList<>(ghosts.entrySet()))if(!visible.contains(entry.getKey())){mc().level.removeEntity(entry.getValue().getId(),Entity.RemovalReason.DISCARDED);ghosts.remove(entry.getKey());lastState.remove(entry.getKey());}
    }
    public static void seek(double time){cursor=Math.max(0,Math.min(take==null?0:take.duration(),time));clearGhosts();lastFrame=System.nanoTime();}
    public static void keyframe(){
        if(!playing||busy||take.cameras.size()>=200)return;
        var c=mc().getCameraEntity();if(c==null)return;
        take.cameras.removeIf(k->Math.abs(k.seconds()-cursor)<.025);
        take.cameras.add(new ReplayTake.Camera(cursor,c.getX(),c.getEyeY(),c.getZ(),c.getYRot(),c.getXRot(),mc().options.fov().get()));
        take.cameras.sort(Comparator.comparingDouble(ReplayTake.Camera::seconds));message=take.cameras.size()+" points de caméra";
    }
    private static void path(){
        if(!pathCamera||(take.cameras.isEmpty()&&!ReplayExporter.active)){if(camera!=null){mc().setCameraEntity(mc().player);camera=null;}return;}
        ReplayTake.Camera c;
        if(take.cameras.isEmpty())c=ReplayExporter.fixedCamera;
        else{
            var keys=take.cameras;var a=keys.getFirst();var b=a;
            for(var k:keys){if(k.seconds()<=cursor)a=k;if(k.seconds()>=cursor){b=k;break;}b=k;}
            c=ReplayTake.interpolate(a,b,cursor);
        }
        if(camera==null){camera=new ArmorStand(mc().level,0,0,0);camera.setInvisible(true);mc().setCameraEntity(camera);}
        double y=c.y()-camera.getEyeHeight();camera.setPos(c.x(),y,c.z());camera.xo=camera.xOld=c.x();camera.yo=camera.yOld=y;camera.zo=camera.zOld=c.z();camera.setYRot(c.yaw());camera.yRotO=c.yaw();camera.setXRot(c.pitch());camera.xRotO=c.pitch();
    }
    private static void clearGhosts(){
        if(mc().level!=null)for(var e:ghosts.values())if(e.level()==mc().level)mc().level.removeEntity(e.getId(),Entity.RemovalReason.DISCARDED);
        ghosts.clear();ghostPoses.clear();lastState.clear();failed.clear();
    }
    public static void stop(boolean notify){
        ReplayExporter.cancel();
        boolean wasRecording=recording;recording=false;
        if(playing){playing=false;mc().setCameraEntity(mc().player);}
        camera=null;pathCamera=false;clean=false;clearGhosts();
        if(mc().gui.screen() instanceof ReplayScreen)mc().gui.setScreen(null);
        if(wasRecording){message="Prise arrêtée";save();}
        if(notify)request("stop");
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        if((recording||playing)&&(!authorized()||!levelId.equals(dimension()))){stop(true);return;}
        if(recording)capture();
    }
    @SubscribeEvent public static void frame(RenderFrameEvent.Pre event){
        if(!playing||!authorized()||take==null)return;
        long now=System.nanoTime();double dt=Math.min(.25,Math.max(0,(now-lastFrame)/1e9));lastFrame=now;
        ReplayExporter.prepare();
        if(!paused){cursor=Math.min(take.duration(),cursor+dt*speed);if(cursor>=take.duration())paused=true;}
        actors();path();
        if(clean)mc().gui.toastManager().clear();
    }
    @SubscribeEvent public static void fov(ViewportEvent.ComputeFov e){
        if(playing&&pathCamera&&!take.cameras.isEmpty()){
            var a=take.cameras.getFirst();var b=a;for(var k:take.cameras){if(k.seconds()<=cursor)a=k;if(k.seconds()>=cursor){b=k;break;}b=k;}
            e.setFOV(ReplayTake.interpolate(a,b,cursor).fov());
        }
    }
    @SubscribeEvent public static void disconnected(ClientPlayerNetworkEvent.LoggingOut e){stop(false);lease=0;}
    @SubscribeEvent public static void freezeGhost(net.neoforged.neoforge.event.tick.EntityTickEvent.Pre e){
        if(playing&&e.getEntity().level().isClientSide()&&ghosts.containsValue(e.getEntity()))e.setCanceled(true);
    }
    @SubscribeEvent public static void input(InputEvent.InteractionKeyMappingTriggered e){if(playing){e.setCanceled(true);e.setSwingHand(false);}}
    @SubscribeEvent public static void modifiers(RegisterRenderStateModifiersEvent e){
        e.registerEntityModifier(new TypeToken<LivingEntityRenderer<LivingEntity,LivingEntityRenderState,?>>(){},(entity,state)->{
            var view=mc().getCameraEntity();
            boolean inside=view!=null&&entity.getBoundingBox().contains(view.getEyePosition());
            state.setRenderData(HIDE,playing&&(!ghostPoses.containsKey(entity.getId())||inside));
            var p=ghostPoses.get(entity.getId());if(playing&&p!=null){
                state.walkAnimationPos=p.walk();state.walkAnimationSpeed=p.walkSpeed();state.ageInTicks=p.age();
                if(state instanceof net.minecraft.client.renderer.entity.state.ArmedEntityRenderState armed){
                    armed.swingAnimation=p.attack();
                    armed.currentSwing=p.attack()>0?new LivingEntity.SwingDescription(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.component.SwingAnimation.DEFAULT,6):null;
                }
            }
        });
    }
    @SubscribeEvent public static void hideLive(RenderLivingEvent.Pre<?,?,?> e){if(Boolean.TRUE.equals(e.getRenderState().getRenderData(HIDE)))e.setCanceled(true);}
}
