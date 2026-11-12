package com.mmorpg.aeloria;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/** Lecteur minimal et autonome du format Sponge Schematic v3 (NBT compressé en gzip). */
public final class AeloriaSchematic {
    private static final String P = "Schematic.";

    int version;
    int dataVersion;
    int width;
    int height;
    int length;
    int[] offset = new int[3];
    String[] blockPalette = new String[0];
    String[] biomePalette = new String[0];
    /** Octets bruts des varints (null en mode en-tête seul). */
    byte[] blockRaw;
    byte[] biomeRaw;
    int blockEntities;

    long volume() {
        return (long) width * height * length;
    }

    /** Lit un fichier. Si headerOnly, les tableaux de données sont sautés. */
    static AeloriaSchematic read(InputStream compressed, boolean headerOnly) throws IOException {
        DataInputStream in =
                new DataInputStream(new BufferedInputStream(new GZIPInputStream(compressed, 1 << 16), 1 << 16));
        AeloriaSchematic s = new AeloriaSchematic();
        int type = in.readUnsignedByte();
        if (type != 10) {
            throw new IOException("racine NBT invalide (type " + type + ")");
        }
        in.readUTF();
        s.body(in, "", headerOnly);
        if (s.version != 3) {
            throw new IOException("format Sponge v" + s.version + " (v3 attendu)");
        }
        if (s.width <= 0 || s.height <= 0 || s.length <= 0) {
            throw new IOException("dimensions absentes ou invalides");
        }
        if (s.blockPalette.length == 0) {
            throw new IOException("palette de blocs absente");
        }
        if (!headerOnly && s.blockRaw == null) {
            throw new IOException("données de blocs absentes");
        }
        return s;
    }

    private void body(DataInputStream in, String path, boolean headerOnly) throws IOException {
        while (true) {
            int t = in.readUnsignedByte();
            if (t == 0) {
                return;
            }
            String name = in.readUTF();
            payload(in, t, path.isEmpty() ? name : path + "." + name, headerOnly);
        }
    }

    private void payload(DataInputStream in, int t, String path, boolean headerOnly) throws IOException {
        switch (t) {
            case 1 -> in.skipNBytes(1);
            case 2 -> {
                int v = in.readUnsignedShort();
                switch (path) {
                    case P + "Width" -> width = v;
                    case P + "Height" -> height = v;
                    case P + "Length" -> length = v;
                    default -> { }
                }
            }
            case 3 -> {
                int v = in.readInt();
                if (path.equals(P + "Version")) {
                    version = v;
                } else if (path.equals(P + "DataVersion")) {
                    dataVersion = v;
                }
            }
            case 4, 6 -> in.skipNBytes(8);
            case 5 -> in.skipNBytes(4);
            case 7 -> {
                int len = in.readInt();
                boolean blocks = path.equals(P + "Blocks.Data");
                boolean biomes = path.equals(P + "Biomes.Data");
                if ((blocks || biomes) && !headerOnly) {
                    byte[] raw = new byte[len];
                    in.readFully(raw);
                    if (blocks) {
                        blockRaw = raw;
                    } else {
                        biomeRaw = raw;
                    }
                } else {
                    in.skipNBytes(len);
                }
            }
            case 8 -> in.skipNBytes(in.readUnsignedShort());
            case 9 -> {
                int et = in.readUnsignedByte();
                int n = in.readInt();
                if (path.equals(P + "Blocks.BlockEntities")) {
                    blockEntities = n;
                }
                for (int i = 0; i < n; i++) {
                    payload(in, et, path + "[]", headerOnly);
                }
            }
            case 10 -> {
                if (path.equals(P + "Blocks.Palette")) {
                    blockPalette = palette(in);
                } else if (path.equals(P + "Biomes.Palette")) {
                    biomePalette = palette(in);
                } else {
                    body(in, path, headerOnly);
                }
            }
            case 11 -> {
                int len = in.readInt();
                if (path.equals(P + "Offset") && len == 3) {
                    for (int i = 0; i < 3; i++) {
                        offset[i] = in.readInt();
                    }
                } else {
                    in.skipNBytes(4L * len);
                }
            }
            case 12 -> in.skipNBytes(8L * in.readInt());
            default -> throw new IOException("type NBT inconnu " + t + " en " + path);
        }
    }

    private static String[] palette(DataInputStream in) throws IOException {
        Map<Integer, String> m = new HashMap<>();
        int max = -1;
        while (true) {
            int t = in.readUnsignedByte();
            if (t == 0) {
                break;
            }
            String name = in.readUTF();
            if (t != 3) {
                throw new IOException("entrée de palette non entière : " + name);
            }
            int idx = in.readInt();
            m.put(idx, name);
            max = Math.max(max, idx);
        }
        String[] out = new String[max + 1];
        m.forEach((i, n) -> out[i] = n);
        return out;
    }

    /** Convertit des varints en un octet par position (indices inférieurs à 256). */
    static byte[] indices(byte[] raw, long count) throws IOException {
        if (count > Integer.MAX_VALUE - 8) {
            throw new IOException("volume trop grand");
        }
        if (raw.length == count) {
            return raw; // un octet par valeur, toutes inférieures à 128
        }
        byte[] out = new byte[(int) count];
        int pos = 0;
        for (int i = 0; i < out.length; i++) {
            int value = 0;
            int shift = 0;
            while (true) {
                if (pos >= raw.length) {
                    throw new IOException("tableau de données tronqué");
                }
                int b = raw[pos++] & 0xFF;
                value |= (b & 0x7F) << shift;
                if ((b & 0x80) == 0) {
                    break;
                }
                shift += 7;
                if (shift > 28) {
                    throw new IOException("varint invalide");
                }
            }
            if (value > 255) {
                throw new IOException("palette de plus de 256 entrées non prise en charge");
            }
            out[i] = (byte) value;
        }
        if (pos != raw.length) {
            throw new IOException("données excédentaires dans le tableau");
        }
        return out;
    }
}
