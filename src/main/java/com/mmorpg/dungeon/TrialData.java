package com.mmorpg.dungeon;

import com.mmorpg.MMORPG;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Epreuves des donjons de boss, enregistrees a la construction : vagues de monstres (herse qui s'ouvre quand tout est mort),
 * parcours de saut (une chute ramene au depart), enigme de sequence (lutrin et quatre dalles).
 * Les coordonnees sont absolues ; les tableaux {@code box} et {@code gate} contiennent x0, y0, z0, x1, y1, z1.
 */
public class TrialData extends SavedData {
    public static final String WAVES = "waves";
    public static final String JUMP = "jump";
    public static final String SIMON = "simon";

    /**
     * @param box    zone de declenchement (vagues, enigme) ou zone de chute (parcours : tombe si y < y0)
     * @param gate   herse a ouvrir (vagues, enigme) ou zone d'arrivee (parcours)
     * @param pos    centre de la salle (vagues), lutrin (enigme) ou point de depart (parcours)
     * @param pads   x, y, z de chaque dalle de l'enigme
     * @param states etat eteint puis allume de chaque dalle (identifiants de blocs), a la suite
     */
    public record Trial(String id, String kind, ResourceKey<Level> dim, List<Integer> box, List<Integer> gate, String gateBlock,
                        List<Integer> pos, List<Integer> pads, List<String> states, int level, int diff, List<String> mobs,
                        String title, String hint, boolean done) {
        static final Codec<Trial> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(Trial::id),
                Codec.STRING.fieldOf("kind").forGetter(Trial::kind),
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dim").forGetter(Trial::dim),
                Codec.INT.listOf().fieldOf("box").forGetter(Trial::box),
                Codec.INT.listOf().optionalFieldOf("gate", List.of()).forGetter(Trial::gate),
                Codec.STRING.optionalFieldOf("gateBlock", "").forGetter(Trial::gateBlock),
                Codec.INT.listOf().fieldOf("pos").forGetter(Trial::pos),
                Codec.INT.listOf().optionalFieldOf("pads", List.of()).forGetter(Trial::pads),
                Codec.STRING.listOf().optionalFieldOf("states", List.of()).forGetter(Trial::states),
                Codec.INT.optionalFieldOf("level", 1).forGetter(Trial::level),
                Codec.INT.optionalFieldOf("diff", 0).forGetter(Trial::diff),
                Codec.STRING.listOf().optionalFieldOf("mobs", List.of()).forGetter(Trial::mobs),
                Codec.STRING.optionalFieldOf("title", "").forGetter(Trial::title),
                Codec.STRING.optionalFieldOf("hint", "").forGetter(Trial::hint),
                Codec.BOOL.optionalFieldOf("done", false).forGetter(Trial::done)
        ).apply(i, Trial::new));

        Trial withDone() {
            return new Trial(id, kind, dim, box, gate, gateBlock, pos, pads, states, level, diff, mobs, title, hint, true);
        }

        boolean inBox(double x, double y, double z) {
            return in(box, x, y, z);
        }

        boolean inGate(double x, double y, double z) {
            return in(gate, x, y, z);
        }

        private static boolean in(List<Integer> b, double x, double y, double z) {
            return b.size() == 6 && x >= b.get(0) && x < b.get(3) + 1 && y >= b.get(1) && y < b.get(4) + 1 && z >= b.get(2) && z < b.get(5) + 1;
        }
    }

    private static final Codec<TrialData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Trial.CODEC.listOf().optionalFieldOf("trials", List.of()).forGetter(d -> d.trials)
    ).apply(i, TrialData::new));
    public static final SavedDataType<TrialData> TYPE = new SavedDataType<>(MMORPG.id("trials"), TrialData::new, CODEC);

    private final List<Trial> trials;

    public TrialData() {
        this.trials = new ArrayList<>();
    }

    private TrialData(List<Trial> trials) {
        this.trials = new ArrayList<>(trials);
    }

    public static TrialData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public List<Trial> all() {
        return List.copyOf(trials);
    }

    public @Nullable Trial byId(String id) {
        for (Trial t : trials) {
            if (t.id().equals(id)) return t;
        }
        return null;
    }

    /** Ajoute (ou remplace) une epreuve. */
    public void add(Trial trial) {
        trials.removeIf(t -> t.id().equals(trial.id()));
        trials.add(trial);
        setDirty();
    }

    void markDone(String id) {
        for (int i = 0; i < trials.size(); i++) {
            if (trials.get(i).id().equals(id)) {
                trials.set(i, trials.get(i).withDone());
                setDirty();
                return;
            }
        }
    }

    /** Efface toutes les epreuves dont le centre tombe dans l'emprise donnee (reconstruction au meme endroit). */
    public void removeInside(ResourceKey<Level> dim, int x0, int z0, int x1, int z1) {
        if (trials.removeIf(t -> t.dim().equals(dim) && t.pos().get(0) >= x0 && t.pos().get(0) <= x1 && t.pos().get(2) >= z0 && t.pos().get(2) <= z1)) setDirty();
    }

    /** Efface les epreuves d'un donjon (reconstruction au meme endroit). */
    public void removePrefix(String prefix) {
        if (trials.removeIf(t -> t.id().startsWith(prefix))) setDirty();
    }
}
