package com.imoonday.soulbound;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = Soulbound.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue ALLOW_BREAK_ITEM = BUILDER
            .define("allowBreakItem", false);

    private static final ModConfigSpec.IntValue MAX_DAMAGE_PERCENT = BUILDER
            .defineInRange("maxDamagePercent", 20, 0, 100);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean allowBreakItem;
    public static int maxDamagePercent;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        allowBreakItem = ALLOW_BREAK_ITEM.get();
        maxDamagePercent = MAX_DAMAGE_PERCENT.get();
    }
}
