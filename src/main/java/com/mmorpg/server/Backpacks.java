package com.mmorpg.server;
import com.mmorpg.item.BackpackItem;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.*;
import net.minecraft.resources.RegistryOps;
import java.util.*;
/** Two equipment slots and up to 48 real inventory cells, carried in the shared RPG profile. */
@net.neoforged.fml.common.EventBusSubscriber(modid=com.mmorpg.MMORPG.MODID)
public final class Backpacks {
 public static final Component TITLE=Component.translatable("container.mmorpg.backpacks");
 private static final Map<Player,State> STATES=new WeakHashMap<>();
 private static final class PersistentInventory extends SimpleContainer {Runnable changed;PersistentInventory(){super(54);}@Override public void setChanged(){super.setChanged();if(changed!=null)changed.run();}}
 private static final class State {final PersistentInventory inventory=new PersistentInventory();CompoundTag source;boolean loading;}
 public static SimpleContainer inventory(ServerPlayer p){
  var d=RpgPlayers.get(p);var state=STATES.get(p);
  if(state==null){state=new State();STATES.put(p,state);var weak=new java.lang.ref.WeakReference<>(p);var saved=state;state.inventory.changed=()->{var owner=weak.get();if(owner!=null&&!saved.loading){var tag=encode(owner,saved.inventory);RpgPlayers.get(owner).backpacks=tag;saved.source=tag;RpgPlayers.get(owner).dirty=true;}};}
  if(state.source!=d.backpacks){state.loading=true;var ops=RegistryOps.create(NbtOps.INSTANCE,p.level().registryAccess());var list=d.backpacks.getListOrEmpty("items");for(int i=0;i<54;i++)state.inventory.setItem(i,i<list.size()?ItemStack.OPTIONAL_CODEC.parse(ops,list.get(i)).getOrThrow():ItemStack.EMPTY);state.loading=false;state.source=d.backpacks;}
  return state.inventory;
 }
 public static CompoundTag encode(ServerPlayer p,Container c){var tag=new CompoundTag();var list=new ListTag();var ops=RegistryOps.create(NbtOps.INSTANCE,p.level().registryAccess());for(int i=0;i<54;i++)list.add(ItemStack.OPTIONAL_CODEC.encodeStart(ops,c.getItem(i)).getOrThrow());tag.put("items",list);return tag;}
 public static SimpleContainer decode(CompoundTag tag,net.minecraft.core.RegistryAccess registries){var c=new SimpleContainer(54);var list=tag.getListOrEmpty("items");var ops=RegistryOps.create(NbtOps.INSTANCE,registries);for(int i=0;i<Math.min(54,list.size());i++)c.setItem(i,ItemStack.OPTIONAL_CODEC.parse(ops,list.get(i)).result().orElse(ItemStack.EMPTY));return c;}
 public static int capacity(Container c){int n=0;for(int i=0;i<2;i++)if(c.getItem(i).getItem() instanceof BackpackItem bag)n+=bag.capacity();return Math.min(48,n);}
 private static int without(Container c,int index){return c.getItem(1-index).getItem() instanceof BackpackItem bag?bag.capacity():0;}
 private static boolean removable(Container c,int index){for(int i=2+without(c,index);i<50;i++)if(!c.getItem(i).isEmpty())return false;return true;}
 public static void restrict(ChestMenu menu){var c=menu.getContainer();for(int i=0;i<54;i++){var old=menu.slots.get(i);final int index=i;var slot=new Slot(c,i,old.x,old.y){
   @Override public boolean mayPlace(ItemStack stack){return index<2?stack.getItem() instanceof BackpackItem:index<2+capacity(c)&&!(stack.getItem() instanceof BackpackItem);}
   @Override public boolean mayPickup(Player p){return index<2?removable(c,index):index<2+capacity(c);}
   @Override public boolean isActive(){return index<2||index<2+capacity(c);}
   @Override public int getMaxStackSize(){return index<2?1:64;}
  };slot.index=old.index;menu.slots.set(i,slot);}}
 public static void open(ServerPlayer p){
        if(!FeatureFlags.allow(p,"sacs"))return;if(!p.isAlive()||p.isSpectator())return;var c=inventory(p);p.openMenu(new SimpleMenuProvider((id,inv,player)->{var menu=new ChestMenu(MenuType.GENERIC_9x6,id,inv,c,6);restrict(menu);return menu;},TITLE));}
 public static void equipAndOpen(ServerPlayer p,InteractionHand hand){if(!FeatureFlags.allow(p,"sacs"))return;var c=inventory(p);var held=p.getItemInHand(hand);if(held.getItem() instanceof BackpackItem)for(int i=0;i<2;i++)if(c.getItem(i).isEmpty()){c.setItem(i,held.copyWithCount(1));held.shrink(1);break;}open(p);}
 public static boolean insert(ServerPlayer p,ItemStack incoming){
  if(!FeatureFlags.enabled(p.level().getServer(),"sacs"))return incoming.isEmpty();
  if(incoming.isEmpty()||incoming.getItem() instanceof BackpackItem)return incoming.isEmpty();var c=inventory(p);int end=2+capacity(c);
  for(int pass=0;pass<2&&!incoming.isEmpty();pass++)for(int i=2;i<end&&!incoming.isEmpty();i++){var current=c.getItem(i);if(pass==0&&!current.isEmpty()&&ItemStack.isSameItemSameComponents(current,incoming)){int n=Math.min(incoming.getCount(),current.getMaxStackSize()-current.getCount());if(n>0){current.grow(n);incoming.shrink(n);c.setChanged();}}else if(pass==1&&current.isEmpty()){int n=Math.min(incoming.getCount(),incoming.getMaxStackSize());c.setItem(i,incoming.copyWithCount(n));incoming.shrink(n);}}
  return incoming.isEmpty();
 }
 @net.neoforged.bus.api.SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){STATES.remove(e.getEntity());}
 @net.neoforged.bus.api.SubscribeEvent public static void stop(net.neoforged.neoforge.event.server.ServerStoppedEvent e){STATES.clear();}
}
