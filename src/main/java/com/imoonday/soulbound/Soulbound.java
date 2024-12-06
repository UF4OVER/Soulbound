package com.imoonday.soulbound;

import com.mojang.logging.LogUtils;
import com.tiviacz.travelersbackpack.capability.AttachmentUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;
import top.theillusivec4.curios.api.event.DropRulesEvent;
import top.theillusivec4.curios.api.type.capability.ICurio;

import java.util.Optional;

@Mod(Soulbound.MODID)
public class Soulbound {

    public static final String MODID = "soulbound";
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceKey<Enchantment> SOULBOUND = ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(MODID, MODID));

    public static boolean curios = ModList.get().isLoaded("curios");
    public static boolean travelersBackpack = ModList.get().isLoaded("travelersbackpack");

    public Soulbound(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.addListener(this::onClone);
        addCuriosDropListener();
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }
    }

    public void onClone(PlayerEvent.Clone e) {
        copySoulBoundItems(e.getOriginal(), e.getEntity(), e.isWasDeath());
    }

    public static void copySoulBoundItems(Player oldPlayer, Player newPlayer, boolean wasDeath) {
        Level level = newPlayer.level();
        if (wasDeath && !(level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) || oldPlayer.isSpectator())) {
            for (int i = 0; i < oldPlayer.getInventory().getContainerSize(); i++) {
                ItemStack oldStack = oldPlayer.getInventory().getItem(i);
                ItemStack newStack = newPlayer.getInventory().getItem(i);
                if (hasSoulbound(level, oldStack) && !ItemStack.matches(oldStack, newStack)) {
                    if (shouldDamage(oldPlayer, oldStack)) {
                        damageRandomly(oldPlayer, oldStack);
                        if (isBroken(oldStack)) {
                            if (Config.allowBreakItem) continue;
                            if (oldStack.isEmpty()) {
                                oldStack.grow(1);
                            }
                            oldStack.setDamageValue(oldStack.getMaxDamage() - 1);
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
                if (AttachmentUtils.isWearingBackpack(oldPlayer)) {
                    ItemStack backpack = AttachmentUtils.getWearingBackpack(oldPlayer);
                    if (hasSoulbound(level, backpack)) {
                        AttachmentUtils.getAttachment(newPlayer).ifPresentOrElse(iTravelersBackpack -> {
                            iTravelersBackpack.updateBackpack(backpack);
                            iTravelersBackpack.synchronise();
                        }, () -> newPlayer.getInventory().placeItemBackInInventory(backpack));
                    }
                }
            }
        }
    }

    public static void addCuriosDropListener() {
        if (curios) {
            NeoForge.EVENT_BUS.<DropRulesEvent>addListener(event -> {
                Entity entity = event.getEntity();
                if (!(entity instanceof ServerPlayer player)) {
                    return;
                }
                event.addOverride(stack -> {
                    if (hasSoulbound(player.serverLevel(), stack)) {
                        if (shouldDamage(player, stack)) {
                            damageRandomly(player, stack);
                            if (isBroken(stack)) {
                                if (Config.allowBreakItem) return false;
                                if (stack.isEmpty()) {
                                    stack.grow(1);
                                }
                                stack.setDamageValue(stack.getMaxDamage() - 1);
                            }
                        }
                        return true;
                    }
                    return false;
                }, ICurio.DropRule.ALWAYS_KEEP);
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
        int maxDamage = stack.getMaxDamage();
        int damageRange = maxDamage * Config.maxDamagePercent / 100;
        if (damageRange <= 0) damageRange = maxDamage;
        RandomSource random = player.getRandom();
        if (player.level() instanceof ServerLevel level) {
            stack.hurtAndBreak(random.nextInt(damageRange) + 1, level, player, item -> {
            });
        }
    }

    public static boolean hasSoulbound(Level level, ItemStack stack) {
        Optional<Holder.Reference<Enchantment>> holder = level.registryAccess().registry(Registries.ENCHANTMENT).flatMap(registry -> registry.getHolder(Soulbound.SOULBOUND));
        return holder.filter(reference -> stack.getEnchantmentLevel(reference) > 0).isPresent();
    }
}
