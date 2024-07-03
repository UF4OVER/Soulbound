package com.imoonday.soulbound.mixin;

import com.beansgalaxy.backpacks.core.BackData;
import com.beansgalaxy.backpacks.events.LivingEntityDeath;
import com.imoonday.soulbound.SoulBoundEnchantment;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LivingEntityDeath.class, remap = false)
public class LivingEntityDeathMixin {

    @Inject(method = "afterDeath", at = @At("HEAD"), cancellable = true)
    public void onLivingEntityDeath(LivingEntity entity, DamageSource damageSource, CallbackInfo ci) {
        if (entity instanceof PlayerEntity player) {
            BackData backData = BackData.get(player);
            ItemStack stack = backData.getStack();
            if (SoulBoundEnchantment.hasSoulbound(stack)) {
                ci.cancel();
            }
        }
    }
}
