package com.mmorpg.client;
import com.mmorpg.client.screen.*;
import com.mmorpg.client.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.*;
public final class DevPvpVisual {
 private static int tick,loadingTicks;private static boolean loadingCaptured;private static String originalLayout;
 private static void shot(Minecraft mc,String name){net.minecraft.client.Screenshot.grab(mc.gameDirectory,"showcase_"+name+".png",mc.gameRenderer.mainRenderTarget(),1,msg->com.mmorpg.MMORPG.LOGGER.info("[MMORPG] Capture {}",name));}
 private static void command(Minecraft mc,String text){mc.player.connection.sendCommand(text);}
 public static void tick(Minecraft mc){if(!loadingCaptured&&mc.gui.screen() instanceof net.minecraft.client.gui.screens.ConnectScreen&&++loadingTicks==5){shot(mc,"portal_without_cancel");loadingCaptured=true;}if(mc.player==null||mc.level==null)return;tick++;mc.gui.toastManager().clear();
  if(tick==60){command(mc,"mmorpg niveau VisualChecks 100");command(mc,"class set VisualChecks paladin");String[] armor={"head","chest","legs","feet"};for(int i=0;i<4;i++)command(mc,"item replace entity @s armor."+armor[i]+" with mmorpg:pvp_tank_4_"+com.mmorpg.item.ItemDefs.PIECES[i]);String[] items={"potion_soin_majeure","potion_mana_majeure","cle_rare","cle_epique","charme_chance","pierre_amelioration_sup","lucky_neant","pvp_tank_4_arme","parchemin_teleportation"};for(int i=0;i<items.length;i++)command(mc,"item replace entity @s inventory."+i+" with mmorpg:"+items[i]);}
  if(tick==80){originalLayout=CharacterLayout.snapshot();CharacterLayout.reset();mc.options.guiScale().set(2);mc.getWindow().setWindowed(1280,720);mc.resizeGui();mc.gui.setScreen(new CharacterScreen(mc.player));}
  if(tick==110)shot(mc,"inventory_large_two_rows");
  if(tick==120&&mc.gui.screen() instanceof CharacterScreen screen)screen.editLayout();
  if(tick==145)shot(mc,"inventory_layout_editor");
  if(tick==155){CharacterLayout.box(CharacterLayout.Module.ATTRIBUTES).visible=false;CharacterLayout.box(CharacterLayout.Module.EQUIPMENT).visible=false;CharacterLayout.move(CharacterLayout.Module.INVENTORY,24,100,mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight());CharacterLayout.scale(CharacterLayout.Module.INVENTORY,1.5f);CharacterLayout.save();CharacterLayout.reload();if(CharacterLayout.box(CharacterLayout.Module.ATTRIBUTES).visible||Math.abs(CharacterLayout.box(CharacterLayout.Module.INVENTORY).scale-1.5)>0.01)throw new AssertionError("custom character layout persistence");mc.gui.setScreen(new CharacterScreen(mc.player));}
  if(tick==180)shot(mc,"inventory_custom_layout");
  if(tick==190){CharacterLayout.reset();mc.gui.setScreen(new CharacterScreen(mc.player));mc.getWindow().setWindowed(960,540);mc.resizeGui();}
  if(tick==215)shot(mc,"inventory_small_window");
  if(tick==225){mc.getWindow().setWindowed(1280,720);mc.resizeGui();MenuScreen.open(MenuScreen.Tab.COMPETENCES);}
  if(tick==250)shot(mc,"menus_two_rows");
  if(tick==260){mc.gui.setScreen(null);var t=new CompoundTag();t.putBoolean("active",true);t.putInt("red",287);t.putInt("blue",195);t.putInt("owner",-1);t.putInt("hits",34);t.putInt("team",0);t.putInt("danger",42);ClientData.arenaState=t;ClientData.arenaUpdated=ClientData.now();}
  if(tick==280)shot(mc,"arena_live_hud");
  if(tick==290){ClientData.arenaState=new CompoundTag();var r=new com.mmorpg.competition.PvpProgression.Report();r.id=java.util.UUID.randomUUID().toString();r.red=500;r.blue=284;r.winner=0;for(int i=0;i<10;i++){var row=new com.mmorpg.competition.PvpProgression.Row();row.id=i==0?mc.player.getUUID().toString():java.util.UUID.randomUUID().toString();row.name=i==0?mc.player.getGameProfile().name():new String[]{"","Lysandre","Orin","Vaël","Élyra","Néria","Kaël","Borin","Sélène","Aëron"}[i];row.team=i<5?0:1;row.kills=5-i%4;row.deaths=i%4;row.strikes=18-i;row.damage=8200-i*430;row.healing=i*210;row.points=i<5?148:74;r.rows.add(row);}mc.gui.setScreen(new ArenaResultsScreen(com.mmorpg.competition.PvpProgression.reportTag(r,mc.player.getUUID())));}
  if(tick==320)shot(mc,"arena_results");
  if(tick==330){var d=new CompoundTag();d.putLong("points",2350);var products=new ListTag();for(var p:com.mmorpg.competition.PvpProgression.products()){if(p.cls()>=0&&p.cls()!=com.mmorpg.rpg.PlayerClass.TANK.ordinal())continue;var t=new CompoundTag();t.putString("id",p.id());t.putString("label",p.label());t.putString("item",p.item());t.putString("unlock",p.unlock());t.putLong("price",p.price());t.putInt("count",p.count());t.putInt("level",p.level());products.add(t);}d.put("products",products);var screen=new PvpStoreScreen(d);screen.selectCategory(1);mc.gui.setScreen(screen);}
  if(tick==355)shot(mc,"pvp_merchant");
  if(tick==365)mc.gui.setScreen(new DevPvpGallery());
  if(tick==400)shot(mc,"pvp_armour_and_mount");
  if(tick==410){mc.gui.setScreen(null);command(mc,"class set VisualChecks archer");command(mc,"item replace entity @s weapon.mainhand with mmorpg:arc_aube_divine");command(mc,"time set day");}
  if(tick==435)mc.gameMode.useItem(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND);
  if(tick==450)shot(mc,"bow_aim_first_person");
  if(tick==460)mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
  if(tick==480)shot(mc,"bow_aim_third_person");
  if(tick==490){mc.gameMode.releaseUsingItem(mc.player);mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);command(mc,"summon minecraft:armor_stand ~ ~ ~2 {ShowArms:true}");}
  if(tick==515){for(var e:mc.level.entitiesForRendering())if(e instanceof net.minecraft.world.entity.decoration.ArmorStand&&e.distanceToSqr(mc.player)<30){var data=new CompoundTag();data.putInt("entity",e.getId());var screen=new ArmorStandEditorScreen(data);screen.appearanceTab();mc.gui.setScreen(screen);break;}}
  if(tick==540)shot(mc,"armor_stand_skin_3d_editor");
  if(tick==550){mc.gui.setScreen(null);command(mc,"class set VisualChecks paladin");command(mc,"item replace entity @s weapon.mainhand with mmorpg:marteau_egide");}
  if(tick==575)mc.gameMode.useItem(mc.player,net.minecraft.world.InteractionHand.OFF_HAND);
  if(tick==620)shot(mc,"shield_guard_draining");
  if(tick==630)mc.gameMode.releaseUsingItem(mc.player);
  if(tick==760)shot(mc,"shield_guard_regenerated");
  if(tick==780){mc.gui.setScreen(null);command(mc,"item replace entity @s inventory.0 with minecraft:diamond 8");command(mc,"item replace entity @s inventory.1 with minecraft:emerald 6");ClientNet.send(new com.mmorpg.network.Payloads.SocialAction("backpack",new CompoundTag()));}
  if(tick==810&&mc.gui.screen() instanceof BackpackScreen screen){var slot=screen.getMenu().getSlot(54);int x=(mc.getWindow().getGuiScaledWidth()-176)/2+slot.x+8,y=(mc.getWindow().getGuiScaledHeight()-222)/2+slot.y+8;screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x,y,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,1)),false);}
  if(tick==815&&mc.gui.screen() instanceof BackpackScreen screen){var slot=screen.getMenu().getSlot(55);int x=(mc.getWindow().getGuiScaledWidth()-176)/2+slot.x+8,y=(mc.getWindow().getGuiScaledHeight()-222)/2+slot.y+8;screen.mouseDragged(new net.minecraft.client.input.MouseButtonEvent(x,y,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,1)),20,0);}
  if(tick==835&&mc.gui.screen() instanceof BackpackScreen screen){if(screen.getMenu().getSlot(54).hasItem()||screen.getMenu().getSlot(55).hasItem()||!screen.getMenu().getCarried().isEmpty())throw new AssertionError("Shift-click and Shift-drag must quick-move both stacks without cursor pickup");shot(mc,"shift_drag_transfer");com.mmorpg.MMORPG.LOGGER.info("[CODEX CHECKS] SHIFT_DRAG_NATIVE PASS: actual client clicks, real packets, two stacks quick-moved without cursor pickup");}
  if(tick==845){mc.player.closeContainer();mc.gui.setScreen(new DevSculptedGallery());}
  if(tick==870)shot(mc,"weapons_refined_again");
  if(tick==880)mc.gui.setScreen(new DevChestProof());
  if(tick==910)shot(mc,"chest_hinges_open_closed");
  if(tick==930){CharacterLayout.restore(originalLayout);mc.options.pauseOnLostFocus=true;mc.options.save();mc.stop();}
 }
}
