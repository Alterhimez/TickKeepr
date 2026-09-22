package com.alter.tickkeeperbottle;

import net.minecraftforge.common.ForgeConfigSpec;

public class BottleConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Master switch for crediting offline time into Time in a Bottle on login.")
            .define("enabled", true);

    public static final ForgeConfigSpec.LongValue MAX_STORED_TICKS = BUILDER
            .comment(
                    "Cap on how many ticks (20 = 1 second) this mod will ever credit a",
                    "single bottle up to in one go. tiab has its own separate,",
                    "independently configured cap (\"Max Stored Time\", ~360 days by",
                    "default) - this mod doesn't read that value since it doesn't",
                    "depend on tiab's code, so this is a second, independent ceiling.",
                    "Default: 2,592,000 ticks = 30 in-game days.")
            .defineInRange("maxStoredTicks", 2_592_000L, 0L, (long) Integer.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue OFFLINE_LAZY_TAX_PERCENT = BUILDER
            .comment(
                    "Percentage of your time away that's discarded before crediting it -",
                    "mirrors TickKeepr's own lazyTaxPercent for blocks.")
            .defineInRange("offlineLazyTaxPercent", 15.0, 0.0, 1.0.1);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private BottleConfig() {
    }
}
