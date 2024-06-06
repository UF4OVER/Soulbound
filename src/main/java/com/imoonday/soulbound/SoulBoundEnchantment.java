package com.imoonday.soulbound;

import dev.emi.trinkets.api.TrinketEnums;
import dev.emi.trinkets.api.event.TrinketDropCallback;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.VanishingCurseEnchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;

import java.util.List;

public class SoulBoundEnchantment extends Enchantment {

    public static final String IGNORED_NBT = "*";
    private static boolean curios = FabricLoader.getInstance().isModLoaded("trinkets");
//    private static boolean travelersBackpack = FabricLoader.getInstance().isModLoaded("travelersbackpack");

    public SoulBoundEnchantment() {
        super(Enchantment.properties(ItemTags.DURABILITY_ENCHANTABLE, getConfig().weight, 1, Enchantment.constantCost(getConfig().minPower), Enchantment.constantCost(getConfig().maxPower), 4, EquipmentSlot.values()));
    }

    public boolean isTreasure() {
        return getConfig().isTreasure;
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        switch (getConfig().compatibilityMode) {
            case WHITELIST_ONLY -> {
                return hasMatchItemStack(getConfig().whitelist, stack);
            }
            case BLACKLIST_ONLY -> {
                return hasMatchItemStack(getConfig().blacklist, stack);
            }
            case WHITELIST_AND_DEFAULT -> {
                if (!hasMatchItemStack(getConfig().whitelist, stack)) {
                    return false;
                }
            }
            case BLACKLIST_AND_DEFAULT -> {
                if (hasMatchItemStack(getConfig().blacklist, stack)) {
                    return false;
                }
            }
        }
        return stack.isDamageable() || !stack.isStackable() || super.isAcceptableItem(stack);
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
//            NbtCompound nbt = null;
//            if (split.length > 1) {
//                try {
//                    nbt = StringNbtReader.parse("{" + split[1]);
//                } catch (CommandSyntaxException ignored) {
//
//                }
//            }
            Item item = Registries.ITEM.get(identifier);
            if (item != Items.AIR) {
                ItemStack itemStack = new ItemStack(item);
//                if (nbt != null) {
//                    itemStack.applyComponentsFrom(nbt);
//                }
                return ItemStack.areItemsEqual(itemStack, stack);
            }
        }
        return false;
    }

    @Override
    protected boolean canAccept(Enchantment other) {
        return (!(other instanceof VanishingCurseEnchantment) || !getConfig().conflictWithVanishingCurse) && super.canAccept(other);
    }

    public static void copySoulBoundItems(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
        if (!alive && !(oldPlayer.getWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY) || oldPlayer.isSpectator())) {
            for (int i = 0; i < oldPlayer.getInventory().size(); i++) {
                ItemStack oldStack = oldPlayer.getInventory().getStack(i);
                ItemStack newStack = newPlayer.getInventory().getStack(i);
                if (hasSoulbound(oldStack) && !ItemStack.areEqual(oldStack, newStack)) {
                    if (shouldDamage(oldPlayer, oldStack)) {
                        damageRandomly(oldStack, oldPlayer);
                        if (shouldBreakItem(oldStack)) continue;
                    }
                    if (newStack.isEmpty()) {
                        newPlayer.getInventory().setStack(i, oldStack);
                    } else {
                        newPlayer.getInventory().offerOrDrop(oldStack);
                    }
                }
            }

//            if (travelersBackpack) {
//                if (ComponentUtils.isWearingBackpack(oldPlayer)) {
//                    ItemStack backpack = ComponentUtils.getWearingBackpack(oldPlayer);
//                    int level = EnchantmentHelper.getLevel(SoulBound.SOUL_BOUND, backpack);
//                    if (level > 0) {
//                        if (ComponentUtils.isWearingBackpack(newPlayer)) {
//                            newPlayer.getInventory().offerOrDrop(backpack);
//                        } else {
//                            ComponentUtils.getComponent(newPlayer).setWearable(backpack);
//                            ComponentUtils.getComponent(newPlayer).setContents(backpack);
//                            ComponentUtils.sync(newPlayer);
//                        }
//                    }
//                }
//            }
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
                        damageRandomly(stack, player);
                        if (shouldBreakItem(stack)) return TrinketEnums.DropRule.DESTROY;
                    }
                    return TrinketEnums.DropRule.KEEP;
                }
                return rule;
            });
        }
    }

    public static boolean hasSoulbound(ItemStack stack) {
        return EnchantmentHelper.getLevel(SoulBound.SOUL_BOUND, stack) > 0;
    }

    private static boolean shouldDamage(ServerPlayerEntity player, ItemStack stack) {
        return getConfig().maxDamagePercent != 0 && !player.isCreative() && stack.isDamageable();
    }

    private static boolean shouldBreakItem(ItemStack stack) {
        int maxDamage = stack.getMaxDamage();
        if (stack.getDamage() >= maxDamage) {
            if (getConfig().allowBreakItem) {
                return true;
            } else {
                stack.setDamage(maxDamage - 1);
            }
        }
        return false;
    }

    private static void damageRandomly(ItemStack stack, ServerPlayerEntity player) {
        int maxDamage = stack.getMaxDamage();
        int damageRange = maxDamage * getConfig().maxDamagePercent / 100;
        if (damageRange <= 0) damageRange = maxDamage;
        Random random = player.getRandom();
        stack.damage(random.nextInt(damageRange) + 1, random, player, () -> {
        });
    }

    private static ModConfig getConfig() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig();
    }
}
