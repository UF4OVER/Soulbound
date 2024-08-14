package com.imoonday.soulbound.util;

import com.google.common.base.CaseFormat;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;

public enum CompatibilityMode implements StringIdentifiable {
    BLACKLIST_AND_DEFAULT,
    BLACKLIST_ONLY,
    WHITELIST_AND_DEFAULT,
    WHITELIST_ONLY,
    DEFAULT;

    public final String translationKey = "text.autoconfig.soulbound.option.compatibilityMode." + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name());

    @Override
    public String toString() {
        return Text.translatable(translationKey).getString();
    }

    @Override
    public String asString() {
        return this.toString();
    }
}
