package com.mmorpg.server;

import com.mmorpg.network.Payloads;
import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.skill.SkillExecutor;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Gestion des paquets recus des clients (toujours valides cote serveur). */
public final class ServerHandlers {
    private ServerHandlers() {
    }

    public static void selectClass(Payloads.SelectClass p, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer sp) {
            RpgPlayers.selectClass(sp, PlayerClass.byId(p.playerClass()));
        }
    }

    public static void allocate(Payloads.Allocate p, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer sp) {
            RpgPlayers.allocate(sp, p.attribute(), p.amount());
        }
    }

    public static void skillAction(Payloads.SkillAction p, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer sp)) return;
        switch (p.action()) {
            case Payloads.SkillAction.UPGRADE -> RpgPlayers.upgradeSkill(sp, p.skill());
            case Payloads.SkillAction.ASSIGN -> RpgPlayers.assignSkill(sp, p.slot(), p.skill());
            case Payloads.SkillAction.CAST -> SkillExecutor.castSlot(sp, p.slot());
            default -> {
            }
        }
    }

    public static void petAction(Payloads.PetAction p, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer sp) {
            PetManager.setActive(sp, p.pet());
        }
    }

    public static void cosmeticAction(Payloads.CosmeticAction p, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer sp) {
            RpgPlayers.equipCosmetic(sp, p.category(), p.cosmetic());
        }
    }

    public static void teleport(Payloads.Teleport p, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer sp) {
            TeleportManager.travel(sp, p.waypoint());
        }
    }

    public static void teleporterSetup(Payloads.TeleporterSetup p, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer sp) {
            TeleportManager.configure(sp, p.pos(), p.name(), p.category(), p.minLevel());
        }
    }

    public static void questAction(Payloads.QuestAction p, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer sp)) return;
        switch (p.action()) {
            case Payloads.QuestAction.ACCEPT -> QuestManager.accept(sp, p.quest(), p.npc());
            case Payloads.QuestAction.COMPLETE -> QuestManager.complete(sp, p.quest(), p.npc());
            case Payloads.QuestAction.ABANDON -> QuestManager.abandon(sp, p.quest());
            case Payloads.QuestAction.TRACK -> QuestManager.track(sp, p.quest());
            default -> {
            }
        }
    }

    public static void shopAction(Payloads.ShopAction p, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer sp)) return;
        switch (p.action()) {
            case Payloads.ShopAction.BUY -> ShopManager.buy(sp, p.npc(), p.index(), p.count());
            case Payloads.ShopAction.SELL -> ShopManager.sell(sp, p.npc(), p.index(), p.count(), false);
            case Payloads.ShopAction.SELL_ALL -> ShopManager.sell(sp, p.npc(), p.index(), 0, true);
            default -> {
            }
        }
    }

    public static void partyAction(Payloads.PartyAction p, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer sp)) return;
        switch (p.action()) {
            case Payloads.PartyAction.INVITE -> PartyManager.invite(sp, p.name());
            case Payloads.PartyAction.ACCEPT -> PartyManager.accept(sp);
            case Payloads.PartyAction.DECLINE -> PartyManager.decline(sp);
            case Payloads.PartyAction.LEAVE -> PartyManager.leave(sp);
            case Payloads.PartyAction.KICK -> PartyManager.kick(sp, p.name());
            case Payloads.PartyAction.PROMOTE -> PartyManager.promote(sp, p.name());
            default -> {
            }
        }
    }

    public static void forgeAction(Payloads.ForgeAction p, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer sp)) return;
        if (p.action() == Payloads.ForgeAction.CRAFT) {
            ForgeManager.craft(sp, p.pos(), p.recipe(), p.value());
        } else if (p.action() == Payloads.ForgeAction.UPGRADE) {
            ForgeManager.upgrade(sp, p.pos(), p.value(), p.recipe());
        }
    }
}
