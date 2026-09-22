package com.alter.tickkeeper.catchup;

import com.alter.tickkeeper.Config;
import com.alter.tickkeeper.TickKeepr;
import com.alter.tickkeeper.capability.CapabilityHandler;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Holds every block entity that still owes some amount of "replay" ticks,
 * and works through them a little at a time on the main server thread.
 *
 * <p>Simulating is just calling the block's own {@code tick()} method
 * repeatedly - the same method the game itself calls every tick - so the
 * result is exactly what would have happened had the chunk stayed loaded
 * the whole time (recipe lookups, output-slot-full stalls, fuel consumption
 * and all). The only inaccuracy comes from the deliberate lazy tax and cap
 * in {@link Config}, plus whatever cross-block effects a machine's tick()
 * happens to skip when neighbors aren't ticking either - both acceptable
 * given the accuracy isn't meant to be perfect.
 *
 * <p>Work is spread across many real server ticks (see {@link Config#SERVER_TICK_BUDGET})
 * instead of being done all at once in the chunk-load event, so a big
 * backlog (e.g. a base full of machines after a multi-day absence) doesn't
 * freeze the server for everyone the moment the world loads.
 *
 * <p>Also holds a separate queue of block entities that were just found in
 * a newly-loaded chunk and still need their {@code getCapability()} check
 * and "how far behind" math done. That deliberately does NOT happen during
 * the chunk load event itself (see {@link ChunkLoadHandler}) - it happens
 * here, during normal server-tick processing, where it's safe even if a
 * particular mod's tile entity does something slow or world-touching in
 * its own getCapability().
 */
@Mod.EventBusSubscriber(modid = TickKeepr.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CatchUpManager {

    private static final long MILLIS_PER_TICK = 50L;

    /** Cap on how many freshly-loaded block entities get their capability
     *  checked per server tick, so a chunk packed with machines - or one
     *  with a slow getCapability() - can't spike a single tick. */
    private static final int MAX_EVALUATIONS_PER_TICK = 50;

    private static final Deque<Job> QUEUE = new ArrayDeque<>();
    private static final Deque<WeakReference<TileEntity>> EVALUATION_QUEUE = new ArrayDeque<>();

    private CatchUpManager() {
    }

    public static void enqueue(TileEntity tileEntity, long ticks) {
        if (ticks <= 0) {
            return;
        }
        QUEUE.addLast(new Job(tileEntity, ticks));
    }

    /** Queues a freshly-loaded tile entity for its capability check and
     *  catch-up math, done later on a normal server tick rather than here. */
    public static void enqueueForEvaluation(TileEntity tileEntity) {
        EVALUATION_QUEUE.addLast(new WeakReference<>(tileEntity));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        evaluatePending();

        if (QUEUE.isEmpty()) {
            return;
        }

        long budget = Config.SERVER_TICK_BUDGET.get();

        while (budget > 0 && !QUEUE.isEmpty()) {
            Job job = QUEUE.peekFirst();
            TileEntity te = job.tileEntity.get();

            if (te == null || te.isRemoved() || te.getLevel() == null) {
                QUEUE.pollFirst();
                continue;
            }

            long slice = Math.min(budget, job.remainingTicks);

            if (te instanceof ITickableTileEntity) {
                ITickableTileEntity tickable = (ITickableTileEntity) te;
                try {
                    for (long i = 0; i < slice && !te.isRemoved(); i++) {
                        tickable.tick();
                    }
                } catch (Exception e) {
                    TickKeepr.LOGGER.warn(
                            "TickKeepr: catch-up simulation threw an exception for {} at {} - stopping catch-up for this block.",
                            te.getClass().getName(), te.getBlockPos(), e);
                    job.remainingTicks = 0;
                }
            }

            job.remainingTicks -= slice;
            budget -= slice;

            if (job.remainingTicks <= 0) {
                QUEUE.pollFirst();
            }
        }
    }

    private static void evaluatePending() {
        if (EVALUATION_QUEUE.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        long minMillis = Config.MIN_TICKS_TO_SIMULATE.get() * MILLIS_PER_TICK;
        double taxPercent = Config.LAZY_TAX_PERCENT.get();
        long maxTicks = Config.MAX_CATCH_UP_TICKS.get();

        int evaluated = 0;
        while (evaluated < MAX_EVALUATIONS_PER_TICK && !EVALUATION_QUEUE.isEmpty()) {
            TileEntity tileEntity = EVALUATION_QUEUE.pollFirst().get();
            evaluated++;

            if (tileEntity == null || tileEntity.isRemoved() || tileEntity.getLevel() == null) {
                continue;
            }

            tileEntity.getCapability(CapabilityHandler.LAST_ACTIVE).ifPresent(lastActive -> {
                long elapsedMillis = now - lastActive.getLastActiveTime();
                if (elapsedMillis < minMillis) {
                    return;
                }

                long elapsedTicks = elapsedMillis / MILLIS_PER_TICK;
                elapsedTicks -= Math.round(elapsedTicks * (taxPercent / 100.0));

                if (elapsedTicks > maxTicks) {
                    elapsedTicks = maxTicks;
                }

                if (elapsedTicks > 0) {
                    enqueue(tileEntity, elapsedTicks);
                }
            });
        }
    }

    private static final class Job {
        final WeakReference<TileEntity> tileEntity;
        long remainingTicks;

        Job(TileEntity tileEntity, long remainingTicks) {
            this.tileEntity = new WeakReference<>(tileEntity);
            this.remainingTicks = remainingTicks;
        }
    }
}
