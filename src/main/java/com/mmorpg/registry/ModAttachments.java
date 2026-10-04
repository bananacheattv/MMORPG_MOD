package com.mmorpg.registry;

import com.mmorpg.MMORPG;
import com.mmorpg.rpg.MobData;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.rpg.PublicPlayerData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MMORPG.MODID);

    /** Donnees RPG du joueur : persistantes et conservees a la mort. */
    public static final Supplier<AttachmentType<PlayerData>> PLAYER = ATTACHMENTS.register("player_data",
            () -> AttachmentType.serializable(PlayerData::new).copyOnDeath().build());

    /** Donnees publiques du joueur synchronisees avec les joueurs proches (cosmetiques, classe, niveau). */
    public static final Supplier<AttachmentType<PublicPlayerData>> PUBLIC = ATTACHMENTS.register("public_data",
            () -> AttachmentType.builder(() -> new PublicPlayerData()).sync(PublicPlayerData.STREAM_CODEC).build());

    /** Donnees RPG des monstres (niveau, PV virtuels...), synchronisees pour l'affichage. */
    public static final Supplier<AttachmentType<MobData>> MOB = ATTACHMENTS.register("mob_data",
            () -> AttachmentType.serializable(MobData::new).sync(MobData.STREAM_CODEC).build());

    private ModAttachments() {
    }
}
