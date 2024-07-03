package com.imoonday.soulbound.mixin;

import com.beansgalaxy.backpacks.events.LivingEntityDeath;
import com.imoonday.soulbound.SoulBoundEnchantment;
import com.tiviacz.travelersbackpack.util.BackpackUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackpackUtils.class)
public class BackpackUtilsMixin {

    @Inject(method = "onPlayerDeath", at = @At("HEAD"), cancellable = true)
    private static void handleOnPlayerDeath(World world, PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        if (SoulBoundEnchantment.hasSoulbound(stack)) {
            ci.cancel();
        }
    }
}
