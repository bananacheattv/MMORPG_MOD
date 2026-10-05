package com.mmorpg.mount;

import net.minecraft.world.entity.animal.equine.Variant;

public enum MountType {
    VOYAGEUR("voyageur", "Destrier du Voyageur", 10, 250, .24, Variant.BROWN),
    AUBE("aube", "Courser de l’Aube", 40, 1500, .29, Variant.WHITE),
    OMBRE("ombre", "Étalon de l’Ombre", 75, 6000, .34, Variant.BLACK);
    public final String id, label;
    public final int level, price;
    public final double speed;
    public final Variant variant;
    MountType(String id, String label, int level, int price, double speed, Variant variant) {
        this.id=id; this.label=label; this.level=level; this.price=price; this.speed=speed; this.variant=variant;
    }
    public static MountType byId(String id) {
        for (var t : values()) if (t.id.equals(id)) return t;
        return null;
    }
}
