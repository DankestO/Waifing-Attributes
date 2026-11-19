package com.waifing.waifingattributes.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** CLIENT configuration (config/waifingattributes-client.toml): only how THIS player sees things. Never affects gameplay. */
public final class WaifingClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SHOW_UPGRADE_TOOLTIPS;
    public static final ForgeConfigSpec.BooleanValue SHOW_CATALYST_HINTS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("Tooltips of Waifing Attributes").push("tooltips");
        SHOW_UPGRADE_TOOLTIPS = b.comment("Show the upgrade state (levels, rebirths, imbue) on upgraded items.")
                .define("showUpgradeTooltips", true);
        SHOW_CATALYST_HINTS = b.comment("Show a hint line on items that work as a catalyst (which mastery uses them).")
                .define("showCatalystHints", true);
        b.pop();
        SPEC = b.build();
    }

    private WaifingClientConfig() {}
}
