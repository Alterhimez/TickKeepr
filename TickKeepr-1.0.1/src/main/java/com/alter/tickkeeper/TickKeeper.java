package com.alter.tickkeeper;

import com.alter.tickkeeper.capability.CapabilityHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * TickKeepr simulates offline / unloaded-chunk progress for tickable block
 * entities.
 *
 * <p>Rather than re-implementing the internal recipe/progress math of every
 * possible machine (vanilla furnaces, Tinkers' Construct smelteries, FTB
 * Sluice, anything nudged by Time in a Bottle, etc.), it does the same thing
 * those blocks' own code already does every tick: it just calls their real
 * {@code tick()} method again, many times in a row, to "fast forward" them
 * through however many ticks they missed. That means it needs zero
 * compile-time knowledge of any other mod - it works with whatever tickable
 * machines happen to be in the pack.
 *
 * <p>See the {@code catchup} package for the actual replay/queue logic and
 * {@code capability} for how "when was this block last active" is tracked
 * and persisted.
 */
@Mod(TickKeepr.MOD_ID)
public class TickKeepr {

    public static final String MOD_ID = "tickkeeper";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public TickKeepr() {
        // NOTE: capability registration must NOT happen here. At the point the
        // mod constructor runs, Forge hasn't necessarily finished its own
        // internal @CapabilityInject scan yet (CapabilityManager's callback
        // map can still be null), so calling CapabilityManager.register()
        // this early can throw a NullPointerException. FMLCommonSetupEvent
        // fires only after every mod's constructor (and Forge's own
        // bootstrap) has completed, so it's the safe place for this.
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        LOGGER.info("TickKeepr loaded - tickable machines will catch up on load.");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        CapabilityHandler.register();
    }
}
