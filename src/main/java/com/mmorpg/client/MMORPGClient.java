package com.mmorpg.client;

import com.mmorpg.MMORPG;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Point d'entree client : configuration d'affichage (les autres abonnements sont dans les classes @EventBusSubscriber). */
@Mod(value = MMORPG.MODID, dist = Dist.CLIENT)
public final class MMORPGClient {
    public MMORPGClient(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
