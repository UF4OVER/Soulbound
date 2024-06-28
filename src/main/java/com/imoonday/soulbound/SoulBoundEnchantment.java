package com.imoonday.soulbound;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.tiviacz.travelersbackpack.component.ComponentUtils;
import dev.emi.trinkets.api.TrinketEnums;
import dev.emi.trinkets.api.event.TrinketDropCallback;
import eu.pb4.graves.event.PlayerGraveItemAddedEvent;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.VanishingCurseEnchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.GameRules;

import java.util.List;
import java.util.Random;

public class SoulBoundEnchantment extends Enchantment {

    public static final String IGNORED_NBT = "*";
    public static boolean curios = FabricLoader.getInstance().isModLoaded("trinkets");
    public static boolean travelersBackpack = FabricLoader.getInstance().isModLoaded("travelersbackpack");
    public static boolean universalGraves = FabricLoader.getInstance().isModLoaded("universal-graves");

    public SoulBoundEnchantment() {
        super(Rarity.RARE, EnchantmentTarget.BREAKABLE, EquipmentSlot.values());
    }

    @Override
    public int getMinPower(int level) {
        return getConfig().minPower;
    }

    @Override
    public int getMaxPower(int level) {
        int powerRange = getConfig().powerRange;
        if (powerRange < 0) {
            powerRange = 50;
        }
        return this.getMinPower(level) + powerRange;
    }

    public boolean isTreasure() {
        return getConfig().isTreasure;
    }

    @Override
    public boolean isAvailableForEnchantedBookOffer() {
        return getConfig().allowEnchantedBookTrade;
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        switch (getConfig().compatibilityMode) {
            case WHITELIST_ONLY -> {
                return hasMatchItemStack(getConfig().whitelist, stack);
            }
            case BLACKLIST_ONLY -> {
                return !hasMatchItemStack(getConfig().blacklist, stack);
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
        return stack.isDamageable() || !stack.isStackable() || stack.isOf(Items.BOOK) || super.isAcceptableItem(stack);
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
            Item item = Registry.ITEM.get(identifier);
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
            Item item = Registry.ITEM.get(identifier);
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

    @Override
    protected boolean canAccept(Enchantment other) {
        return (!(other instanceof VanishingCurseEnchantment) || !getConfig().conflictWithVanishingCurse) && super.canAccept(other);
    }

    public static void copySoulBoundItems(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
        if (!alive && !(oldPlayer.getWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY) || oldPlayer.isSpectator())) {
            for (int i = 0; i < oldPlayer.getInventory().size(); i++) {
                ItemStack oldStack = oldPlayer.getInventory().getStack(i);
                ItemStack newStack = newPlayer.getInventory().getStack(i);
                int level = EnchantmentHelper.getLevel(SoulBound.SOUL_BOUND, oldStack);
                if (level > 0 && !ItemStack.areEqual(oldStack, newStack)) {
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
                    int level = EnchantmentHelper.getLevel(SoulBound.SOUL_BOUND, backpack);
                    if (level > 0) {
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

    public static void registerTrinketDropCallback() {
        if (curios) {
            TrinketDropCallback.EVENT.register((rule, stack, ref, entity) -> {
                if (!(entity instanceof ServerPlayerEntity player)) {
                    return rule;
                }
                if (EnchantmentHelper.getLevel(SoulBound.SOUL_BOUND, stack) > 0) {
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

    public static void registerUniversalGravesItemAddedEvent() {
        if (universalGraves) {
            PlayerGraveItemAddedEvent.EVENT.register((player, stack) -> {
                if (EnchantmentHelper.getLevel(SoulBound.SOUL_BOUND, stack) > 0) {
                    if (shouldDamage(player, stack)) {
                        damageRandomly(player, stack);
                        if (isBroken(stack)) {
                            if (getConfig().allowBreakItem) {
                                return ActionResult.PASS;
                            } else {
                                stack.setDamage(stack.getMaxDamage() - 1);
                            }
                        }
                    }
                    return ActionResult.PASS;
                }
                return ActionResult.SUCCESS;
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
        Random random = player.getRandom();
        stack.damage(random.nextInt(stack.getMaxDamage() * getConfig().maxDamagePercent / 100), random, player);
    }

    private static ModConfig getConfig() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig();
    }
}
