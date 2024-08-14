package com.imoonday.soulbound;

import com.imoonday.soulbound.config.ModConfig;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.VanishingCurseEnchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.List;

public class SoulBoundEnchantment extends Enchantment {

    public static final String IGNORED_NBT = "*";

    public SoulBoundEnchantment() {
        super(Rarity.RARE, EnchantmentTarget.BREAKABLE, EquipmentSlot.values());
    }

    @Override
    public int getMinPower(int level) {
        return ModConfig.get().minPower;
    }

    @Override
    public int getMaxPower(int level) {
        int powerRange = ModConfig.get().powerRange;
        if (powerRange < 0) {
            powerRange = 50;
        }
        return this.getMinPower(level) + powerRange;
    }

    @Override
    public boolean isTreasure() {
        return ModConfig.get().isTreasure;
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        ModConfig config = ModConfig.get();
        switch (config.compatibilityMode) {
            case WHITELIST_ONLY -> {
                return hasMatchItemStack(config.whitelist, stack);
            }
            case BLACKLIST_ONLY -> {
                return !hasMatchItemStack(config.blacklist, stack);
            }
            case WHITELIST_AND_DEFAULT -> {
                if (!hasMatchItemStack(config.whitelist, stack)) {
                    return false;
                }
            }
            case BLACKLIST_AND_DEFAULT -> {
                if (hasMatchItemStack(config.blacklist, stack)) {
                    return false;
                }
            }
        }
        return stack.isDamageable() || !stack.isStackable() || stack.isOf(Items.BOOK) || super.isAcceptableItem(stack);
    }

    @Override
    public boolean isAvailableForEnchantedBookOffer() {
        ModConfig config = ModConfig.get();
        return config.allowEnchantedBookTrade && !config.disableSurvivalObtaining;
    }

    @Override
    public boolean isAvailableForRandomSelection() {
        return !ModConfig.get().disableSurvivalObtaining;
    }

    @Override
    protected boolean canAccept(Enchantment other) {
        return (!(other instanceof VanishingCurseEnchantment) || !ModConfig.get().conflictWithVanishingCurse) && super.canAccept(other);
    }

    public static boolean hasMatchItemStack(List<String> list, ItemStack stack) {
        return list.stream().anyMatch(s -> match(stack, s));
    }

    public static boolean match(ItemStack stack, String s) {
        if (s.endsWith(IGNORED_NBT)) {
            String id = s.split("\\*", 2)[0];
            Identifier identifier = Identifier.tryParse(id);
            if (identifier == null) {
                identifier = Identifier.tryParse(Identifier.DEFAULT_NAMESPACE + ":" + id);
            }
            if (identifier == null) {
                return false;
            }
            Item item = Registries.ITEM.get(identifier);
            return stack.isOf(item);
        }
        String[] split = s.split("\\{", 2);
        if (split.length > 0) {
            Identifier identifier = Identifier.tryParse(split[0]);
            if (identifier == null) {
                identifier = Identifier.tryParse(Identifier.DEFAULT_NAMESPACE + ":" + split[0]);
            }
            if (identifier == null) {
                return false;
            }
            NbtCompound nbt = null;
            if (split.length > 1) {
                try {
                    nbt = StringNbtReader.parse("{" + split[1]);
                } catch (CommandSyntaxException ignored) {

                }
            }
            Item item = Registries.ITEM.get(identifier);
            if (item != Items.AIR) {
                ItemStack itemStack = new ItemStack(item);
                if (nbt != null) {
                    itemStack.setNbt(nbt);
                }
                return ItemStack.canCombine(itemStack, stack);
            }
        }
        return false;
    }
}
