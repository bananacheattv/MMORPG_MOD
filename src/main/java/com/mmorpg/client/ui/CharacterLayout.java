package com.mmorpg.client.ui;
import com.mmorpg.MMORPG;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.*;
/** Local layout preferences contain presentation only; they never alter server inventory slots or attributes. */
public final class CharacterLayout {
 public enum Module{EQUIPMENT("Équipement & sacs"),INVENTORY("Inventaire"),IDENTITY("Identité & jauges"),ATTRIBUTES("Attributs"),STATISTICS("Statistiques"),EVOLUTION("Évolution");public final String label;Module(String label){this.label=label;}}
 public static final class Box{public float x,y,scale=1;public boolean custom,visible=true;}
 public record Rect(float x,float y,float w,float h,float scale,boolean visible){public boolean contains(double mx,double my){return mx>=x&&my>=y&&mx<x+w&&my<y+h;}}
 private static final EnumMap<Module,Box> BOXES=new EnumMap<>(Module.class);
 private static boolean loaded;
 private static java.nio.file.Path path(){return Minecraft.getInstance().gameDirectory.toPath().resolve("config/aeloria-character-layout.json");}
 public static Box box(Module module){load();return BOXES.computeIfAbsent(module,k->new Box());}
 private static void load(){if(loaded)return;loaded=true;try{if(java.nio.file.Files.exists(path())){var type=new com.google.gson.reflect.TypeToken<Map<String,Box>>(){}.getType();Map<String,Box> data=new com.google.gson.Gson().fromJson(java.nio.file.Files.readString(path()),type);if(data!=null)for(var e:data.entrySet())try{if(e.getValue()!=null)BOXES.put(Module.valueOf(e.getKey()),e.getValue());}catch(IllegalArgumentException ignored){}}}catch(Exception e){MMORPG.LOGGER.warn("Disposition du personnage : {}",e.getMessage());}}
 public static void save(){try{java.nio.file.Files.createDirectories(path().getParent());var temp=java.nio.file.Files.createTempFile(path().getParent(),"character-layout", ".tmp");java.nio.file.Files.writeString(temp,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(BOXES));java.nio.file.Files.move(temp,path(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);}catch(Exception e){MMORPG.LOGGER.warn("Disposition non sauvegardée : {}",e.getMessage());}}
 public static String snapshot(){load();return new com.google.gson.Gson().toJson(BOXES);}
 public static void restore(String json){BOXES.clear();if(json!=null){Map<String,Box> d=new com.google.gson.Gson().fromJson(json,new com.google.gson.reflect.TypeToken<Map<String,Box>>(){}.getType());if(d!=null)for(var e:d.entrySet())BOXES.put(Module.valueOf(e.getKey()),e.getValue());}loaded=true;save();}
 public static void reload(){BOXES.clear();loaded=false;load();}
 public static void reset(){BOXES.clear();loaded=true;save();}
 public static Rect resolve(Module module,float dx,float dy,float dw,float dh,int width,int height){var box=box(module);float scale=Float.isFinite(box.scale)?Math.clamp(box.scale,.5f,2f):1;scale=Math.max(.5f,Math.min(scale,Math.min((width-6)/Math.max(1,dw),(height-80)/Math.max(1,dh))));float x=box.custom?box.x*width:dx,y=box.custom?box.y*height:dy;if(!Float.isFinite(x))x=dx;if(!Float.isFinite(y))y=dy;float w=dw*scale,h=dh*scale;return new Rect(Math.clamp(x,3,Math.max(3,width-w-3)),Math.clamp(y,54,Math.max(54,height-h-26)),w,h,scale,box.visible);}
 public static void push(GuiGraphicsExtractor g,Rect r,float dx,float dy){g.pose().pushMatrix();g.pose().translate(r.x,r.y);g.pose().scale(r.scale,r.scale);g.pose().translate(-dx,-dy);if(!r.visible)g.enableScissor(0,0,0,0);}
 public static void pop(GuiGraphicsExtractor g,Rect r){if(!r.visible)g.disableScissor();g.pose().popMatrix();}
 public static void move(Module module,float x,float y,int width,int height){var b=box(module);b.custom=true;b.x=x/Math.max(1,width);b.y=y/Math.max(1,height);}
 public static void scale(Module module,float value){box(module).scale=Math.clamp(value,.5f,2f);}
}
