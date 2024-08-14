package com.imoonday.soulbound.forge;

import com.beansgalaxy.backpacks.data.BackData;
import com.beansgalaxy.backpacks.platform.ForgeCompatHelper;
import com.imoonday.soulbound.Soulbound;
import com.imoonday.soulbound.config.ModConfig;
import com.imoonday.soulbound.util.EnchantmentUtils;
import com.tiviacz.travelersbackpack.capability.CapabilityUtils;
import com.tiviacz.travelersbackpack.capability.ITravelersBackpack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.event.DropRulesEvent;
import top.theillusivec4.curios.api.type.capability.ICurio;

import java.util.Optional;

import static com.imoonday.soulbound.util.EnchantmentUtils.*;

@Mod(Soulbound.MOD_ID)
public final class SoulboundForge {

    public static boolean curios = ModList.get().isLoaded("curios");
    public static boolean travelersBackpack = ModList.get().isLoaded("travelersbackpack");
    public static boolean beansBackpacks = ModList.get().isLoaded("beansbackpacks");

    public SoulboundForge() {
        Soulbound.init();
        MinecraftForge.EVENT_BUS.addListener(this::onClone);
        addCuriosDropListener();
        registerBeansBackpacksDropCallback();
    }

    public void onClone(PlayerEvent.Clone e) {
        EnchantmentUtils.copySoulBoundItems(e.getOriginal(), e.getEntity(), !e.isWasDeath());
        copyOtherData(e.getOriginal(), e.getEntity(), !e.isWasDeath());
    }

    private void copyOtherData(PlayerEntity oldPlayer, PlayerEntity newPlayer, boolean alive) {
        if (!shouldCopyItems(oldPlayer, alive)) return;

        if (travelersBackpack) {
            oldPlayer.reviveCaps();
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
                        newPlayer.getInventory().offerOrDrop(backpack);
                    }
                }
            }
            oldPlayer.invalidateCaps();
        }

        if (beansBackpacks) {
            BackData backData = BackData.get(oldPlayer);
            ItemStack stack = backData.getStack();
            if (hasSoulbound(stack)) {
                backData.copyTo(BackData.get(newPlayer));
            }
        }
    }

    private static void addCuriosDropListener() {
        if (curios) {
            MinecraftForge.EVENT_BUS.<DropRulesEvent>addListener(e -> {
                Entity entity = e.getEntity();
                if (entity instanceof ServerPlayerEntity player) {
                    e.addOverride(stack -> {
                        if (hasSoulbound(stack)) {
                            if (shouldDamage(player, stack)) {
                                damageRandomly(player, stack);
                                if (isBroken(stack)) {
                                    if (ModConfig.get().allowBreakItem) {
                                        return false;
                                    } else {
                                        stack.setDamage(stack.getMaxDamage() - 1);
                                    }
                                }
                            }
                            return true;
                        }
                        return false;
                    }, ICurio.DropRule.ALWAYS_KEEP);
                }
            });
        }
    }

    private static void registerBeansBackpacksDropCallback() {
        if (beansBackpacks) {
            MinecraftForge.EVENT_BUS.<ForgeCompatHelper.OnDeath>addListener(e -> {
                if (e.getEntity() instanceof ServerPlayerEntity) {
                    ItemStack stack = e.getBackStack();
                    if (hasSoulbound(stack)) {
                        e.setCanceled(true);
                    }
                }
            });
        }
    }
}
