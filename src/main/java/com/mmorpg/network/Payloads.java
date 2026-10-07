package com.mmorpg.network;

import com.mmorpg.MMORPG;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Tous les paquets reseau du mod (hors ouverture d'ecran). */
public final class Payloads {
    public record QuestEdit(CompoundTag data) implements CustomPacketPayload {
        public static final Type<QuestEdit> TYPE=new Type<>(MMORPG.id("quest_edit"));
        public static final StreamCodec<ByteBuf,QuestEdit> CODEC=StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG,QuestEdit::data,QuestEdit::new);
        @Override public Type<? extends CustomPacketPayload> type() {return TYPE;}
    }
    private Payloads() {
    }

    // =====================================================================================  serveur -> client

    /** Synchronisation complete des donnees RPG privees du joueur. */
    public record SyncPlayer(CompoundTag data) implements CustomPacketPayload {
        public static final Type<SyncPlayer> TYPE = new Type<>(MMORPG.id("sync_player"));
        public static final StreamCodec<ByteBuf, SyncPlayer> CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, SyncPlayer::data, SyncPlayer::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Mise a jour legere des barres de vie / mana. */
    public record Vitals(float hp, float maxHp, float mana, float maxMana) implements CustomPacketPayload {
        public static final Type<Vitals> TYPE = new Type<>(MMORPG.id("vitals"));
        public static final StreamCodec<ByteBuf, Vitals> CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, Vitals::hp, ByteBufCodecs.FLOAT, Vitals::maxHp,
                ByteBufCodecs.FLOAT, Vitals::mana, ByteBufCodecs.FLOAT, Vitals::maxMana, Vitals::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Texte de combat flottant (degats, critiques, soins, esquive). */
    public record CombatText(double x, double y, double z, float amount, int kind) implements CustomPacketPayload {
        public static final int DAMAGE = 0, CRIT = 1, TAKEN = 2, HEAL = 3, DODGE = 4, MANA = 5, XP = 6, IMMUNE = 7, GOLD = 8;
        public static final Type<CombatText> TYPE = new Type<>(MMORPG.id("combat_text"));
        public static final StreamCodec<ByteBuf, CombatText> CODEC = StreamCodec.composite(
                ByteBufCodecs.DOUBLE, CombatText::x, ByteBufCodecs.DOUBLE, CombatText::y, ByteBufCodecs.DOUBLE, CombatText::z,
                ByteBufCodecs.FLOAT, CombatText::amount, ByteBufCodecs.VAR_INT, CombatText::kind, CombatText::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Animation de lancement d'une competence jouee par un joueur (vue par tous ceux qui l'entourent). */
    public record CastAnimation(int entityId, int anim, int duration) implements CustomPacketPayload {
        public static final Type<CastAnimation> TYPE = new Type<>(MMORPG.id("cast_animation"));
        public static final StreamCodec<ByteBuf, CastAnimation> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, CastAnimation::entityId, ByteBufCodecs.VAR_INT, CastAnimation::anim,
                ByteBufCodecs.VAR_INT, CastAnimation::duration, CastAnimation::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Banniere d'annonce (montee de niveau, evolution, decouverte...). */
    public record Notify(int kind, String title, String subtitle, int color) implements CustomPacketPayload {
        public static final int LEVEL_UP = 0, EVOLUTION = 1, INFO = 2, DISCOVERY = 3, BOSS = 4, LOOT = 5, QUEST = 6;
        public static final Type<Notify> TYPE = new Type<>(MMORPG.id("notify"));
        public static final StreamCodec<ByteBuf, Notify> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Notify::kind, ByteBufCodecs.STRING_UTF8, Notify::title,
                ByteBufCodecs.STRING_UTF8, Notify::subtitle, ByteBufCodecs.INT, Notify::color, Notify::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Informations du bestiaire (noms, niveaux, butin) issues de la configuration serveur. */
    public record MobInfo(CompoundTag data) implements CustomPacketPayload {
        public static final Type<MobInfo> TYPE = new Type<>(MMORPG.id("mob_info"));
        public static final StreamCodec<ByteBuf, MobInfo> CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, MobInfo::data, MobInfo::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // =====================================================================================  client -> serveur

    public record SelectClass(int playerClass) implements CustomPacketPayload {
        public static final Type<SelectClass> TYPE = new Type<>(MMORPG.id("select_class"));
        public static final StreamCodec<ByteBuf, SelectClass> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, SelectClass::playerClass, SelectClass::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Allocate(int attribute, int amount) implements CustomPacketPayload {
        public static final Type<Allocate> TYPE = new Type<>(MMORPG.id("allocate"));
        public static final StreamCodec<ByteBuf, Allocate> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Allocate::attribute, ByteBufCodecs.VAR_INT, Allocate::amount, Allocate::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Action sur une competence : 0 = ameliorer, 1 = placer dans la barre, 2 = lancer. */
    public record SkillAction(int action, int slot, String skill) implements CustomPacketPayload {
        public static final int UPGRADE = 0, ASSIGN = 1, CAST = 2;
        public static final Type<SkillAction> TYPE = new Type<>(MMORPG.id("skill_action"));
        public static final StreamCodec<ByteBuf, SkillAction> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SkillAction::action, ByteBufCodecs.VAR_INT, SkillAction::slot,
                ByteBufCodecs.STRING_UTF8, SkillAction::skill, SkillAction::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record MountAction(int action, String mount) implements CustomPacketPayload {
        public static final int UNLOCK = 0, SUMMON = 1, DISMISS = 2;
        public static final Type<MountAction> TYPE = new Type<>(MMORPG.id("mount_action"));
        public static final StreamCodec<ByteBuf, MountAction> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, MountAction::action, ByteBufCodecs.STRING_UTF8, MountAction::mount, MountAction::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record LuckyRoll(BlockPos pos) implements CustomPacketPayload {
        public static final Type<LuckyRoll> TYPE = new Type<>(MMORPG.id("lucky_roll"));
        public static final StreamCodec<ByteBuf, LuckyRoll> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, LuckyRoll::pos, LuckyRoll::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Invoquer (id) ou renvoyer (chaine vide) un familier. */
    public record PetAction(String pet) implements CustomPacketPayload {
        public static final Type<PetAction> TYPE = new Type<>(MMORPG.id("pet_action"));
        public static final StreamCodec<ByteBuf, PetAction> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, PetAction::pet, PetAction::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Equiper (id) ou retirer (chaine vide) un cosmetique d'une categorie. */
    public record CosmeticAction(int category, String cosmetic) implements CustomPacketPayload {
        public static final Type<CosmeticAction> TYPE = new Type<>(MMORPG.id("cosmetic_action"));
        public static final StreamCodec<ByteBuf, CosmeticAction> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, CosmeticAction::category, ByteBufCodecs.STRING_UTF8, CosmeticAction::cosmetic, CosmeticAction::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Teleport(String waypoint) implements CustomPacketPayload {
        public static final Type<Teleport> TYPE = new Type<>(MMORPG.id("teleport"));
        public static final StreamCodec<ByteBuf, Teleport> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Teleport::waypoint, Teleport::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record TeleporterSetup(BlockPos pos, String name, int category, int minLevel) implements CustomPacketPayload {
        public static final Type<TeleporterSetup> TYPE = new Type<>(MMORPG.id("teleporter_setup"));
        public static final StreamCodec<ByteBuf, TeleporterSetup> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, TeleporterSetup::pos, ByteBufCodecs.STRING_UTF8, TeleporterSetup::name,
                ByteBufCodecs.VAR_INT, TeleporterSetup::category, ByteBufCodecs.VAR_INT, TeleporterSetup::minLevel, TeleporterSetup::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Action a la forge : 0 = fabriquer (recette, quantite), 1 = ameliorer (emplacement d'inventaire). */
    public record ForgeAction(int action, String recipe, int value, BlockPos pos) implements CustomPacketPayload {
        public static final int CRAFT = 0, UPGRADE = 1;
        public static final Type<ForgeAction> TYPE = new Type<>(MMORPG.id("forge_action"));
        public static final StreamCodec<ByteBuf, ForgeAction> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ForgeAction::action, ByteBufCodecs.STRING_UTF8, ForgeAction::recipe,
                ByteBufCodecs.VAR_INT, ForgeAction::value, BlockPos.STREAM_CODEC, ForgeAction::pos, ForgeAction::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // =====================================================================================  ajouts : quetes, boutique, groupe

    /** Etat du groupe du joueur (membres, chef, invitation en attente). */
    public record PartySync(CompoundTag data) implements CustomPacketPayload {
        public static final Type<PartySync> TYPE = new Type<>(MMORPG.id("party_sync"));
        public static final StreamCodec<ByteBuf, PartySync> CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, PartySync::data, PartySync::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Action de quete : 0 = accepter, 1 = rendre, 2 = abandonner. */
    public record QuestAction(int action, String quest, int npc) implements CustomPacketPayload {
        public static final int ACCEPT = 0, COMPLETE = 1, ABANDON = 2, TRACK = 3;
        public static final Type<QuestAction> TYPE = new Type<>(MMORPG.id("quest_action"));
        public static final StreamCodec<ByteBuf, QuestAction> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, QuestAction::action, ByteBufCodecs.STRING_UTF8, QuestAction::quest,
                ByteBufCodecs.VAR_INT, QuestAction::npc, QuestAction::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Action de boutique : 0 = acheter (index d'offre, quantite), 1 = vendre (emplacement, quantite), 2 = tout vendre (emplacement). */
    public record ShopAction(int action, int index, int count, int npc) implements CustomPacketPayload {
        public static final int BUY = 0, SELL = 1, SELL_ALL = 2;
        public static final Type<ShopAction> TYPE = new Type<>(MMORPG.id("shop_action"));
        public static final StreamCodec<ByteBuf, ShopAction> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ShopAction::action, ByteBufCodecs.VAR_INT, ShopAction::index,
                ByteBufCodecs.VAR_INT, ShopAction::count, ByteBufCodecs.VAR_INT, ShopAction::npc, ShopAction::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Action de groupe depuis l'interface : 0 inviter, 1 accepter, 2 refuser, 3 quitter, 4 exclure, 5 nommer chef. */
    public record PartyAction(int action, String name) implements CustomPacketPayload {
        public static final int INVITE = 0, ACCEPT = 1, DECLINE = 2, LEAVE = 3, KICK = 4, PROMOTE = 5;
        public static final Type<PartyAction> TYPE = new Type<>(MMORPG.id("party_action"));
        public static final StreamCodec<ByteBuf, PartyAction> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, PartyAction::action, ByteBufCodecs.STRING_UTF8, PartyAction::name, PartyAction::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
