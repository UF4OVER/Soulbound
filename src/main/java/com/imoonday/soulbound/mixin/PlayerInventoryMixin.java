package com.imoonday.soulbound.mixin;

import com.imoonday.soulbound.SoulBound;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
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

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {

    @Shadow
    @Final
    public PlayerEntity player;
    @Shadow
    @Final
    private List<DefaultedList<ItemStack>> combinedInventory;
    @Unique
    private final Map<int[], ItemStack> reservedItems = new HashMap<>();

    @Inject(method = "dropAll", at = @At("HEAD"))
    public void reserveItems(CallbackInfo ci) {
        for (int listIndex = 0; listIndex < combinedInventory.size(); listIndex++) {
            DefaultedList<ItemStack> list = combinedInventory.get(listIndex);
            for (int itemIndex = 0; itemIndex < list.size(); itemIndex++) {
                ItemStack itemStack = list.get(itemIndex);
                if (shouldReserve(itemStack)) {
                    reservedItems.put(new int[]{listIndex, itemIndex}, itemStack);
                    list.set(itemIndex, ItemStack.EMPTY);
                }
            }
        }
    }

    @Inject(method = "dropAll", at = @At("RETURN"))
    public void restoreItems(CallbackInfo ci) {
        reservedItems.forEach((position, itemStack) -> combinedInventory.get(position[0]).set(position[1], itemStack));
        reservedItems.clear();
    }

    @Unique
    public boolean shouldReserve(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return this.player.isAlive() || EnchantmentHelper.getLevel(SoulBound.SOUL_BOUND, stack) > 0;
    }
}
