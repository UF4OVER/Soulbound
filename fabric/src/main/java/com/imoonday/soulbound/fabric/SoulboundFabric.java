package com.imoonday.soulbound.fabric;

import com.beansgalaxy.backpacks.data.BackData;
import com.beansgalaxy.backpacks.platform.FabricCompatHelper;
import com.imoonday.soulbound.Soulbound;
import com.imoonday.soulbound.config.ModConfig;
import com.imoonday.soulbound.util.EnchantmentUtils;
import com.tiviacz.travelersbackpack.component.ComponentUtils;
import dev.emi.trinkets.api.TrinketEnums;
import dev.emi.trinkets.api.event.TrinketDropCallback;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import static com.imoonday.soulbound.util.EnchantmentUtils.*;

public final class SoulboundFabric implements ModInitializer {

    public static boolean curios = FabricLoader.getInstance().isModLoaded("trinkets");
    public static boolean travelersBackpack = FabricLoader.getInstance().isModLoaded("travelersbackpack");
    public static boolean beansBackpacks = FabricLoader.getInstance().isModLoaded("beansbackpacks");

    @Override
    public void onInitialize() {
        Soulbound.init();
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            EnchantmentUtils.copySoulBoundItems(oldPlayer, newPlayer, alive);
            copyOtherData(oldPlayer, newPlayer, alive);
        });
        registerTrinketDropCallback();
        registerBeansBackpacksDropCallback();
    }

    private static void copyOtherData(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
        if (!shouldCopyItems(oldPlayer, alive)) return;

        if (travelersBackpack) {
            if (ComponentUtils.isWearingBackpack(oldPlayer)) {
                ItemStack backpack = ComponentUtils.getWearingBackpack(oldPlayer);
                if (hasSoulbound(backpack)) {
                    if (ComponentUtils.isWearingBackpack(newPlayer)) {
                        newPlayer.getInventory().offerOrDrop(backpack);
                    } else {
                        ComponentUtils.getComponent(newPlayer).setWearable(backpack);
                        ComponentUtils.getComponent(newPlayer).setContents(backpack);
                        ComponentUtils.sync(newPlayer);
                    }
                }
            }
        }

        if (beansBackpacks) {
            BackData backData = BackData.get(oldPlayer);
            ItemStack stack = backData.getStack();
            if (hasSoulbound(stack)) {
                backData.copyTo(BackData.get(newPlayer));
            }
        }
    }

    public static void registerTrinketDropCallback() {
        if (curios) {
            TrinketDropCallback.EVENT.register((rule, stack, ref, entity) -> {
                if (!(entity instanceof ServerPlayerEntity player)) {
                    return rule;
                }
                if (hasSoulbound(stack)) {
                    if (shouldDamage(player, stack)) {
                        damageRandomly(player, stack);
                        if (isBroken(stack)) {
                            if (ModConfig.get().allowBreakItem) {
                                return TrinketEnums.DropRule.DESTROY;
                            } else {
                                stack.setDamage(stack.getMaxDamage() - 1);
                            }
                        }
                    }
                    return TrinketEnums.DropRule.KEEP;
                }
                return rule;
            });
        }
    }

    public static void registerBeansBackpacksDropCallback() {
        if (beansBackpacks) {
            FabricCompatHelper.OnDeathCallback.EVENT.register(context -> {
                if (context.getPlayer() instanceof ServerPlayerEntity) {
                    ItemStack stack = context.getBackStack();
                    if (hasSoulbound(stack)) {
                        context.cancel();
                    }
                }
            });
        }
    }
}
