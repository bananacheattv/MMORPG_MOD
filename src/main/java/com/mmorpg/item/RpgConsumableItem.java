package com.mmorpg.item;

import com.mmorpg.MMORPG;
import com.mmorpg.rpg.LevelSystem;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.rpg.Rarity;
import com.mmorpg.server.RpgPlayers;
import com.mmorpg.server.TeleportManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Consommables : potions de soin/mana, elixir, parchemins, orbe de renaissance, coffre cosmetique. */
public class RpgConsumableItem extends RpgMaterialItem {
    public static final Identifier POTION_GROUP = MMORPG.id("potions");

    public enum Kind {
        HEAL, MANA, ELIXIR, TELEPORT_SCROLL, FORGET_SCROLL, REBIRTH_ORB, COSMETIC_CHEST
    }

    private final Kind kind;
    private final double amount;

    public RpgConsumableItem(Properties properties, Rarity rarity, Kind kind, double amount, String lore) {
        super(properties, rarity, "Consommable", lore);
        this.kind = kind;
        this.amount = amount;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }
        PlayerData data = RpgPlayers.get(sp);
        ServerLevel sl = sp.level();
        boolean consumed = true;
        switch (kind) {
            case HEAL -> {
                if (sp.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
                if (data.hp >= data.stats.get(com.mmorpg.rpg.Stat.MAX_HP)) return InteractionResult.FAIL;
                RpgPlayers.heal(sp, amount, true);
                sp.getCooldowns().addCooldown(POTION_GROUP, 160);
                sl.playSound(null, sp.blockPosition(), SoundEvents.GENERIC_DRINK.value(), SoundSource.PLAYERS, 1, 1);
                sl.sendParticles(ParticleTypes.HEART, sp.getX(), sp.getY() + 1.2, sp.getZ(), 6, 0.4, 0.4, 0.4, 0.02);
            }
            case MANA -> {
                if (sp.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
                if (data.mana >= data.stats.get(com.mmorpg.rpg.Stat.MAX_MANA)) return InteractionResult.FAIL;
                RpgPlayers.restoreMana(sp, amount);
                sp.getCooldowns().addCooldown(POTION_GROUP, 160);
                sl.playSound(null, sp.blockPosition(), SoundEvents.GENERIC_DRINK.value(), SoundSource.PLAYERS, 1, 1.2f);
                sl.sendParticles(ParticleTypes.ENCHANT, sp.getX(), sp.getY() + 1.2, sp.getZ(), 20, 0.4, 0.5, 0.4, 0.5);
            }
            case ELIXIR -> {
                if (data.level >= LevelSystem.MAX_LEVEL) {
                    sp.sendSystemMessage(Component.literal("Vous avez déjà atteint le niveau maximum.").withColor(0xFF6060));
                    return InteractionResult.FAIL;
                }
                long xp = Math.max(50, Math.round(LevelSystem.xpToNext(data.level) * amount));
                RpgPlayers.giveXp(sp, xp, false);
                sl.playSound(null, sp.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.4f);
            }
            case TELEPORT_SCROLL -> {
                TeleportManager.openFor(sp, null);
                consumed = false; // consomme seulement si le voyage a lieu
            }
            case FORGET_SCROLL -> {
                data.resetAttributes();
                RpgPlayers.recomputeAndSync(sp);
                sp.sendSystemMessage(Component.literal("Vos points d'attribut ont été réinitialisés (" + data.attributePoints + " points disponibles).").withColor(0xC080FF));
                sl.playSound(null, sp.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1, 0.8f);
            }
            case REBIRTH_ORB -> {
                data.resetClass();
                RpgPlayers.recomputeAndSync(sp);
                RpgPlayers.openClassSelection(sp);
                sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, sp.getX(), sp.getY() + 1, sp.getZ(), 60, 0.5, 1, 0.5, 0.3);
                sl.playSound(null, sp.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8f, 1);
            }
            case COSMETIC_CHEST -> {
                RpgPlayers.unlockRandomCosmetic(sp);
                sl.playSound(null, sp.blockPosition(), SoundEvents.CHEST_OPEN, SoundSource.PLAYERS, 1, 1);
                sl.sendParticles(ParticleTypes.FIREWORK, sp.getX(), sp.getY() + 1, sp.getZ(), 30, 0.4, 0.4, 0.4, 0.15);
            }
        }
        if (consumed && !sp.hasInfiniteMaterials()) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
