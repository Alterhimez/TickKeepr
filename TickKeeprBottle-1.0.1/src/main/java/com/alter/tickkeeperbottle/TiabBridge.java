package com.alter.tickkeeperbottle;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;

/**
 * Everything needed to recognize haoict's Time in a Bottle item and credit
 * it more stored time, confirmed directly against that mod's own 1.16.5
 * source (the {@code v1.1.0} tag - the exact commit the published 1.16.5
 * jar builds from - at github.com/haoict/time-in-a-bottle):
 *
 * <ul>
 *   <li>Mod id {@code tiab}, item registered as {@code timeinabottle}
 *   ({@code com.haoict.tiab.config.Constants} / {@code ItemRegistry}).</li>
 *   <li>Stored time lives in a child compound tag {@code timeData}, under
 *   the key {@code storedTime}, as a plain {@code int} number of ticks
 *   ({@code com.haoict.tiab.config.NBTKeys}).</li>
 *   <li>20 ticks per second ({@code Constants.TICK_CONST}) - same as
 *   everywhere else in TickKeepr.</li>
 * </ul>
 *
 * <p>Deliberately just string/NBT constants rather than a compiled
 * dependency on tiab's jar, so this mod works whether or not tiab is
 * installed instead of requiring it to even build.
 */
public final class TiabBridge {

    private static final String TIAB_MOD_ID = "tiab";
    private static final ResourceLocation BOTTLE_ID = new ResourceLocation(TIAB_MOD_ID, "timeinabottle");

    private static final String TAG_TIME_DATA = "timeData";
    private static final String TAG_STORED_TIME = "storedTime";

    public static boolean isTimeBottle(ItemStack stack) {
        return !stack.isEmpty() && BOTTLE_ID.equals(stack.getItem().getRegistryName());
    }

    /** Adds {@code ticks} to whatever this bottle already has stored, capped at {@code cap}. */
    public static void addStoredTicks(ItemStack stack, long ticks, long cap) {
        if (ticks <= 0) {
            return;
        }

        CompoundNBT root = stack.getOrCreateTag();
        CompoundNBT timeData;
        if (root.contains(TAG_TIME_DATA)) {
            timeData = root.getCompound(TAG_TIME_DATA);
        } else {
            // tiab's own item creates this compound itself the moment it
            // next ticks in an inventory, but we can't count on that having
            // happened yet (e.g. a bottle that's never left a chest), so
            // create and attach it ourselves if it isn't there yet.
            timeData = new CompoundNBT();
            root.put(TAG_TIME_DATA, timeData);
        }

        long current = timeData.getInt(TAG_STORED_TIME);
        long updated = Math.min(current + ticks, Math.min(cap, Integer.MAX_VALUE));
        timeData.putInt(TAG_STORED_TIME, (int) updated);
    }

    private TiabBridge() {
    }
}
