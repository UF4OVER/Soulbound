package com.imoonday.soulbound;

import com.beansgalaxy.backpacks.data.BackData;
import com.beansgalaxy.backpacks.platform.ForgeCompatHelper;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.tiviacz.travelersbackpack.capability.CapabilityUtils;
import com.tiviacz.travelersbackpack.capability.ITravelersBackpack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.VanishingCurseEnchantment;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.event.DropRulesEvent;
import top.theillusivec4.curios.api.type.capability.ICurio;

import java.util.List;
import java.util.Optional;

public class SoulBoundEnchantment extends Enchantment {

    public static final String IGNORED_NBT = "*";
    public static boolean curios = ModList.get().isLoaded("curios");
    public static boolean travelersBackpack = ModList.get().isLoaded("travelersbackpack");
    public static boolean beansBackpacks = ModList.get().isLoaded("beansbackpacks");

    protected SoulBoundEnchantment() {
        super(Rarity.RARE, EnchantmentCategory.BREAKABLE, EquipmentSlot.values());
    }

    @Override
    public int getMinCost(int level) {
        return Config.minPower;
    }

    @Override
    public int getMaxCost(int level) {
        int powerRange = Config.powerRange;
        if (powerRange < 0) {
            powerRange = 50;
        }
        return this.getMinCost(level) + powerRange;
    }

    @Override
    public boolean isTreasureOnly() {
        return Config.isTreasure;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        switch (Config.compatibilityMode) {
            case WHITELIST_ONLY -> {
                return hasMatchItemStack(Config.whitelist, stack);
            }
            case BLACKLIST_ONLY -> {
                return !hasMatchItemStack(Config.blacklist, stack);
            }
            case WHITELIST_AND_DEFAULT -> {
                if (!hasMatchItemStack(Config.whitelist, stack)) {
                    return false;
                }
            }
            case BLACKLIST_AND_DEFAULT -> {
                if (hasMatchItemStack(Config.blacklist, stack)) {
                    return false;
                }
            }
        }
        return stack.isDamageableItem() || !stack.isStackable() || stack.is(Items.BOOK) || super.canEnchant(stack);
    }

    public static boolean hasMatchItemStack(List<? extends String> list, ItemStack stack) {
        return list.stream().anyMatch(s -> match(stack, s));
    }

    public static boolean match(ItemStack stack, String s) {
        if (s.endsWith(IGNORED_NBT)) {
            String id = s.split("\\*", 2)[0];
            ResourceLocation identifier = ResourceLocation.tryParse(id);
            if (identifier == null) {
                identifier = ResourceLocation.tryParse(ResourceLocation.DEFAULT_NAMESPACE + ":" + id);
            }
            if (identifier == null) {
                return false;
            }
            Item item = ForgeRegistries.ITEMS.getValue(identifier);
            return stack.is(item);
        }
        String[] split = s.split("\\{", 2);
        if (split.length > 0) {
            ResourceLocation identifier = ResourceLocation.tryParse(split[0]);
            if (identifier == null) {
                identifier = ResourceLocation.tryParse(ResourceLocation.DEFAULT_NAMESPACE + ":" + split[0]);
            }
            if (identifier == null) {
                return false;
            }
            CompoundTag nbt = null;
            if (split.length > 1) {
                try {
                    nbt = TagParser.parseTag("{" + split[1]);
                } catch (CommandSyntaxException ignored) {

                }
            }
            Item item = ForgeRegistries.ITEMS.getValue(identifier);
            if (item != null) {
                ItemStack itemStack = new ItemStack(item);
                if (nbt != null) {
                    itemStack.save(nbt);
                }
                return ItemStack.isSameItemSameTags(itemStack, stack);
            }
        }
        return false;
    }

    @Override
    public boolean isTradeable() {
        return Config.allowEnchantedBookTrade && !Config.disableSurvivalObtaining;
    }

    @Override
    public boolean isDiscoverable() {
        return !Config.disableSurvivalObtaining;
    }

    @Override
    protected boolean checkCompatibility(@NotNull Enchantment other) {
        return (!(other instanceof VanishingCurseEnchantment) || !Config.conflictWithVanishingCurse) && super.checkCompatibility(other);
    }

    public static void copySoulBoundItems(Player oldPlayer, Player newPlayer, boolean wasDeath) {
        if (wasDeath && !(newPlayer.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) || oldPlayer.isSpectator())) {
            for (int i = 0; i < oldPlayer.getInventory().getContainerSize(); i++) {
                ItemStack oldStack = oldPlayer.getInventory().getItem(i);
                ItemStack newStack = newPlayer.getInventory().getItem(i);
                if (hasSoulbound(oldStack) && !ItemStack.matches(oldStack, newStack)) {
                    if (shouldDamage(oldPlayer, oldStack)) {
                        damageRandomly(oldPlayer, oldStack);
                        if (isBroken(oldStack)) {
                            if (Config.allowBreakItem) {
                                continue;
                            } else {
                                oldStack.setDamageValue(oldStack.getMaxDamage() - 1);
                            }
                        }
                    }
                    if (newStack.isEmpty()) {
                        newPlayer.getInventory().setItem(i, oldStack);
                    } else {
                        newPlayer.getInventory().placeItemBackInInventory(oldStack);
                    }
                }
            }

            if (travelersBackpack) {
                if (CapabilityUtils.isWearingBackpack(oldPlayer)) {
                    ItemStack backpack = CapabilityUtils.getWearingBackpack(oldPlayer);
                    if (hasSoulbound(backpack)) {
                        Optional<ITravelersBackpack> optional = CapabilityUtils.getCapability(newPlayer).resolve();
                        boolean synchronised = false;
                        if (optional.isPresent()) {
                            ITravelersBackpack iTravelersBackpack = optional.get();
                            ItemStack wearable = iTravelersBackpack.getWearable();
                            ItemStack content = iTravelersBackpack.getContainer().getItemStack();
                            boolean areNull = wearable == null && content == null;
                            boolean areEmpty = wearable != null && content != null && wearable.isEmpty() && content.isEmpty();
                            boolean areEqual = backpack.equals(wearable) && backpack.equals(content);
                            if (areNull || areEmpty || areEqual) {
                                iTravelersBackpack.setWearable(backpack);
                                iTravelersBackpack.setContents(backpack);
                                iTravelersBackpack.synchronise();
                                iTravelersBackpack.synchroniseToOthers(newPlayer);
                                synchronised = true;
                            }
                        }
                        if (!synchronised) {
                            newPlayer.getInventory().placeItemBackInInventory(backpack);
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
    }

    public static void addCuriosDropListener() {
        if (curios) {
            MinecraftForge.EVENT_BUS.<DropRulesEvent>addListener(event -> {
                Entity entity = event.getEntity();
                if (!(entity instanceof ServerPlayer player)) {
                    return;
                }
                event.addOverride(stack -> {
                    if (hasSoulbound(stack)) {
                        if (shouldDamage(player, stack)) {
                            damageRandomly(player, stack);
                            if (isBroken(stack)) {
                                if (Config.allowBreakItem) {
                                    return false;
                                } else {
                                    stack.setDamageValue(stack.getMaxDamage() - 1);
                                }
                            }
                        }
                        return true;
                    }
                    return false;
                }, ICurio.DropRule.ALWAYS_KEEP);
            });
        }
    }

    public static void registerBeansBackpacksDropCallback() {
        if (beansBackpacks) {
            MinecraftForge.EVENT_BUS.<ForgeCompatHelper.OnDeath>addListener(event -> {
                if (event.getEntity() instanceof ServerPlayer) {
                    ItemStack stack = event.getBackStack();
                    if (hasSoulbound(stack)) {
                        event.setCanceled(true);
                    }
                }
            });
        }
    }

    private static boolean isBroken(ItemStack stack) {
        return stack.getDamageValue() >= stack.getMaxDamage();
    }

    private static boolean shouldDamage(Player player, ItemStack stack) {
        return Config.maxDamagePercent != 0 && !player.isCreative() && stack.isDamageableItem();
    }

    private static void damageRandomly(Player player, ItemStack stack) {
        RandomSource random = player.getRandom();
        stack.hurt(random.nextInt(stack.getMaxDamage() * Config.maxDamagePercent / 100), random, player instanceof ServerPlayer serverPlayer ? serverPlayer : null);
    }

    public static boolean hasSoulbound(ItemStack stack) {
        return stack.getEnchantmentLevel(Soulbound.SOUL_BOUND_ENCHANTMENT.get()) > 0;
    }
}
