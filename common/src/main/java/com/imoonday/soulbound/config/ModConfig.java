package com.imoonday.soulbound.config;

import com.imoonday.soulbound.Soulbound;
import com.imoonday.soulbound.util.CompatibilityMode;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;
import java.util.List;

@Config(name = Soulbound.MOD_ID)
public class ModConfig implements ConfigData {

    public boolean conflictWithVanishingCurse = true;

    public boolean isTreasure = true;

    public boolean allowEnchantedBookTrade = true;

    public boolean allowBreakItem = false;

    public boolean disableSurvivalObtaining = false;

    @ConfigEntry.BoundedDiscrete(max = 100)
    public int maxDamagePercent = 20;

    @ConfigEntry.BoundedDiscrete(max = 100)
    public int minPower = 25;

    @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
    public int powerRange = 50;

    public CompatibilityMode compatibilityMode = CompatibilityMode.DEFAULT;

    @ConfigEntry.Gui.Tooltip
    public List<String> blacklist = new ArrayList<>();

    @ConfigEntry.Gui.Tooltip
    public List<String> whitelist = new ArrayList<>();

    public static ModConfig get() {
        return AutoConfig.getConfigHolder(ModConfig.class).get();
    }
}
