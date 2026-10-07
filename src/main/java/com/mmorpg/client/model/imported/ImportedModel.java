package com.mmorpg.client.model.imported;

import com.google.gson.*;
import com.mmorpg.MMORPG;
import com.mmorpg.client.model.BlockbenchAnimations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import java.util.*;

/** Blockbench geometry with original per-face UVs, cube pivots and UUID-addressed animation bones. */
public final class ImportedModel extends EntityModel<LivingEntityRenderState> {
    private final Map<String, KeyframeAnimation> animations = new HashMap<>();
    public final Identifier texture;
    public ImportedModel(String kind, String key) { this(kind, key, read(kind, key)); }
    private ImportedModel(String kind, String key, JsonObject data) {
        super(root(data, kind.equals("cosmetics") ? 0 : 24), RenderTypes::entityTranslucent);
        texture = MMORPG.id("textures/imported/" + kind + "/" + key + ".png");
        for (var value : data.getAsJsonArray("animations")) {
            var a = value.getAsJsonObject();
            var def = BlockbenchAnimations.parse(new String[]{a.get("tracks").getAsString()},
                    a.get("length").getAsFloat(), a.get("loop").getAsBoolean());
            animations.put(a.get("name").getAsString(), def.bake(root()));
        }
    }
    private static JsonObject read(String kind, String key) {
        var id = MMORPG.id("bbmodels/" + kind + "/" + key + ".json");
        try (var reader = Minecraft.getInstance().getResourceManager().openAsReader(id)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) { throw new IllegalStateException("Invalid imported model: " + id, e); }
    }
    private static float[] vec(JsonObject j, String key) {
        float[] v = new float[3];
        if (j.has(key)) for (int i = 0; i < 3; i++) v[i] = j.getAsJsonArray(key).get(i).getAsFloat();
        return v;
    }
    private static ModelPart root(JsonObject data, float y) {
        Map<String, ModelPart> children = new LinkedHashMap<>();
        var res = data.getAsJsonObject("resolution");
        for (var e : data.getAsJsonArray("parts")) {
            var n = e.getAsJsonObject();
            children.put(n.get("uuid").getAsString(), part(n, new float[3], res, y));
        }
        return new ModelPart(List.of(), children);
    }
    private static ModelPart part(JsonObject n, float[] parent, JsonObject res, float rootY) {
        float[] origin = vec(n, "origin"), rotation = vec(n, "rotation");
        Map<String, ModelPart> children = new LinkedHashMap<>();
        List<ModelPart.Cube> cubes = new ArrayList<>();
        if (n.has("children")) {
            for (var e : n.getAsJsonArray("children")) {
                var c = e.getAsJsonObject();
                children.put(c.get("uuid").getAsString(), part(c, origin, res, 0));
            }
        } else cubes.add(cube(n, origin, res));
        ModelPart part = new ModelPart(cubes, children);
        var pose = PartPose.offsetAndRotation(parent[0] - origin[0], parent[1] - origin[1] + rootY,
                origin[2] - parent[2], (float)Math.toRadians(-rotation[0]),
                (float)Math.toRadians(-rotation[1]), (float)Math.toRadians(rotation[2]));
        part.setInitialPose(pose); part.loadPose(pose);
        return part;
    }
    private static ModelPart.Cube cube(JsonObject n, float[] o, JsonObject res) {
        float[] a = vec(n,"from"), b = vec(n,"to");
        float grow = n.has("inflate") ? n.get("inflate").getAsFloat() : 0;
        var faces = n.getAsJsonObject("faces");
        Set<Direction> visible = EnumSet.noneOf(Direction.class);
        for (Direction d : Direction.values()) {
            String face = originalFace(d);
            if (faces.has(face) && !faces.getAsJsonObject(face).get("texture").isJsonNull()) visible.add(d);
        }
        var cube = new ModelPart.Cube(0,0,o[0]-b[0],o[1]-b[1],a[2]-o[2],b[0]-a[0],b[1]-a[1],b[2]-a[2],
                grow,grow,grow,false,64,64,visible);
        int i = 0;
        for (Direction d : new Direction[]{Direction.DOWN,Direction.UP,Direction.WEST,Direction.NORTH,Direction.EAST,Direction.SOUTH}) {
            if (!visible.contains(d)) continue;
            String face = originalFace(d);
            var f = faces.getAsJsonObject(face);
            var uv = f.getAsJsonArray("uv");
            var polygon = cube.polygons[i];
            var vertices = polygon.vertices();
            for (int v = 0; v < vertices.length; v++) {
                var vertex = vertices[v];
                float x = ratio(o[0]-vertex.x(),a[0]-grow,b[0]+grow);
                float y = ratio(o[1]-vertex.y(),a[1]-grow,b[1]+grow);
                float z = ratio(vertex.z()+o[2],a[2]-grow,b[2]+grow);
                float u = switch(face) { case "north" -> 1-x; case "east" -> 1-z; case "west" -> z; default -> x; };
                float vv = switch(face) { case "up" -> z; case "down" -> 1-z; default -> 1-y; };
                int turns = f.has("rotation") ? Math.floorMod(f.get("rotation").getAsInt()/90,4) : 0;
                for (int r=0;r<turns;r++) { float old=u;u=vv;vv=1-old; }
                vertices[v] = vertex.remap((uv.get(0).getAsFloat() + u*(uv.get(2).getAsFloat()-uv.get(0).getAsFloat()))/res.get("width").getAsFloat(),
                        (uv.get(1).getAsFloat() + vv*(uv.get(3).getAsFloat()-uv.get(1).getAsFloat()))/res.get("height").getAsFloat());
            }
            i++;
        }
        return cube;
    }
    private static float ratio(float v,float a,float b) { return b==a?0:(v-a)/(b-a); }
    private static String originalFace(Direction d) {
        return switch(d) { case UP -> "down";case DOWN -> "up";case EAST -> "west";case WEST -> "east";default -> d.getName(); };
    }
    private void animate(String name,float time,float weight) {
        var animation=animations.get(name);
        if(animation!=null && weight>0) animation.apply((long)(time*50),weight);
    }
    @Override public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        if(state instanceof com.mmorpg.client.model.mount.MountRenderer.State m && m.flying && animations.containsKey("fly")) {
            animate("fly",state.ageInTicks,1);
            return;
        }
        float moving = Math.min(1,state.walkAnimationSpeed*3);
        animate("idle",state.ageInTicks,1-moving);
        String move=animations.containsKey("walk")?"walk":animations.containsKey("fly")?"fly":"slither";
        animate(move,state.ageInTicks,moving);
        if(state instanceof ImportedMobRenderer.State s && s.attackTicks>=0) animate("attack",s.attackTicks,1);
    }
}
