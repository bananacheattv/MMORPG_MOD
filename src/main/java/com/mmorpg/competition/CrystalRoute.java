package com.mmorpg.competition;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Ground-level circuit. Obstacles are excluded by their entire column, never climbed. */
final class CrystalRoute {
    static final double SPEED = .035; // 0.7 blocks/second, independently of segment length.
    private record Cell(int x,int z) {}
    private record Node(Cell cell,int cost,int priority) {}
    private final ServerLevel level;
    private final BlockPos center;
    private final Map<Cell,Boolean> free=new HashMap<>();
    final List<Vec3> points=new ArrayList<>();
    private int next=1;
    private Vec3 position;
    CrystalRoute(ServerLevel level,BlockPos center) {
        this.level=level;this.center=center;
        Cell start=new Cell(18,0),from=start;
        points.add(point(start));
        for(Cell target:List.of(new Cell(12,12),new Cell(-12,12),new Cell(-32,0),new Cell(-12,-12),new Cell(12,-12),new Cell(32,0),start)) {
            var segment=search(from,target);
            if(segment.isEmpty())throw new IllegalStateException("Aucun passage libre pour le cristal d’arène : "+from+" vers "+target);
            int i=0;
            while(i<segment.size()-1){int last=i+1;while(last+1<segment.size()&&clearLine(point(segment.get(i)),point(segment.get(last+1))))last++;points.add(point(segment.get(last)));i=last;}
            from=target;
        }
        position=points.getFirst();
    }
    private Vec3 point(Cell cell){return new Vec3(center.getX()+cell.x+.5,101.15,center.getZ()+cell.z+.5);}
    private boolean open(Cell cell){return Math.abs(cell.x)<=32&&Math.abs(cell.z)<=30&&free.computeIfAbsent(cell,c->clear(point(c)));}
    private boolean clear(Vec3 p){return level.noCollision(null,new AABB(p.x-1.5,100,p.z-1.5,p.x+1.5,113,p.z+1.5));}
    private boolean clearLine(Vec3 a,Vec3 b){int n=(int)Math.ceil(a.distanceTo(b)*4);for(int i=0;i<=n;i++)if(!clear(a.lerp(b,i/(double)Math.max(1,n))))return false;return true;}
    private List<Cell> search(Cell start,Cell target){
        var queue=new PriorityQueue<Node>(Comparator.comparingInt(Node::priority));var cost=new HashMap<Cell,Integer>();var parents=new HashMap<Cell,Cell>();cost.put(start,0);queue.add(new Node(start,0,0));
        while(!queue.isEmpty()) {var node=queue.remove();var cell=node.cell;if(node.cost!=cost.get(cell))continue;if(cell.equals(target)){var out=new ArrayList<Cell>();for(Cell at=target;at!=null;at=parents.get(at))out.add(at);Collections.reverse(out);return out;}
            for(var step:List.of(new Cell(1,0),new Cell(-1,0),new Cell(0,1),new Cell(0,-1))){var other=new Cell(cell.x+step.x,cell.z+step.z);int value=node.cost+1;if(!open(other)||value>=cost.getOrDefault(other,Integer.MAX_VALUE))continue;cost.put(other,value);parents.put(other,cell);queue.add(new Node(other,value,value+Math.abs(other.x-target.x)+Math.abs(other.z-target.z)));}
        }return List.of();
    }
    Vec3 position(){return position;}
    Vec3 advance(long tick){
        double remaining=SPEED;
        while(remaining>0){var target=points.get(next);double distance=position.distanceTo(target);if(distance<1e-8){next=(next+1)%points.size();continue;}var candidate=position.add(target.subtract(position).scale(Math.min(remaining,distance)/distance));if(!clear(candidate))break;position=candidate;remaining-=Math.min(remaining,distance);if(position.distanceTo(target)<1e-8)next=(next+1)%points.size();}
        return position.add(0,Math.sin(tick*.025)*.08,0);
    }
}
