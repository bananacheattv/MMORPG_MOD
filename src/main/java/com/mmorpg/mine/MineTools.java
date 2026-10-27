package com.mmorpg.mine;

import com.mmorpg.MMORPG;
import com.mmorpg.item.ItemTooltips;
import com.mmorpg.registry.ModItems;
import com.mmorpg.rpg.Rarity;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Outils des mines : six paliers (debutant, Verdite, Argentite, Ambrasite, Glacium, Neantite) de pioches, haches, pelles et houes aux
 * modeles 3D (art/outils_blockbench). Le palier d'une pioche decide quel minerai elle peut extraire (voir {@link MineManager}) et
 * augmente la chance de lingot supplementaire et de lingots rares ; la houe augmente le rendement des cultures (voir MineCrops).
 */
public final class MineTools {
    private MineTools() {
    }

    public static final String[] TIER_ID = {"debutant", "verdite", "argentite", "ambrasite", "glacium", "neantite"};
    public static final String[] TIER_NAME = {"du débutant", "de Verdite", "d'Argentite", "d'Ambrasite", "de Glacium", "de Néantite"};
    public static final int[] TIER_COLOR = {0xB8A890, 0x58D670, 0xA9C0F0, 0xFF9030, 0x70DCFF, 0xB070FF};
    private static final Rarity[] RARITY = {Rarity.COMMUN, Rarity.PEU_COMMUN, Rarity.RARE, Rarity.EPIQUE, Rarity.LEGENDAIRE, Rarity.MYTHIQUE};
    private static final int[] DURABILITY = {160, 520, 1050, 1800, 2900, 4300};
    private static final float[] SPEED = {4.5F, 6.0F, 8.0F, 10.0F, 12.5F, 15.0F};
    private static final int[] ENCHANT = {8, 10, 13, 16, 20, 25};

    public static final ToolMaterial[] MATERIAL = new ToolMaterial[6];
    private static final Map<Item, Integer> PICK_TIER = new HashMap<>();
    public static final Map<Item, Integer> HOE_TIER = new HashMap<>();
    public static final List<DeferredItem<Item>> ITEMS = new ArrayList<>();

    /** Pioche : l'objet porte son palier et une infobulle qui explique ce qu'elle extrait. */
    public static final class Pickaxe extends Item {
        private final int tier;

        public Pickaxe(Properties properties, int tier) {
            super(properties);
            this.tier = tier;
        }

        @Override public <T extends net.minecraft.world.entity.LivingEntity> int damageItem(ItemStack stack,int amount,T entity,java.util.function.Consumer<ItemStack> onBroken){
            if(entity==null)return amount;int keep=0;for(int i=0;i<amount;i++)if(!ToolRunes.preserve(stack,entity.getRandom()))keep++;return keep;
        }

        @Override
        public Component getName(ItemStack stack) {
            return Component.translatable(this.getDescriptionId()).withColor(TIER_COLOR[tier]);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
            String reach = tier >= MineBlocks.ORES.size() - 1 ? "tous les minerais" : "jusqu'à la " + MineBlocks.ORES.get(Math.min(tier, MineBlocks.ORES.size() - 1)).name();
            String vein = MineManager.veinLimit(tier) > 0 ? "\nBonus : accroupi, extrait toute la veine d'un coup (jusqu'à " + MineManager.veinLimit(tier) + " minerais)." : "\nÀ partir de l'Ambrasite : accroupi, extrait toute la veine d'un coup.";
            ToolRunes.tooltip(stack,out);
            ItemTooltips.simple(RARITY[tier], "Pioche des mines", "Extrait " + (tier == 0 ? "la Verdite" : reach) + ". Plus le palier est haut, plus les lingots doublent et plus les lingots rares tombent." + vein, out);
        }
    }

    /** Hache, pelle ou houe des mines : la houe recolte en zone et replante (voir AeloriaCropBlock). */
    public static final class Tool extends Item {
        private final int tier;
        private final String kind;

        public Tool(Properties properties, int tier, String kind) {
            super(properties);
            this.tier = tier;
            this.kind = kind;
        }

        @Override public <T extends net.minecraft.world.entity.LivingEntity> int damageItem(ItemStack stack,int amount,T entity,java.util.function.Consumer<ItemStack> onBroken){
            if(entity==null)return amount;int keep=0;for(int i=0;i<amount;i++)if(!ToolRunes.preserve(stack,entity.getRandom()))keep++;return keep;
        }

        @Override
        public Component getName(ItemStack stack) {
            return Component.translatable(this.getDescriptionId()).withColor(TIER_COLOR[tier]);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
            String text = kind.equals("Houe")
                    ? "Chaque récolte mûre peut donner des récoltes en plus." + (tier >= 2 ? "\nBonus : récolte aussi les plantes mûres alentour (rayon " + (tier >= 4 ? 2 : 1) + ") et les replante." : "\nÀ partir de l'Argentite : récolte en zone et replante.")
                    : "Outil forgé dans le minerai du palier ; plus le palier est haut, plus il est solide et rapide.";
            ToolRunes.tooltip(stack,out);
            ItemTooltips.simple(RARITY[tier], kind + " des mines", text, out);
        }
    }

    static {
        MATERIAL[0] = material(BlockTags.INCORRECT_FOR_STONE_TOOL, 0);
        MATERIAL[1] = material(BlockTags.INCORRECT_FOR_IRON_TOOL, 1);
        MATERIAL[2] = material(BlockTags.INCORRECT_FOR_IRON_TOOL, 2);
        MATERIAL[3] = material(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 3);
        MATERIAL[4] = material(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 4);
        MATERIAL[5] = material(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 5);
        for (int t = 0; t < TIER_ID.length; t++) {
            final int tier = t;
            ToolMaterial mat = MATERIAL[t];
            reg("pioche_" + TIER_ID[t], p -> new Pickaxe(p.pickaxe(mat, 1.0F, -2.8F), tier));
            reg("houe_" + TIER_ID[t], p -> new Tool(p.hoe(mat, -2.0F, -1.0F + tier * 0.25F), tier, "Houe"));
            if (t == 0) continue;
            reg("hache_" + TIER_ID[t], p -> new Tool(p.axe(mat, 6.0F + tier * 0.5F, -3.1F + tier * 0.1F), tier, "Hache"));
            reg("pelle_" + TIER_ID[t], p -> new Tool(p.shovel(mat, 1.5F, -3.0F), tier, "Pelle"));
        }
    }

    /** Enregistre un outil et l'ajoute a l'index des objets (ModItems.get). */
    private static void reg(String id, java.util.function.Function<Item.Properties, Item> factory) {
        DeferredItem<Item> item = ModItems.ITEMS.registerItem(id, factory);
        ITEMS.add(item);
        ModItems.BY_ID.put(id, item);
    }

    private static ToolMaterial material(TagKey<net.minecraft.world.level.block.Block> incorrect, int tier) {
        TagKey<Item> repair = TagKey.create(Registries.ITEM, MMORPG.id("reparation_" + TIER_ID[tier]));
        return new ToolMaterial(incorrect, DURABILITY[tier], SPEED[tier], tier * 1.0F, ENCHANT[tier], repair);
    }

    /** Charge la classe (enregistrement des objets) avant les registres. */
    public static void init() {
    }

    /** Palier d'une pioche (0 pour une pioche de Minecraft ou le poing). */
    public static int pickTier(ItemStack stack) {
        if (stack.getItem() instanceof Pickaxe p) return p.tier;
        return 0;
    }

    /** Palier d'une houe (0 pour toute autre houe). */
    public static int hoeTier(ItemStack stack) {
        String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (!path.startsWith("houe_")) return 0;
        for (int i = 0; i < TIER_ID.length; i++) if (path.equals("houe_" + TIER_ID[i])) return i;
        return 0;
    }
}
