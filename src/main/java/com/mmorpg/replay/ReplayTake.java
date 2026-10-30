package com.mmorpg.replay;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Bounded, versioned local visual takes. Contains no server instructions and is never uploaded. */
public final class ReplayTake {
    public static final int MAX_FRAMES = 3600, MAX_ACTORS = 128, MAX_STATES = 16384;
    public static final long MAX_BYTES = 64L * 1024 * 1024;
    public record Actor(String type, UUID uuid, String name, byte[] initial) {}
    public record State(byte[] synced, byte[] gear) {}
    public record Pose(int actor, int state, double x, double y, double z, float yaw, float pitch,
                       float head, float body, float walk, float walkSpeed, float attack, int hurt, int death, int age) {}
    public record Camera(double seconds, double x, double y, double z, float yaw, float pitch, float fov) {}
    public final List<Actor> actors = new ArrayList<>();
    public final List<State> states = new ArrayList<>();
    public final List<List<Pose>> frames = new ArrayList<>();
    public final List<Camera> cameras = new ArrayList<>();
    public String server = "", dimension = "", name = "", registry = "";
    public long bytes;
    public double duration() { return Math.max(0, frames.size() - 1) / 20.0; }
    public static float angle(float a, float b, double t) {
        float d = (b - a) % 360;
        if (d >= 180) d -= 360;
        if (d < -180) d += 360;
        return (float)(a + d * t);
    }
    public static Camera interpolate(Camera a, Camera b, double seconds) {
        double t = Math.max(0, Math.min(1, (seconds - a.seconds()) / Math.max(.000001, b.seconds() - a.seconds())));
        t = t * t * (3 - 2 * t);
        return new Camera(seconds, a.x() + (b.x()-a.x())*t, a.y() + (b.y()-a.y())*t, a.z() + (b.z()-a.z())*t,
                angle(a.yaw(),b.yaw(),t), (float)(a.pitch()+(b.pitch()-a.pitch())*t), (float)(a.fov()+(b.fov()-a.fov())*t));
    }
    public void write(Path target) throws IOException {
        Path part = target.resolveSibling(target.getFileName()+".part");
        Files.createDirectories(target.getParent());
        try (var out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(part))))) {
            out.writeInt(0x41455250); out.writeInt(1);
            out.writeUTF(server); out.writeUTF(dimension); out.writeUTF(name); out.writeUTF(registry);
            out.writeInt(actors.size());
            for (Actor a : actors) { out.writeUTF(a.type()); out.writeLong(a.uuid().getMostSignificantBits()); out.writeLong(a.uuid().getLeastSignificantBits()); out.writeUTF(a.name()); blob(out,a.initial()); }
            out.writeInt(states.size()); for (State s : states) { blob(out,s.synced()); blob(out,s.gear()); }
            out.writeInt(frames.size());
            for (List<Pose> frame : frames) { out.writeInt(frame.size()); for (Pose p : frame) {
                out.writeInt(p.actor());out.writeInt(p.state());out.writeDouble(p.x());out.writeDouble(p.y());out.writeDouble(p.z());
                out.writeFloat(p.yaw());out.writeFloat(p.pitch());out.writeFloat(p.head());out.writeFloat(p.body());
                out.writeFloat(p.walk());out.writeFloat(p.walkSpeed());out.writeFloat(p.attack());
                out.writeInt(p.hurt());out.writeInt(p.death());out.writeInt(p.age());
            }}
            out.writeInt(cameras.size()); for (Camera c : cameras) {
                out.writeDouble(c.seconds());out.writeDouble(c.x());out.writeDouble(c.y());out.writeDouble(c.z());out.writeFloat(c.yaw());out.writeFloat(c.pitch());out.writeFloat(c.fov());
            }
        } catch (IOException e) { Files.deleteIfExists(part); throw e; }
        try { Files.move(part,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException e) { Files.move(part,target,StandardCopyOption.REPLACE_EXISTING); }
    }
    private static void blob(DataOutput out, byte[] b) throws IOException { out.writeInt(b.length); out.write(b); }
    private static int count(DataInput in, int max) throws IOException {
        int n=in.readInt(); if(n<0||n>max)throw new IOException("Replay : limite de taille dépassée");return n;
    }
    private static byte[] blob(DataInput in) throws IOException { byte[] b=new byte[count(in,262144)]; in.readFully(b);return b; }
    public static ReplayTake read(Path path) throws IOException {
        ReplayTake t=new ReplayTake();
        // Limit the decompressed stream as well as individual collections (zip bombs / malformed local files).
        try(var in=new DataInputStream(new BufferedInputStream(new Limited(new GZIPInputStream(Files.newInputStream(path)),128L*1024*1024)))) {
            if(in.readInt()!=0x41455250||in.readInt()!=1)throw new IOException("Format de replay incompatible");
            t.server=in.readUTF();t.dimension=in.readUTF();t.name=in.readUTF();t.registry=in.readUTF();
            int actors=count(in,MAX_ACTORS);
            for(int i=0;i<actors;i++)t.actors.add(new Actor(in.readUTF(),new UUID(in.readLong(),in.readLong()),in.readUTF(),blob(in)));
            int states=count(in,MAX_STATES);
            for(int i=0;i<states;i++){State s=new State(blob(in),blob(in));t.bytes+=s.synced().length+s.gear().length;if(t.bytes>MAX_BYTES)throw new IOException("Replay trop volumineux");t.states.add(s);}
            int frames=count(in,MAX_FRAMES);
            for(int f=0;f<frames;f++){
                int n=count(in,MAX_ACTORS);List<Pose> frame=new ArrayList<>(n);Set<Integer> ids=new HashSet<>();
                for(int j=0;j<n;j++){
                    int actor=in.readInt(),state=in.readInt();
                    if(actor<0||actor>=actors||state<0||state>=states||!ids.add(actor))throw new IOException("Référence d'acteur invalide");
                    Pose p=new Pose(actor,state,in.readDouble(),in.readDouble(),in.readDouble(),in.readFloat(),in.readFloat(),in.readFloat(),in.readFloat(),in.readFloat(),in.readFloat(),in.readFloat(),in.readInt(),in.readInt(),in.readInt());
                    if(!finite(p.x(),p.y(),p.z(),p.yaw(),p.pitch(),p.head(),p.body(),p.walk(),p.walkSpeed(),p.attack())||Math.abs(p.x())>30_000_000||Math.abs(p.z())>30_000_000||Math.abs(p.y())>20_000_000)throw new IOException("Coordonnées de replay invalides");
                    frame.add(p);
                }t.frames.add(frame);
            }
            int cameras=count(in,200);
            double prev=-1;
            for(int i=0;i<cameras;i++){
                Camera c=new Camera(in.readDouble(),in.readDouble(),in.readDouble(),in.readDouble(),in.readFloat(),in.readFloat(),in.readFloat());
                if(!finite(c.seconds(),c.x(),c.y(),c.z(),c.yaw(),c.pitch(),c.fov())||c.seconds()<prev||c.seconds()<0||c.seconds()>t.duration()||c.fov()<10||c.fov()>120)throw new IOException("Caméra invalide");
                prev=c.seconds();t.cameras.add(c);
            }
            if(in.read()!=-1)throw new IOException("Données inattendues en fin de replay");
        }return t;
    }
    private static boolean finite(double... v){for(double x:v)if(!Double.isFinite(x))return false;return true;}
    private static final class Limited extends FilterInputStream {
        private long remaining;
        Limited(InputStream in,long limit){super(in);remaining=limit;}
        @Override public int read() throws IOException {int b=super.read();if(b>=0&&--remaining<0)throw new IOException("Replay décompressé trop grand");return b;}
        @Override public int read(byte[] b,int off,int len)throws IOException {int n=super.read(b,off,(int)Math.min(len,Math.max(1,remaining+1)));if(n>0&&(remaining-=n)<0)throw new IOException("Replay décompressé trop grand");return n;}
    }
}
