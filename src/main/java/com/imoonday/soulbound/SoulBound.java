package com.imoonday.soulbound;

import com.tiviacz.travelersbackpack.component.ComponentUtils;
import dev.emi.trinkets.api.TrinketEnums;
import dev.emi.trinkets.api.event.TrinketDropCallback;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;

public class SoulBound implements ModInitializer {

    public static final TagKey<Item> SOULBOUND_ENCHANTABLE = TagKey.of(RegistryKeys.ITEM, id("enchantable"));
    public static final RegistryKey<Enchantment> SOULBOUND = of("soulbound");
    public static boolean trinkets = FabricLoader.getInstance().isModLoaded("trinkets");
    public static boolean travelersBackpack = FabricLoader.getInstance().isModLoaded("travelersbackpack");

    @Override
    public void onInitialize() {
        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
        ServerPlayerEvents.COPY_FROM.register(SoulBound::copySoulBoundItems);
        registerTrinketDropCallback();
    }

    private static RegistryKey<Enchantment> of(String id) {
        return RegistryKey.of(RegistryKeys.ENCHANTMENT, id(id));
    }

    public static Identifier id(String id) {
        return Identifier.of("soulbound", id);
    }

    private static void copySoulBoundItems(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
        if (!alive && !(oldPlayer.getWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY) || oldPlayer.isSpectator())) {
            for (int i = 0; i < oldPlayer.getInventory().size(); i++) {
                ItemStack oldStack = oldPlayer.getInventory().getStack(i);
                ItemStack newStack = newPlayer.getInventory().getStack(i);
                if (hasSoulbound(oldStack) && !ItemStack.areEqual(oldStack, newStack)) {
                    if (shouldDamage(oldPlayer, oldStack)) {
                        damageRandomly(oldPlayer, oldStack);
                        if (isBroken(oldStack)) {
                            if (getConfig().allowBreakItem) {
                                continue;
                            } else {
                                oldStack.setDamage(oldStack.getMaxDamage() - 1);
                            }
                        }
                    }
                    if (newStack.isEmpty()) {
                        newPlayer.getInventory().setStack(i, oldStack);
                    } else {
                        newPlayer.getInventory().offerOrDrop(oldStack);
                    }
                }
            }

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
        }
    }

    private static void registerTrinketDropCallback() {
        if (trinkets) {
            TrinketDropCallback.EVENT.register((rule, stack, ref, entity) -> {
                if (!(entity instanceof ServerPlayerEntity player)) {
                    return rule;
                }
                if (hasSoulbound(stack)) {
                    if (shouldDamage(player, stack)) {
                        damageRandomly(player, stack);
                        if (isBroken(stack)) {
                            if (getConfig().allowBreakItem) {
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

    private static boolean isBroken(ItemStack stack) {
        return stack.getDamage() >= stack.getMaxDamage();
    }

    private static boolean shouldDamage(ServerPlayerEntity player, ItemStack stack) {
        return getConfig().maxDamagePercent != 0 && !player.isCreative() && stack.isDamageable();
    }

    private static void damageRandomly(ServerPlayerEntity player, ItemStack stack) {
        int maxDamage = stack.getMaxDamage();
        int damageRange = maxDamage * getConfig().maxDamagePercent / 100;
        if (damageRange <= 0) damageRange = maxDamage;
        Random random = player.getRandom();
        stack.damage(random.nextInt(damageRange) + 1, player.getServerWorld(), player, item -> {
        });
    }

    public static boolean hasSoulbound(ItemStack stack) {
        return EnchantmentHelper.getEnchantments(stack).getEnchantments().stream().anyMatch(enchantment -> enchantment.matchesKey(SoulBound.SOULBOUND));
    }

    private static ModConfig getConfig() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig();
    }
}
