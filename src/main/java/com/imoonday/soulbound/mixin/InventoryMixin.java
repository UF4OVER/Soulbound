package com.imoonday.soulbound.mixin;

import com.imoonday.soulbound.Soulbound;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(Inventory.class)
public class InventoryMixin {

    @Shadow
    @Final
    public Player player;
    @Shadow
    @Final
    private List<NonNullList<ItemStack>> compartments;
    @Unique
    private final Map<int[], ItemStack> soulbound$reservedItems = new HashMap<>();

    @Inject(method = "dropAll", at = @At("HEAD"))
    public void soulbound$reserveItems(CallbackInfo ci) {
        for (int listIndex = 0; listIndex < compartments.size(); listIndex++) {
            NonNullList<ItemStack> list = compartments.get(listIndex);
            for (int itemIndex = 0; itemIndex < list.size(); itemIndex++) {
                ItemStack itemStack = list.get(itemIndex);
                if (soulbound$shouldReserve(itemStack)) {
                    soulbound$reservedItems.put(new int[]{listIndex, itemIndex}, itemStack);
                    list.set(itemIndex, ItemStack.EMPTY);
                }
            }
        }
    }

    @Inject(method = "dropAll", at = @At("RETURN"))
    public void soulbound$restoreItems(CallbackInfo ci) {
        soulbound$reservedItems.forEach((position, itemStack) -> compartments.get(position[0]).set(position[1], itemStack));
        soulbound$reservedItems.clear();
    }

    @Unique
    public boolean soulbound$shouldReserve(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return this.player.isAlive() || EnchantmentHelper.getItemEnchantmentLevel(Soulbound.SOUL_BOUND_ENCHANTMENT.get(), stack) > 0;
    }
}
