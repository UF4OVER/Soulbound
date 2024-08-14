package com.imoonday.soulbound;

import com.imoonday.soulbound.config.ModConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class Soulbound {

    public static final String MOD_ID = "soulbound";
    public static final Enchantment SOULBOUND = Registry.register(Registries.ENCHANTMENT, new Identifier(MOD_ID, MOD_ID), new SoulBoundEnchantment());

    public static void init() {
        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
    }
}
