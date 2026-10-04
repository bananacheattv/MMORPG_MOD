package com.mmorpg.client.model.structure;

import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Modele d'une structure multibloc avec ses trois animations d'etat : repos (inactif / eteinte), activation, actif.
 * Chaque passe de rendu a sa propre instance (pose calculee au moment du rendu, visibilite du portail propre a la passe).
 */
public class StructureModel extends Model<StructureRenderer.State> {
    public enum Pass { MAIN, GLOW, PORTAL }

    private final KeyframeAnimation idle;
    private final KeyframeAnimation activation;
    private final KeyframeAnimation active;
    private final float activationLength;
    private final Pass pass;
    private final @Nullable ModelPart portal;
    private final List<ModelPart> others = new ArrayList<>();

    /**
     * @param anims  indices (repos, activation, actif) dans {@code defs}
     * @param portal os de la surface du portail (ou null) ; {@code siblings} = autres os enfants de 'racine'
     */
    public StructureModel(ModelPart root, AnimationDefinition[] defs, int[] anims, Pass pass, @Nullable String portal, String... siblings) {
        super(root, RenderTypes::entityCutout);
        this.idle = defs[anims[0]].bake(root);
        this.activation = defs[anims[1]].bake(root);
        this.active = defs[anims[2]].bake(root);
        this.activationLength = defs[anims[1]].lengthInSeconds();
        this.pass = pass;
        var lookup = root.createPartLookup();
        this.portal = portal == null ? null : lookup.apply(portal);
        for (String s : siblings) {
            ModelPart p = lookup.apply(s);
            if (p != null) others.add(p);
        }
    }

    @Override
    public void setupAnim(StructureRenderer.State state) {
        super.setupAnim(state);
        if (!state.active) {
            idle.apply((long) (state.time * 1000.0F), 1.0F);
        } else if (state.sinceActive < activationLength) {
            activation.apply((long) (state.sinceActive * 1000.0F), 1.0F);
        } else {
            active.apply((long) ((state.sinceActive - activationLength) * 1000.0F), 1.0F);
        }
        if (portal != null) {
            boolean portalPass = pass == Pass.PORTAL;
            portal.visible = portalPass;
            for (ModelPart p : others) p.visible = !portalPass;
        }
    }
}
