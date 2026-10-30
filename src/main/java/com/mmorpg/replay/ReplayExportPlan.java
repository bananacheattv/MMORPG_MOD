package com.mmorpg.replay;

/** Output seconds, including slow motion, are limited independently from source duration. */
public final class ReplayExportPlan {
    public static final int FPS=60, MAX_SECONDS=300;
    public static int frames(double duration,double cursor,double speed,int seconds){
        if(!Double.isFinite(duration)||!Double.isFinite(cursor)||!Double.isFinite(speed)||duration<0||cursor<0||speed<=0||seconds<1||seconds>MAX_SECONDS)
            throw new IllegalArgumentException("Durée ou vitesse d'export invalide");
        double length=Math.min(seconds,Math.max(0,duration-cursor)/speed);
        return (int)Math.ceil(length*FPS-1e-9);
    }
    public static long diskBudget(int width,int height,int frames,boolean mp4){
        if(width<=0||height<=0||frames<=0||frames>MAX_SECONDS*FPS)throw new IllegalArgumentException("Dimensions d'export invalides");
        long pixels=Math.multiplyExact(Math.multiplyExact((long)width,height),frames);
        return mp4?Math.addExact(pixels/12,64L*1024*1024):Math.multiplyExact(pixels,4);
    }
}
