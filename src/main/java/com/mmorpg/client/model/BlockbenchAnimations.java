package com.mmorpg.client.model;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;
import org.joml.Vector3f;

/**
 * Lit les animations exportees depuis les projets Blockbench (art/boss_blockbench, entity_kit.java_export).
 * Format compact, une piste par ligne : {@code os|canal|t,x,y,z,interp;t,x,y,z,interp;...} avec canal r / p / s
 * (rotation en degres, position, echelle) et interp l / c (lineaire / catmullrom). Les valeurs sont celles que
 * l'export Java de Blockbench passerait a degreeVec / posVec / scaleVec. Le texte est decoupe en morceaux pour
 * rester sous la limite de taille des constantes Java.
 */
public final class BlockbenchAnimations {
    private BlockbenchAnimations() {
    }

    public static AnimationDefinition parse(String[] chunks, float length, boolean looping) {
        String data = String.join("", chunks);
        AnimationDefinition.Builder builder = AnimationDefinition.Builder.withLength(length);
        if (looping) {
            builder.looping();
        }
        for (String line : data.split("\n")) {
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split("\\|");
            char channel = parts[1].charAt(0);
            String[] keys = parts[2].split(";");
            Keyframe[] frames = new Keyframe[keys.length];
            for (int i = 0; i < keys.length; i++) {
                String[] v = keys[i].split(",");
                float t = Float.parseFloat(v[0]);
                float x = Float.parseFloat(v[1]);
                float y = Float.parseFloat(v[2]);
                float z = Float.parseFloat(v[3]);
                Vector3f vec = switch (channel) {
                    case 'r' -> KeyframeAnimations.degreeVec(x, y, z);
                    case 'p' -> KeyframeAnimations.posVec(x, y, z);
                    default -> KeyframeAnimations.scaleVec(x, y, z);
                };
                frames[i] = new Keyframe(t, vec, "c".equals(v[4]) ? AnimationChannel.Interpolations.CATMULLROM : AnimationChannel.Interpolations.LINEAR);
            }
            AnimationChannel.Target target = switch (channel) {
                case 'r' -> AnimationChannel.Targets.ROTATION;
                case 'p' -> AnimationChannel.Targets.POSITION;
                default -> AnimationChannel.Targets.SCALE;
            };
            builder.addAnimation(parts[0], new AnimationChannel(target, frames));
        }
        return builder.build();
    }
}
