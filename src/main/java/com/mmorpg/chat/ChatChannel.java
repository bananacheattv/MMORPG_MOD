package com.mmorpg.chat;

import java.util.regex.Pattern;

/** Canaux de discussion : classement des lignes recues et routage des messages saisis (logique pure, sans Minecraft). */
public enum ChatChannel {
    GENERAL("Général", 0xE8D9FF, null),
    GUILDE("Guilde", 0x4FD1A5, "g"),
    GROUPE("Groupe", 0x60C0FF, "p");

    private static final Pattern GUILD_LINE = Pattern.compile("^\\[Guilde] [^:\\n]+ : .*", Pattern.DOTALL);
    private static final Pattern PARTY_LINE = Pattern.compile("^\\[Groupe] [^:\\n]+ : .*", Pattern.DOTALL);

    public final String label;
    public final int color;
    /** Commande serveur du canal, sans « / » ; null pour le chat general. */
    public final String command;

    ChatChannel(String label, int color, String command) {
        this.label = label;
        this.color = color;
        this.command = command;
    }

    /** Canal d'une ligne recue : seuls les vrais messages de guilde / groupe quittent le canal general. */
    public static ChatChannel classify(String line) {
        if (line == null) return GENERAL;
        if (GUILD_LINE.matcher(line).matches()) return GUILDE;
        if (PARTY_LINE.matcher(line).matches()) return GROUPE;
        return GENERAL;
    }

    /** Texte réellement envoyé : les commandes restent intactes, le reste part dans le canal choisi. */
    public String route(String input) {
        if (input == null) return "";
        String text = input.strip();
        if (text.isEmpty() || text.startsWith("/") || command == null) return input;
        return "/" + command + " " + text;
    }

    public ChatChannel next(int step) {
        ChatChannel[] all = values();
        return all[Math.floorMod(ordinal() + step, all.length)];
    }

    public static ChatChannel byName(String name) {
        for (ChatChannel c : values()) if (c.name().equalsIgnoreCase(name == null ? "" : name.strip())) return c;
        return GENERAL;
    }
}
