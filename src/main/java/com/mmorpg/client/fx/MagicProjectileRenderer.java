package com.mmorpg.client.fx;

import com.mmorpg.entity.MagicProjectile;
import com.mmorpg.item.ItemDefs;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Projectile magique des batons et des boss : orbe coloree selon l'element, avec trainee. */
public class MagicProjectileRenderer extends EntityRenderer<MagicProjectile, SpellProjectileRenderer.State> {
    public MagicProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public SpellProjectileRenderer.State createRenderState() {
        return new SpellProjectileRenderer.State();
    }

    @Override
    public void extractRenderState(MagicProjectile e, SpellProjectileRenderer.State s, float partial) {
        super.extractRenderState(e, s, partial);
        s.age = e.tickCount + partial;
        s.seed = e.getId();
        s.kind = switch (e.element()) {
            case FEU -> com.mmorpg.entity.SpellProjectile.Kind.FIREBALL;
            case ECLAIR -> com.mmorpg.entity.SpellProjectile.Kind.LIGHTNING_ORB;
            default -> com.mmorpg.entity.SpellProjectile.Kind.VOLLEY_ARROW;
        };
        int[] c = element(e.element());
        s.c1 = c[0];
        s.c2 = c[1];
        s.c3 = c[2];
        s.trail.clear();
        for (Vec3 p : e.trailPositions) s.trail.add(new Vector3f((float) (p.x - s.x), (float) (p.y - s.y), (float) (p.z - s.z)));
    }

    /** Couleurs (principale, coeur, trainee) de chaque element. */
    private static int[] element(ItemDefs.Element el) {
        return switch (el) {
            case FEU -> new int[]{0xFF7A1E, 0xFFF0C0, 0xFF3A10};
            case GIVRE -> new int[]{0x9AE4FF, 0xFFFFFF, 0x4AA8FF};
            case NEANT -> new int[]{0xB060FF, 0xF0D8FF, 0x6020C0};
            case OMBRE -> new int[]{0x7040A0, 0xD0B0FF, 0x301050};
            case ECLAIR -> new int[]{0xFFF080, 0xFFFFFF, 0xC0B040};
            default -> new int[]{0xD070FF, 0xFFE0FF, 0x8040D0};
        };
    }

    @Override
    public void submit(SpellProjectileRenderer.State s, PoseStack ps, SubmitNodeCollector out, CameraRenderState camera) {
        SpellProjectileRenderer.orb(s, ps, out, camera.orientation, s.c1, s.c2, 0.5f, s.c3);
        super.submit(s, ps, out, camera);
    }
}
