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
    /** Edition des butins (caisses, Lucky Blocks) par un operateur. */
    public record LootEdit(CompoundTag data) implements CustomPacketPayload {
        public static final Type<LootEdit> TYPE=new Type<>(MMORPG.id("loot_edit"));
        public static final StreamCodec<ByteBuf,LootEdit> CODEC=StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG,LootEdit::data,LootEdit::new);
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

    /** Animation d'entree dans une zone (style 0) ou une salle de boss (style 1). */
    public record AreaEntry(String title, String subtitle, int style, int color, String lore) implements CustomPacketPayload {
        /** Styles : 0 zone, 1 salle de boss, 2 vague, 3 phase de boss, 4 victoire, 5 entree de donjon. */
        public static final int ZONE = 0, BOSS_ROOM = 1, WAVE = 2, PHASE = 3, VICTORY = 4, DUNGEON = 5;

        public static final Type<AreaEntry> TYPE = new Type<>(MMORPG.id("area_entry"));
        public static final StreamCodec<ByteBuf, AreaEntry> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, AreaEntry::title, ByteBufCodecs.STRING_UTF8, AreaEntry::subtitle,
                ByteBufCodecs.VAR_INT, AreaEntry::style, ByteBufCodecs.INT, AreaEntry::color, ByteBufCodecs.STRING_UTF8, AreaEntry::lore, AreaEntry::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Mort du joueur : cause et experience perdue, affichees par l'ecran de mort personnalise (l'equipement n'est jamais perdu). */
    public record DeathInfo(long xpLost, int level, long xpNow, long xpNext, String cause, String lostLoot) implements CustomPacketPayload {
        public static final Type<DeathInfo> TYPE = new Type<>(MMORPG.id("death_info"));
        public static final StreamCodec<ByteBuf, DeathInfo> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, DeathInfo::xpLost, ByteBufCodecs.VAR_INT, DeathInfo::level, ByteBufCodecs.VAR_LONG, DeathInfo::xpNow,
                ByteBufCodecs.VAR_LONG, DeathInfo::xpNext, ByteBufCodecs.STRING_UTF8, DeathInfo::cause, ByteBufCodecs.STRING_UTF8, DeathInfo::lostLoot, DeathInfo::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Musique d'ambiance d'un donjon : nom du donjon, « boss » dans l'antre, ou vide hors des donjons. */
    public record DungeonMusic(String track) implements CustomPacketPayload {
        public static final Type<DungeonMusic> TYPE = new Type<>(MMORPG.id("dungeon_music"));
        public static final StreamCodec<ByteBuf, DungeonMusic> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, DungeonMusic::track, DungeonMusic::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Cinematique d'apparition d'un boss invoque a l'autel (camera pilotee cote client). */
    public record BossReveal(int x, int y, int z, int ticks) implements CustomPacketPayload {
        public static final Type<BossReveal> TYPE = new Type<>(MMORPG.id("boss_reveal"));
        public static final StreamCodec<ByteBuf, BossReveal> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, BossReveal::x, ByteBufCodecs.VAR_INT, BossReveal::y, ByteBufCodecs.VAR_INT, BossReveal::z,
                ByteBufCodecs.VAR_INT, BossReveal::ticks, BossReveal::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Ecran de chargement d'un donjon instancie : 0 = debut, 1 = progression, 2 = fin. */
    public record Loading(int mode, String title, String lore, int color, int percent) implements CustomPacketPayload {
        public static final int START = 0, PROGRESS = 1, END = 2;
        public static final Type<Loading> TYPE = new Type<>(MMORPG.id("loading"));
        public static final StreamCodec<ByteBuf, Loading> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Loading::mode, ByteBufCodecs.STRING_UTF8, Loading::title, ByteBufCodecs.STRING_UTF8, Loading::lore,
                ByteBufCodecs.INT, Loading::color, ByteBufCodecs.VAR_INT, Loading::percent, Loading::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Choix du joueur chez le Maitre des donjons : donjon et difficulte (0 normal, 1 heroique, 2 mythique). */
    /** Choix d'un etage dans le menu des mines (-1 pour le PNJ si le menu vient de la commande /mine). */
    public record MineEnter(int npc, int sector) implements CustomPacketPayload {
        public static final Type<MineEnter> TYPE = new Type<>(MMORPG.id("mine_enter"));
        public static final StreamCodec<ByteBuf, MineEnter> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, MineEnter::npc, ByteBufCodecs.VAR_INT, MineEnter::sector, MineEnter::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DungeonEnter(int npc, String dungeon, int difficulty) implements CustomPacketPayload {
        public static final Type<DungeonEnter> TYPE = new Type<>(MMORPG.id("dungeon_enter"));
        public static final StreamCodec<ByteBuf, DungeonEnter> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, DungeonEnter::npc, ByteBufCodecs.STRING_UTF8, DungeonEnter::dungeon,
                ByteBufCodecs.VAR_INT, DungeonEnter::difficulty, DungeonEnter::new);

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

    /** Action du hub : essayer / choisir une classe, quitter l'essai, revoir la cinematique ou la terminer. */
    public record HubAction(int action, int a, int b) implements CustomPacketPayload {
        public static final int TRY = 0, CHOOSE = 1, QUIT = 2, REPLAY = 3, CINEMATIC_END = 4, CINEMATIC_SKIP = 5, GUIDE_COMPLETE = 6;
        public static final Type<HubAction> TYPE = new Type<>(MMORPG.id("hub_action"));
        public static final StreamCodec<ByteBuf, HubAction> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, HubAction::action, ByteBufCodecs.VAR_INT, HubAction::a, ByteBufCodecs.VAR_INT, HubAction::b, HubAction::new);

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
        public static final int UNLOCK = 0, SUMMON = 1, DISMISS = 2, TOGGLE = 3;
        public static final Type<MountAction> TYPE = new Type<>(MMORPG.id("mount_action"));
        public static final StreamCodec<ByteBuf, MountAction> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, MountAction::action, ByteBufCodecs.STRING_UTF8, MountAction::mount, MountAction::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record LuckyRoll(BlockPos pos) implements CustomPacketPayload {
        public static final Type<LuckyRoll> TYPE = new Type<>(MMORPG.id("lucky_roll"));
        public static final StreamCodec<ByteBuf, LuckyRoll> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, LuckyRoll::pos, LuckyRoll::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Ouvrir la caisse (bloc) a cette position avec une cle. */
    public record CrateOpen(BlockPos pos) implements CustomPacketPayload {
        public static final Type<CrateOpen> TYPE = new Type<>(MMORPG.id("crate_open"));
        public static final StreamCodec<ByteBuf, CrateOpen> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, CrateOpen::pos, CrateOpen::new);
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
    /** Menu d'administration, client -> serveur : lecture d'un fichier (GET), envoi d'un fichier par morceaux (SAVE), deconnexion (LOGOUT). */
    public record AdminRequest(int action, String file, int index, int count, String text) implements CustomPacketPayload {
        public static final int GET = 0, SAVE = 1, LOGOUT = 2;
        public static final Type<AdminRequest> TYPE = new Type<>(MMORPG.id("admin_request"));
        public static final StreamCodec<ByteBuf, AdminRequest> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, AdminRequest::action, ByteBufCodecs.STRING_UTF8, AdminRequest::file,
                ByteBufCodecs.VAR_INT, AdminRequest::index, ByteBufCodecs.VAR_INT, AdminRequest::count,
                ByteBufCodecs.STRING_UTF8, AdminRequest::text, AdminRequest::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Menu d'administration, serveur -> client : ouverture (liste des fichiers), morceau d'un fichier, message de reussite ou d'erreur. */
    public record AdminReply(int kind, String file, int index, int count, String text) implements CustomPacketPayload {
        public static final int OPEN = 0, FILE = 1, OK = 2, ERROR = 3;
        public static final Type<AdminReply> TYPE = new Type<>(MMORPG.id("admin_reply"));
        public static final StreamCodec<ByteBuf, AdminReply> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, AdminReply::kind, ByteBufCodecs.STRING_UTF8, AdminReply::file,
                ByteBufCodecs.VAR_INT, AdminReply::index, ByteBufCodecs.VAR_INT, AdminReply::count,
                ByteBufCodecs.STRING_UTF8, AdminReply::text, AdminReply::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Action d'un service de la ville : banque (service 0) ou hotel des ventes (service 1). */
    public record ServiceAction(int service, int action, long amount, String text, int page, BlockPos pos) implements CustomPacketPayload {
        public static final int BANK = 0, AUCTION = 1;
        public static final Type<ServiceAction> TYPE = new Type<>(MMORPG.id("service_action"));
        public static final StreamCodec<ByteBuf, ServiceAction> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, ServiceAction::service, ByteBufCodecs.VAR_INT, ServiceAction::action,
                ByteBufCodecs.VAR_LONG, ServiceAction::amount, ByteBufCodecs.STRING_UTF8, ServiceAction::text,
                ByteBufCodecs.VAR_INT, ServiceAction::page, BlockPos.STREAM_CODEC, ServiceAction::pos, ServiceAction::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ForgeAction(int action, String recipe, int value, BlockPos pos) implements CustomPacketPayload {
        public static final int CRAFT = 0, UPGRADE = 1, COMPRESS = 2;
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
    public record SocialAction(String system,CompoundTag data) implements CustomPacketPayload {
        public static final Type<SocialAction> TYPE=new Type<>(MMORPG.id("social_action"));
        public static final StreamCodec<ByteBuf,SocialAction> CODEC=StreamCodec.composite(ByteBufCodecs.STRING_UTF8,SocialAction::system,ByteBufCodecs.COMPOUND_TAG,SocialAction::data,SocialAction::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record MerchantEdit(CompoundTag data) implements CustomPacketPayload {
        public static final Type<MerchantEdit> TYPE=new Type<>(MMORPG.id("merchant_edit"));
        public static final StreamCodec<ByteBuf,MerchantEdit> CODEC=StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG,MerchantEdit::data,MerchantEdit::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
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
    public record Guidance(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Guidance> TYPE=new Type<>(MMORPG.id("guidance"));
        public static final StreamCodec<ByteBuf,Guidance> CODEC=StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG,Guidance::data,Guidance::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }

}
