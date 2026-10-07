package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.client.screen.MenuScreen;
import com.mmorpg.network.Payloads;
import com.mmorpg.rpg.PlayerData;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/** Raccourcis configurables, charges uniquement sur le client physique. */
@EventBusSubscriber(modid = MMORPG.MODID, value = Dist.CLIENT)
public final class Keys {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(MMORPG.id("mmorpg"));
    public static final KeyMapping MENU = new KeyMapping("key.mmorpg.menu", InputConstants.KEY_M, CATEGORY);
    public static final KeyMapping MOUNT = new KeyMapping("key.mmorpg.mount", InputConstants.KEY_N, CATEGORY);
    public static final KeyMapping HUD_EDIT = new KeyMapping("key.mmorpg.hud_edit", InputConstants.KEY_U, CATEGORY);
    public static final KeyMapping[] SKILLS = createSkills();

    private Keys() {
    }

    private static KeyMapping[] createSkills() {
        int[] defaults = {InputConstants.KEY_R, InputConstants.KEY_B, InputConstants.KEY_G,
                InputConstants.KEY_H, InputConstants.KEY_J, InputConstants.KEY_V};
        KeyMapping[] mappings = new KeyMapping[PlayerData.SKILL_SLOTS];
        for (int i = 0; i < mappings.length; i++) {
            mappings[i] = new KeyMapping("key.mmorpg.skill" + (i + 1),
                    i < defaults.length ? defaults[i] : InputConstants.UNKNOWN.getValue(), CATEGORY);
        }
        return mappings;
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(MENU);
        event.register(HUD_EDIT);
        event.register(MOUNT);
        for (KeyMapping skill : SKILLS) event.register(skill);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (MENU.consumeClick()) {
            if (mc.player != null && mc.level != null && mc.gui.screen() == null) {
                MenuScreen.open(MenuScreen.lastTab);
            }
        }
        while (MOUNT.consumeClick()) {
            if (mc.player != null && mc.level != null && mc.gui.screen() == null) {
                ClientNet.send(new Payloads.MountAction(Payloads.MountAction.TOGGLE, ""));
            }
        }
        while (HUD_EDIT.consumeClick()) {
            if (mc.player != null && mc.level != null && mc.gui.screen() == null) {
                mc.gui.setScreen(new com.mmorpg.client.screen.HudEditorScreen(null));
            }
        }
        for (int slot = 0; slot < SKILLS.length; slot++) {
            while (SKILLS[slot].consumeClick()) {
                if (mc.player != null && mc.level != null && mc.gui.screen() == null) {
                    ClientNet.send(new Payloads.SkillAction(Payloads.SkillAction.CAST, slot, ""));
                }
            }
        }
    }
}
