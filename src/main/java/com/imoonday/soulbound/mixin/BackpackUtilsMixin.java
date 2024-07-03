package com.imoonday.soulbound.mixin;

import com.imoonday.soulbound.SoulBoundEnchantment;
import com.tiviacz.travelersbackpack.util.BackpackUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BackpackUtils.class)
public class BackpackUtilsMixin {

    @Inject(method = "onPlayerDrops", at = @At("HEAD"), cancellable = true)
    private static void handleOnPlayerDrops(World world, PlayerEntity player, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (SoulBoundEnchantment.hasSoulbound(stack)) {
            cir.setReturnValue(false);
        }
    }
}
