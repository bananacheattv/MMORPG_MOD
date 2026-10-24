package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.entity.MountEntity;
import com.mmorpg.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;

/**
 * Camera a la troisieme personne en selle : les montures sont affichees deux fois plus grandes que leur modele de base, la camera
 * (4 blocs derriere le joueur par defaut) finissait contre leur dos. On l'eloigne selon la taille de la monture pour la voir en entier.
 */
@EventBusSubscriber(modid = MMORPG.MODID, value = Dist.CLIENT)
public final class MountCameraClient {
    private MountCameraClient() {
    }

    @SubscribeEvent
    public static void onDistance(CalculateDetachedCameraDistanceEvent event) {
        var player = Minecraft.getInstance().player;
        if (player == null || !(player.getVehicle() instanceof MountEntity mount)) return;
        float size = Math.max(Math.max(mount.mountType().width, mount.mountType().height), mount.mountType().seat) * ModEntities.MOUNT_SCALE;
        event.setDistance(Math.max(event.getDistance(), event.getDistance() + size * 1.5F));
    }
}
