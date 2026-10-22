package com.mmorpg.registry;

import com.mmorpg.MMORPG;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Effets sonores originaux d'Eldoria (synthetises par tools/gen_sfx.py, fichiers dans assets/mmorpg/sounds/sfx) : cinematiques du
 * hub, donjons, epreuves et boss. Aucun son de Minecraft n'y est reutilise.
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MMORPG.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> RISE = reg("rise");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOOM = reg("boom");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHOOSH = reg("whoosh");
    public static final DeferredHolder<SoundEvent, SoundEvent> BELL = reg("bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHIME = reg("chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> WARRIOR = reg("warrior");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARROW = reg("arrow");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARCANE = reg("arcane");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHIELD = reg("shield");
    public static final DeferredHolder<SoundEvent, SoundEvent> FANFARE = reg("fanfare");
    public static final DeferredHolder<SoundEvent, SoundEvent> GROWL = reg("growl");
    public static final DeferredHolder<SoundEvent, SoundEvent> RITUAL = reg("ritual");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEARTBEAT = reg("heartbeat");
    public static final DeferredHolder<SoundEvent, SoundEvent> HORN = reg("horn");
    public static final DeferredHolder<SoundEvent, SoundEvent> DOOR = reg("door");
    public static final DeferredHolder<SoundEvent, SoundEvent> SUCCESS = reg("success");
    public static final DeferredHolder<SoundEvent, SoundEvent> ERROR = reg("error");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIREWORK = reg("firework");
    public static final DeferredHolder<SoundEvent, SoundEvent> TELEPORT = reg("teleport");
    public static final DeferredHolder<SoundEvent, SoundEvent>[] NOTES = notes();

    private ModSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String name) {
        return SOUNDS.register("sfx." + name, () -> SoundEvent.createVariableRangeEvent(MMORPG.id("sfx." + name)));
    }

    @SuppressWarnings("unchecked")
    private static DeferredHolder<SoundEvent, SoundEvent>[] notes() {
        DeferredHolder<SoundEvent, SoundEvent>[] n = new DeferredHolder[4];
        for (int i = 0; i < n.length; i++) n[i] = reg("note" + i);
        return n;
    }
}
