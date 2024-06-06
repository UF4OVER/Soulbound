package com.imoonday.soulbound;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class SoulBound implements ModInitializer {

    public static Enchantment SOUL_BOUND;

    @Override
    public void onInitialize() {
        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
        SOUL_BOUND = Registry.register(Registries.ENCHANTMENT, new Identifier("soulbound", "soulbound"), new SoulBoundEnchantment());
        ServerPlayerEvents.COPY_FROM.register(SoulBoundEnchantment::copySoulBoundItems);
        SoulBoundEnchantment.registerTrinketDropCallback();
    }
}
