package com.mmorpg.client.cinematic;

import net.minecraft.util.Mth;

/** Courbes d'amortissement (0..1 -> 0..1) utilisees par la camera, les fondus et les titres. */
public enum Ease {
    LINEAR {
        @Override
        public float apply(float t) {
            return Mth.clamp(t, 0F, 1F);
        }
    },
    IN_CUBIC {
        @Override
        public float apply(float t) {
            t = Mth.clamp(t, 0F, 1F);
            return t * t * t;
        }
    },
    OUT_CUBIC {
        @Override
        public float apply(float t) {
            float u = 1F - Mth.clamp(t, 0F, 1F);
            return 1F - u * u * u;
        }
    },
    IN_OUT_CUBIC {
        @Override
        public float apply(float t) {
            t = Mth.clamp(t, 0F, 1F);
            return t < 0.5F ? 4F * t * t * t : 1F - (float) Math.pow(-2F * t + 2F, 3) / 2F;
        }
    },
    IN_OUT_QUINT {
        @Override
        public float apply(float t) {
            t = Mth.clamp(t, 0F, 1F);
            return t < 0.5F ? 16F * t * t * t * t * t : 1F - (float) Math.pow(-2F * t + 2F, 5) / 2F;
        }
    };

    public abstract float apply(float t);

    /** Nom du fichier de configuration : linear, inCubic, outCubic, inOutCubic, inOutQuint (par defaut inOutCubic). */
    public static Ease byName(String name) {
        if (name == null) return IN_OUT_CUBIC;
        return switch (name) {
            case "linear" -> LINEAR;
            case "inCubic" -> IN_CUBIC;
            case "outCubic" -> OUT_CUBIC;
            case "inOutQuint" -> IN_OUT_QUINT;
            default -> IN_OUT_CUBIC;
        };
    }
}
