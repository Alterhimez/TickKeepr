package com.alter.tickkeeperbottle;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * TickKeepr: Bottled Time.
 *
 * <p>haoict's "Time in a Bottle" (mod id {@code tiab}) already does the live
 * side of this perfectly well on its own - carry the bottle, it fills up a
 * tick at a time while you play. What it structurally can't do is know how
 * long you were offline, since nothing ticks while the world is closed.
 *
 * <p>This mod is just the missing other half: it stamps the real-world time
 * on logout, and on the next login works out how long that gap was and
 * credits it straight into any {@code tiab:timeinabottle} stack sitting in
 * your inventory - using the exact NBT layout tiab's own item reads, so
 * from the item's point of view it just looks like time that accrued while
 * you were gone.
 *
 * <p>No new items, blocks, or dependency on tiab's code - see
 * {@link TiabBridge} for how that's done through the registry name and NBT
 * alone, and {@link com.alter.tickkeeperbottle.event.OfflineCreditHandler}
 * for the login/logout hooks.
 */
@Mod(TickKeeprBottle.MOD_ID)
public class TickKeeprBottle {

    public static final String MOD_ID = "tickkeeperbottle";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public TickKeeprBottle() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, BottleConfig.SPEC);
        LOGGER.info("TickKeepr: Bottled Time loaded - will credit offline time into Time in a Bottle on login, if it's installed.");
    }
}
