package com.alter.tickkeeper;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

public class Config {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Master switch for catch-up simulation.")
            .define("enabled", true);

    public static final ForgeConfigSpec.IntValue MAX_CATCH_UP_TICKS = BUILDER
            .comment(
                    "Hard cap on how many game ticks (20 = 1 second) a single block",
                    "will ever be fast-forwarded by, no matter how long it was",
                    "actually unloaded/offline for. Keeps a multi-day absence from",
                    "freezing/stalling the server for everyone when the world loads.",
                    "Default: 1,728,000 ticks = 24 in-game hours.")
            .defineInRange("maxCatchUpTicks", 1_728_000, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue LAZY_TAX_PERCENT = BUILDER
            .comment(
                    "Percentage of the calculated catch-up time to discard before",
                    "simulating, so machines never come back at a suspiciously exact",
                    "100% real-time accuracy. 15 means only 85% of the elapsed real",
                    "time actually gets simulated.")
            .defineInRange("lazyTaxPercent", 15.0, 0.0, 100.0);

    public static final ForgeConfigSpec.IntValue MIN_TICKS_TO_SIMULATE = BUILDER
            .comment("Ignore gaps shorter than this many ticks (20 = 1 second) - not worth the overhead.")
            .defineInRange("minTicksToSimulate", 20, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.IntValue SERVER_TICK_BUDGET = BUILDER
            .comment(
                    "How many total simulated ticks TickKeepr will run per real",
                    "server tick, spread across every block currently catching up.",
                    "Higher = machines finish catching up faster after you load in,",
                    "but risks a brief stutter if a lot of blocks need it at once.",
                    "Lower = smoother, but machines take visibly longer to catch up.")
            .defineInRange("serverTickBudget", 20_000, 100, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLOCK_ENTITY_BLACKLIST = BUILDER
            .comment(
                    "Block entity registry names (e.g. \"minecraft:hopper\") that should",
                    "never be fast-forwarded, in case some specific machine misbehaves",
                    "when ticked rapidly with no player/world interaction in between.",
                    "Empty by default.")
            .defineList("blockEntityBlacklist", new ArrayList<>(), o -> o instanceof String);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
