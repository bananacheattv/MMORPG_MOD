package com.mmorpg.client;

import com.mmorpg.MMORPG;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;

/**
 * Police « Aeloria » (assets/mmorpg/font/aeloria.json, generee par tools/gen_font.py) : capitales a empattements pour les titres
 * du hub, de la cinematique et des dialogues. Les minuscules s'affichent en petites capitales.
 */
public final class AeloriaFont {
    private AeloriaFont() {
    }

    public static final FontDescription FONT = new FontDescription.Resource(MMORPG.id("aeloria"));

    public static MutableComponent of(String text) {
        return Component.literal(text).withStyle(style -> style.withFont(FONT));
    }
}
