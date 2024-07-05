package com.imoonday.soulbound;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "soulbound")
public class ModConfig implements ConfigData {

    public boolean allowBreakItem = false;

    @ConfigEntry.BoundedDiscrete(max = 100)
    public int maxDamagePercent = 20;
}
