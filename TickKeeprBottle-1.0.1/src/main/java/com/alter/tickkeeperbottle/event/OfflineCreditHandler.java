package com.alter.tickkeeperbottle.event;

import com.alter.tickkeeperbottle.BottleConfig;
import com.alter.tickkeeperbottle.TiabBridge;
import com.alter.tickkeeperbottle.TickKeeprBottle;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The whole mod, really: stamp "now" into the player's own persistent data
 * when they log out, then on their next login compare that stamp to the new
 * "now" to find out how long they were away, and hand the result to every
 * Time in a Bottle stack currently in their inventory.
 *
 * <p>Tracking the checkpoint on the player rather than on each bottle means
 * a bottle only gets credited if it was actually in your inventory at
 * login - one sitting untouched in a chest the whole time doesn't
 * retroactively fill up.
 */
@Mod.EventBusSubscriber(modid = TickKeeprBottle.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class OfflineCreditHandler {

    private static final long MILLIS_PER_TICK = 50L;
    private static final String TAG_LAST_LOGOUT = "tickkeeperbottle_lastLogout";

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        event.getPlayer().getPersistentData().putLong(TAG_LAST_LOGOUT, System.currentTimeMillis());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!BottleConfig.ENABLED.get()) {
            return;
        }

        PlayerEntity player = event.getPlayer();
        CompoundNBT persistentData = player.getPersistentData();

        if (!persistentData.contains(TAG_LAST_LOGOUT)) {
            // Never seen this player log out with this mod installed before
            // (brand new world, or the mod was just added) - nothing to
            // credit yet, just start tracking from here.
            return;
        }

        long now = System.currentTimeMillis();
        long lastLogout = persistentData.getLong(TAG_LAST_LOGOUT);
        long elapsedMillis = now - lastLogout;
        if (elapsedMillis <= 0) {
            return;
        }

        long elapsedTicks = elapsedMillis / MILLIS_PER_TICK;

        double taxPercent = BottleConfig.OFFLINE_LAZY_TAX_PERCENT.get();
        if (taxPercent > 0) {
            elapsedTicks -= Math.round(elapsedTicks * (taxPercent / 100.0));
        }
        if (elapsedTicks <= 0) {
            return;
        }

        long cap = BottleConfig.MAX_STORED_TICKS.get();

        for (int i = 0; i < player.inventory.getContainerSize(); i++) {
            ItemStack stack = player.inventory.getItem(i);
            if (TiabBridge.isTimeBottle(stack)) {
                TiabBridge.addStoredTicks(stack, elapsedTicks, cap);
            }
        }
    }
}
