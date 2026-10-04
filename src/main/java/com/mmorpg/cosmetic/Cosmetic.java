package com.mmorpg.cosmetic;

import java.util.ArrayList;
import java.util.List;

/** Cosmetiques animes (particules) : auras, ailes, halos et trainees. */
public enum Cosmetic {
    TRAINEE_COEURS("trainee_coeurs", "Traînée de Cœurs", Category.TRAINEE, 5, 0xFFFF6080,
            "Des cœurs s'échappent de vos pas."),
    AURA_FLAMMES("aura_flammes", "Aura de Flammes", Category.AURA, 10, 0xFFFF8020,
            "Une double hélice de flammes tourbillonne autour de vous."),
    HALO_DORE("halo_dore", "Halo Doré", Category.HALO, 25, 0xFFFFD040,
            "Un anneau de lumière dorée tourne au-dessus de votre tête."),
    TRAINEE_ETOILES("trainee_etoiles", "Traînée d'Étoiles", Category.TRAINEE, 40, 0xFFFFFFA0,
            "Une pluie d'étoiles scintillantes suit chacun de vos pas."),
    AILES_ANGELIQUES("ailes_angeliques", "Ailes Angéliques", Category.AILES, 50, 0xFFF0F8FF,
            "De grandes ailes de lumière blanche battent dans votre dos."),
    COURONNE_INFERNALE("couronne_infernale", "Couronne Infernale", Category.HALO, 75, 0xFFFF4020,
            "Une couronne de flammes infernales flotte au-dessus de vous."),
    VORTEX_NEANT("vortex_neant", "Vortex du Néant", Category.AURA, 100, 0xFFB040FF,
            "Un tourbillon d'énergie du Néant vous enveloppe."),
    AURA_GIVRE("aura_givre", "Aura de Givre", Category.AURA, -1, 0xFF90E0FF,
            "Des flocons glacés dansent en spirale autour de vous."),
    AURA_ARCANE("aura_arcane", "Aura Arcanique", Category.AURA, -1, 0xFFC080FF,
            "Des runes arcaniques orbitent autour de votre corps."),
    AILES_DEMONIAQUES("ailes_demoniaques", "Ailes Démoniaques", Category.AILES, -1, 0xFFFF3010,
            "Des ailes de flammes sombres se déploient dans votre dos."),
    AILES_FEE("ailes_fee", "Ailes de Fée", Category.AILES, -1, 0xFFFF90E0,
            "De délicates ailes irisées scintillent dans votre dos."),
    AILES_DRAGON("ailes_dragon", "Ailes du Dragon du Néant", Category.AILES, -1, 0xFF9040FF,
            "D'immenses ailes violettes de dragon battent lentement."),
    COURONNE_GIVRE("couronne_givre", "Couronne de Givre", Category.HALO, -1, 0xFFB0F0FF,
            "Une couronne de cristaux de glace scintille au-dessus de vous."),
    TRAINEE_NOTES("trainee_notes", "Traînée Mélodique", Category.TRAINEE, -1, 0xFF60FF90,
            "Des notes de musique jaillissent sur votre passage."),
    SILLAGE_ARDENT("sillage_ardent", "Sillage Ardent", Category.TRAINEE, -1, 0xFFFF6010,
            "Vos pas laissent une traînée de braises.");

    public enum Category {
        AURA("Auras"), AILES("Ailes"), HALO("Halos"), TRAINEE("Traînées");

        public final String label;

        Category(String label) {
            this.label = label;
        }
    }

    public final String id;
    public final String label;
    public final Category category;
    /** Niveau requis pour le debloquer automatiquement, ou -1 s'il s'obtient dans un Coffre Cosmetique. */
    public final int unlockLevel;
    public final int color;
    public final String description;

    Cosmetic(String id, String label, Category category, int unlockLevel, int color, String description) {
        this.id = id;
        this.label = label;
        this.category = category;
        this.unlockLevel = unlockLevel;
        this.color = color;
        this.description = description;
    }

    public static Cosmetic byId(String id) {
        for (Cosmetic c : values()) {
            if (c.id.equals(id)) return c;
        }
        return null;
    }

    public static List<Cosmetic> ofCategory(Category cat) {
        List<Cosmetic> list = new ArrayList<>();
        for (Cosmetic c : values()) {
            if (c.category == cat) list.add(c);
        }
        return list;
    }

    public String unlockText() {
        return unlockLevel > 0 ? "Débloqué au niveau " + unlockLevel : "Obtenu dans un Coffre Cosmétique";
    }
}
