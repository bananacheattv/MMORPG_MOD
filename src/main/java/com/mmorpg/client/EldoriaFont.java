package com.mmorpg.client;

import com.mmorpg.MMORPG;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;

/**
 * Police « Eldoria » (assets/mmorpg/font/eldoria.json, generee par tools/gen_font.py) : capitales a empattements pour les titres
 * du hub, de la cinematique et des dialogues. Les minuscules s'affichent en petites capitales.
 */
public final class EldoriaFont {
    private EldoriaFont() {
    }

    public static final FontDescription FONT = new FontDescription.Resource(MMORPG.id("eldoria"));

    public static MutableComponent of(String text) {
        return Component.literal(text).withStyle(style -> style.withFont(FONT));
    }
}
