package com.alter.tickkeeper.catchup;

import com.alter.tickkeeper.Config;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunk;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

import static com.alter.tickkeeper.TickKeepr.MOD_ID;

/**
 * Finds every tickable, non-blacklisted block entity in a newly-loaded
 * chunk and hands each one to {@link CatchUpManager} for evaluation.
 *
 * <p>This deliberately does NOT call {@code getCapability()} here. That's a
 * normal virtual method any mod's tile entity can override, and some do so
 * with logic that queries neighboring block state - which can trigger a
 * blocking chunk load. Doing that from inside the chunk load event itself
 * (i.e. from inside the chunk manager's own load callback) can hang the
 * server, since it'd be reentrantly blocking on chunk loading from within
 * chunk loading. So the capability check and the "how far behind is it"
 * math happen later, in {@link CatchUpManager}'s normal server-tick
 * processing instead, where a slow or blocking call is merely slow, not
 * reentrant.
 */
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChunkLoadHandler {

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!Config.ENABLED.get()) {
            return;
        }

        IChunk chunk = event.getChunk();
        if (!(chunk instanceof Chunk)) {
            // Not a fully-loaded chunk (e.g. still generating) - nothing with
            // real tile entity state to catch up yet.
            return;
        }

        List<? extends String> blacklist = Config.BLOCK_ENTITY_BLACKLIST.get();

        for (TileEntity tileEntity : ((Chunk) chunk).getBlockEntities().values()) {
            if (!(tileEntity instanceof ITickableTileEntity)) {
                continue;
            }

            ResourceLocation registryName = tileEntity.getType().getRegistryName();
            if (registryName != null && blacklist.contains(registryName.toString())) {
                continue;
            }

            CatchUpManager.enqueueForEvaluation(tileEntity);
        }
    }
}
