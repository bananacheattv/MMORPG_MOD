package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.combat.RpgCombat;
import com.mmorpg.config.ConfigManager;
import com.mmorpg.config.GeneralConfig;
import com.mmorpg.config.MobConfig;
import com.mmorpg.crafting.ForgeRecipes;
import com.mmorpg.entity.MobAbilities;
import com.mmorpg.registry.ModAttachments;
import com.mmorpg.registry.ModItems;
import com.mmorpg.rpg.LevelSystem;
import com.mmorpg.rpg.MobData;
import com.mmorpg.rpg.PlayerData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.ArrayList;
import java.util.List;

/** Experience, butin et statistiques de chasse lors de la mort d'un monstre RPG. */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class KillRewards {
    private KillRewards() {
    }

    private static ServerPlayer killer(LivingEntity dead, Entity sourceEntity) {
        if (sourceEntity instanceof ServerPlayer sp) return sp;
        Player last = dead.getLastHurtByPlayer() instanceof Player p ? p : null;
        return last instanceof ServerPlayer sp ? sp : null;
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level)) return;
        if (dead instanceof ServerPlayer sp) {
            RpgPlayers.onDeath(sp);
            return;
        }
        MobData md = dead.getExistingDataOrNull(ModAttachments.MOB);
        if (md == null || !md.initialized()) return;
        ServerPlayer killer = killer(dead, event.getSource().getEntity());
        if (killer == null) return;

        // beneficiaires : le tueur et les membres de son groupe a portee ; pour un boss, tous les joueurs proches
        List<ServerPlayer> receivers = new ArrayList<>();
        double range = 48;
        for (ServerPlayer p : PartyManager.onlineMembers(killer)) {
            if (p.level() == level && p.distanceToSqr(dead) <= range * range && RpgPlayers.get(p).playerClass.isPlayable()) receivers.add(p);
        }
        if (md.boss) {
            for (ServerPlayer p : level.players()) {
                if (!receivers.contains(p) && p.distanceToSqr(dead) <= range * range && RpgPlayers.get(p).playerClass.isPlayable()) receivers.add(p);
            }
        }
        if (!receivers.contains(killer)) receivers.add(killer);
        int groupSize = Math.max(1, (int) receivers.stream().filter(p -> p == killer || PartyManager.sameParty(killer, p)).count());
        // bonus de groupe : +10 % d'XP totale par membre supplementaire, repartie equitablement
        double groupFactor = groupSize > 1 ? (1.0 + 0.1 * (groupSize - 1)) / groupSize : 1.0;
        String typeId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType()).toString();
        for (ServerPlayer p : receivers) {
            PlayerData d = RpgPlayers.get(p);
            boolean inGroup = p == killer || PartyManager.sameParty(killer, p);
            double share = inGroup ? groupFactor : 1.0;
            long xp = Math.max(1, Math.round(LevelSystem.xpForKill(md.level, md.xpMultiplier, d.level) * share));
            RpgPlayers.giveXp(p, xp, true);
            if (!d.activePet.isEmpty()) PetManager.addPetXp(p, d.activePet, (int) Math.max(1, xp * 0.05));
            d.kills.merge(md.key, 1, Integer::sum);
            QuestManager.onKill(p, md.key, typeId, md.boss);
            d.dirty = true;
        }
        if (md.boss) {
            level.playSound(null, dead.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.HOSTILE, 2f, 1f);
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, dead.getX(), dead.getY() + 1, dead.getZ(), 120, 1, 1.5, 1, 0.5);
            if (ConfigManager.general().announceBossKills) {
                level.getServer().getPlayerList().broadcastSystemMessage(
                        Component.literal("☠ ").withColor(0xFF4040)
                                .append(dead.getDisplayName())
                                .append(Component.literal(" (niveau " + md.level + ") a été vaincu par ").withColor(0xE0D8C0))
                                .append(killer.getDisplayName())
                                .append(Component.literal(" !").withColor(0xE0D8C0)), false);
            }
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level) || dead instanceof Player) return;
        MobData md = dead.getExistingDataOrNull(ModAttachments.MOB);
        if (md == null || !md.initialized()) return;
        ServerPlayer lootOwner = killer(dead, event.getSource().getEntity());
        if (lootOwner == null) return;
        GeneralConfig g = ConfigManager.general();
        double dropMultiplier = g.dropMultiplier * (RpgPlayers.get(lootOwner).luckCharmSeconds > 0 ? 1.25 : 1.0);
        var random = dead.getRandom();
        List<ItemStack> loot = new ArrayList<>();
        MobConfig cfg = ConfigManager.mobs().get(md.key);
        if (cfg != null) {
            for (MobConfig.Drop d : cfg.drops) {
                double chance = Math.min(1.0, d.chance * dropMultiplier);
                if (random.nextDouble() < chance) {
                    Item item = ForgeRecipes.resolveItem(d.item);
                    if (item != null && item != net.minecraft.world.item.Items.AIR) {
                        int n = d.min >= d.max ? d.min : d.min + random.nextInt(d.max - d.min + 1);
                        loot.add(new ItemStack(item, Math.max(1, n)));
                    }
                }
            }
        }
        if (!md.boss) {
            if (random.nextDouble() < g.goldDropChance) {
                loot.add(new ItemStack(ModItems.PIECE_OR.get(), 1 + random.nextInt(1 + md.level / 8)));
            }
            // trefles d'amelioration : rares, plus puissants sur les monstres de haut niveau
            if (md.level >= 10 && random.nextDouble() < g.cloverDropChance * dropMultiplier) {
                // palier selon le niveau du monstre (le Trefle Divin ne vient que de l'Avatar du Neant et de la Forge)
                int top = md.level >= 70 ? 3 : md.level >= 50 ? 2 : md.level >= 30 ? 1 : 0;
                int t = Math.max(0, top - random.nextInt(2));
                loot.add(new ItemStack(ModItems.get(com.mmorpg.crafting.ForgeRecipes.CLOVERS[t])));
            }
            if (random.nextDouble() < g.upgradeStoneDropChance * dropMultiplier) {
                loot.add(new ItemStack(md.level >= 60 && random.nextInt(4) == 0 ? ModItems.PIERRE_AMELIORATION_SUP.get() : ModItems.PIERRE_AMELIORATION.get()));
            }
            if (random.nextDouble() < g.potionDropChance * dropMultiplier) {
                Item potion = switch (random.nextInt(4)) {
                    case 0 -> md.level >= 50 ? ModItems.POTION_SOIN_MAJEURE.get() : md.level >= 20 ? ModItems.POTION_SOIN.get() : ModItems.POTION_SOIN_MINEURE.get();
                    case 1 -> md.level >= 50 ? ModItems.POTION_MANA_MAJEURE.get() : md.level >= 20 ? ModItems.POTION_MANA.get() : ModItems.POTION_MANA_MINEURE.get();
                    case 2 -> ModItems.POTION_SOIN_MINEURE.get();
                    default -> ModItems.PARCHEMIN_TELEPORTATION.get();
                };
                loot.add(new ItemStack(potion));
            }
        }
        for (ItemStack stack : loot) {
            ItemEntity ie = new ItemEntity(level, dead.getX(), dead.getY() + 0.5, dead.getZ(), stack);
            ie.setDefaultPickUpDelay();
            event.getDrops().add(ie);
        }
    }

    /** Fleches explosives (competence d'archer). */
    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow) || !(arrow.level() instanceof ServerLevel level)) return;
        CompoundTag tag = arrow.getPersistentData();
        if (!tag.contains(RpgCombat.ARROW_EXPLODE)) return;
        double dmg = tag.getDoubleOr(RpgCombat.ARROW_EXPLODE, 0);
        double radius = tag.getDoubleOr("mmorpg_radius", 3);
        tag.remove(RpgCombat.ARROW_EXPLODE);
        Vec3 c = event.getRayTraceResult().getLocation();
        level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y, c.z, 3, radius * 0.3, 0.3, radius * 0.3, 0);
        level.sendParticles(ParticleTypes.FLAME, c.x, c.y, c.z, 30, radius * 0.4, 0.4, radius * 0.4, 0.05);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1f, 1.3f);
        if (arrow.getOwner() instanceof ServerPlayer owner) {
            for (LivingEntity e : MobAbilities.enemiesAround(owner, c, radius)) {
                RpgCombat.playerHit(owner, e, dmg, false);
            }
        }
        Scheduler.later(level, 1, arrow::discard);
    }
}
