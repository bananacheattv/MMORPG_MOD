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
    public static final Supplier<AttachmentType<Integer>> MINE_DENSITY = ATTACHMENTS.register("mine_density",
            () -> AttachmentType.builder(() -> 0).serialize(com.mojang.serialization.Codec.INT.fieldOf("version")).build());

    public static final Supplier<AttachmentType<String>> STAND_LOOK=ATTACHMENTS.register("stand_appearance",()->AttachmentType.builder(()->"").serialize(com.mojang.serialization.Codec.STRING.fieldOf("appearance")).sync(net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8).build());

    /** Donnees RPG du joueur : persistantes et conservees a la mort. */
    public static final Supplier<AttachmentType<PlayerData>> PLAYER = ATTACHMENTS.register("player_data",
            () -> AttachmentType.serializable(PlayerData::new).copyOnDeath().build());

    /** Donnees publiques du joueur synchronisees avec les joueurs proches (cosmetiques, classe, niveau). */
    public static final Supplier<AttachmentType<PublicPlayerData>> PUBLIC = ATTACHMENTS.register("public_data",
            () -> AttachmentType.builder(() -> new PublicPlayerData()).sync(PublicPlayerData.STREAM_CODEC).build());

    /** Donnees RPG des monstres (niveau, PV virtuels...), synchronisees pour l'affichage. */
    public static final Supplier<AttachmentType<MobData>> MOB = ATTACHMENTS.register("mob_data",
            () -> AttachmentType.serializable(MobData::new).sync(MobData.STREAM_CODEC).build());

    /** Donnees du hub : cinematique vue, classes essayees, sauvegarde du personnage pendant un essai. */
    public static final Supplier<AttachmentType<com.mmorpg.hub.HubPlayerData>> HUB = ATTACHMENTS.register("hub_data",
            () -> AttachmentType.serializable(com.mmorpg.hub.HubPlayerData::new).copyOnDeath().build());

    private ModAttachments() {
    }
}
