package com.mmorpg.competition;

import java.util.*;

/** Server-only match state; neutral crystal generates no points and capture resets the two-second clock. */
public final class ArenaRules {
    public static final int WIN=500,KILL=10,CAPTURE_INTERVAL=40;
    public static final int CAPTURE_HITS=50,STRIKE_INTERVAL=0;
    public enum Kind {FEU(5,"Arène de feu"),DUNES(3,"Arène des dunes");public final int size;public final String label;Kind(int size,String label){this.size=size;this.label=label;}}
    public static int bracket(int level){if(level<1||level>100)throw new IllegalArgumentException("Niveau invalide");return (level-1)/20;}
    public static String range(int bracket){return (bracket*20+1)+"–"+(bracket*20+20);}
    public static LinkedHashMap<UUID,Integer> balance(Kind kind,int bracket,Map<UUID,Integer> players){
        if(players.size()!=kind.size*2||players.values().stream().anyMatch(level->bracket(level)!=bracket))throw new IllegalArgumentException("Effectif incomplet ou mauvaise tranche de niveau.");
        var ordered=players.entrySet().stream().sorted(Comparator.comparingInt((Map.Entry<UUID,Integer> e)->e.getValue()).reversed().thenComparing(Map.Entry::getKey)).toList();var teams=new LinkedHashMap<UUID,Integer>();int i=0;for(var e:ordered){teams.put(e.getKey(),(i/2)%2==0?i%2:1-i%2);i++;}return teams;
    }
    public static final class Match {
        public final UUID id=UUID.randomUUID();public final Kind kind;public final int bracket;public final LinkedHashMap<UUID,Integer> teams=new LinkedHashMap<>();
        public int red,blue,owner=-1,captureHits;
        public static final class Contribution{public int kills,deaths,strikes;public double damage,healing;}
        public final Map<UUID,Contribution> contributions=new LinkedHashMap<>();
        public Contribution contribution(UUID id){return contributions.computeIfAbsent(id,k->new Contribution());}public long captureAt,started,lastStrike=-STRIKE_INTERVAL;public boolean ended;
        public Match(Kind kind,int bracket){this.kind=kind;this.bracket=bracket;}
        public boolean enemy(UUID a,UUID b){return teams.containsKey(a)&&teams.containsKey(b)&&!teams.get(a).equals(teams.get(b));}
        public boolean capture(UUID player,long tick){
            if(ended||!teams.containsKey(player)||tick-lastStrike<STRIKE_INTERVAL)return false;
            int team=teams.get(player);if(owner==team)return false;
            lastStrike=tick;owner=-1;captureHits++;contribution(player).strikes++;
            if(captureHits==CAPTURE_HITS){owner=team;captureHits=0;captureAt=tick;}
            return true;
        }
        public void kill(UUID killer,UUID victim){if(!ended&&enemy(killer,victim)){contribution(killer).kills++;contribution(victim).deaths++;score(teams.get(killer),KILL);}}
        public void tick(long tick){if(!ended&&owner>=0&&tick>=captureAt+CAPTURE_INTERVAL){long n=(tick-captureAt)/CAPTURE_INTERVAL;score(owner,(int)Math.min(WIN,n));captureAt+=n*CAPTURE_INTERVAL;}}
        public void score(int team,int amount){if(team==0)red=Math.min(WIN,red+amount);else blue=Math.min(WIN,blue+amount);ended=red>=WIN||blue>=WIN;}
        public int winner(){return red==blue?-1:red>blue?0:1;}
    }
}
