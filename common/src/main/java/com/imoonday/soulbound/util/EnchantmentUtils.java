package com.imoonday.soulbound.util;

import com.imoonday.soulbound.Soulbound;
import com.imoonday.soulbound.config.ModConfig;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;

public class EnchantmentUtils {

    public static void copySoulBoundItems(PlayerEntity oldPlayer, PlayerEntity newPlayer, boolean alive) {
        if (!shouldCopyItems(oldPlayer, alive)) return;
        PlayerInventory oldInventory = oldPlayer.getInventory();
        PlayerInventory newInventory = newPlayer.getInventory();
        for (int i = 0; i < oldInventory.size(); i++) {
            ItemStack oldStack = oldInventory.getStack(i);
            ItemStack newStack = newInventory.getStack(i);
            if (!shouldCopy(oldStack, newStack)) continue;
            if (shouldDamage(oldPlayer, oldStack)) {
                damageRandomly(oldPlayer, oldStack);
                if (isBroken(oldStack)) {
                    if (ModConfig.get().allowBreakItem) continue;
                    oldStack.setDamage(oldStack.getMaxDamage() - 1);
                }
            }
            if (newStack.isEmpty()) {
                newInventory.setStack(i, oldStack);
            } else {
                newInventory.offerOrDrop(oldStack);
            }
        }
    }

    public static boolean shouldCopy(ItemStack oldStack, ItemStack newStack) {
        return hasSoulbound(oldStack) && !ItemStack.areEqual(oldStack, newStack);
    }

    public static boolean shouldCopyItems(PlayerEntity oldPlayer, boolean alive) {
        return !alive && !(oldPlayer.getWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY) || oldPlayer.isSpectator());
    }

    public static boolean isBroken(ItemStack stack) {
        return stack.getDamage() >= stack.getMaxDamage();
    }

    public static boolean shouldDamage(PlayerEntity player, ItemStack stack) {
        return ModConfig.get().maxDamagePercent != 0 && !player.isCreative() && stack.isDamageable();
    }

    public static void damageRandomly(PlayerEntity player, ItemStack stack) {
        Random random = player.getRandom();
        stack.damage(random.nextInt(stack.getMaxDamage() * ModConfig.get().maxDamagePercent / 100), random, player instanceof ServerPlayerEntity serverPlayer ? serverPlayer : null);
    }

    public static boolean hasSoulbound(ItemStack stack) {
        return EnchantmentHelper.getLevel(Soulbound.SOULBOUND, stack) > 0;
    }
}
