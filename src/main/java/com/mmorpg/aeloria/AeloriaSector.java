package com.mmorpg.aeloria;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Une ligne de importation.csv. */
record AeloriaSector(
        int order, String file, int x, int y, int z, int width, int height, int length, long bytes, String sha256) {

    static List<AeloriaSector> parse(InputStream in) throws IOException {
        String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        if (!text.isEmpty() && text.charAt(0) == '﻿') {
            text = text.substring(1);
        }
        String[] lines = text.split("\\r?\\n");
        if (lines.length < 2) {
            throw new IOException("CSV vide");
        }
        String[] head = lines[0].trim().split(",");
        Map<String, Integer> col = new HashMap<>();
        for (int i = 0; i < head.length; i++) {
            col.put(head[i].trim().toLowerCase(Locale.ROOT), i);
        }
        for (String c : new String[] {
            "ordre", "fichier", "collage_x", "collage_y", "collage_z", "largeur_x", "hauteur_y", "longueur_z",
            "octets", "sha256"
        }) {
            if (!col.containsKey(c)) {
                throw new IOException("colonne CSV manquante : " + c);
            }
        }
        List<AeloriaSector> out = new ArrayList<>();
        Set<Integer> orders = new HashSet<>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            String[] f = lines[i].trim().split(",");
            AeloriaSector s;
            try {
                s = new AeloriaSector(
                        Integer.parseInt(f[col.get("ordre")]),
                        f[col.get("fichier")],
                        Integer.parseInt(f[col.get("collage_x")]),
                        Integer.parseInt(f[col.get("collage_y")]),
                        Integer.parseInt(f[col.get("collage_z")]),
                        Integer.parseInt(f[col.get("largeur_x")]),
                        Integer.parseInt(f[col.get("hauteur_y")]),
                        Integer.parseInt(f[col.get("longueur_z")]),
                        Long.parseLong(f[col.get("octets")]),
                        f[col.get("sha256")].toLowerCase(Locale.ROOT));
            } catch (RuntimeException e) {
                throw new IOException("ligne CSV " + (i + 1) + " invalide : " + e);
            }
            if (!orders.add(s.order())) {
                throw new IOException("ordre dupliqué " + s.order());
            }
            out.add(s);
        }
        out.sort(Comparator.comparingInt(AeloriaSector::order));
        return out;
    }

    static List<AeloriaSector> parse(Path p) throws IOException {
        try (InputStream in = Files.newInputStream(p)) {
            return parse(in);
        }
    }
}
