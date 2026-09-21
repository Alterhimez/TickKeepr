package com.alter.tickkeeper.capability;

import net.minecraft.nbt.INBT;
import net.minecraft.nbt.LongNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;

public class LastActiveCapability implements ILastActive {

    // Defaults to "now" so that a brand-new block entity - or one loaded from
    // a save that predates this mod being installed, which will have no
    // stored value at all - is never mistaken for having been offline since
    // the epoch and queued for a huge (if capped) catch-up.
    private long lastActiveEpochMillis = System.currentTimeMillis();

    @Override
    public long getLastActiveTime() {
        return lastActiveEpochMillis;
    }

    @Override
    public void setLastActiveTime(long epochMillis) {
        this.lastActiveEpochMillis = epochMillis;
    }

    /**
     * Bridges this capability to the chunk/tile entity NBT round-trip that
     * Forge already does for every attached capability (the "ForgeCaps" tag
     * on {@code TileEntity#save}/{@code load}), so this works for a block
     * entity from any mod without needing to touch that mod's own read/write
     * code at all.
     */
    public static class Storage implements Capability.IStorage<ILastActive> {

        @Override
        public INBT writeNBT(Capability<ILastActive> capability, ILastActive instance, Direction side) {
            // Deliberately ignore whatever the in-memory value currently is
            // and stamp "now" instead. This method only ever runs while the
            // owning block entity is actually loaded and about to be saved
            // (autosave, chunk unload, or world shutdown), so "now" is by
            // definition the most recent moment we know it was still active.
            return LongNBT.valueOf(System.currentTimeMillis());
        }

        @Override
        public void readNBT(Capability<ILastActive> capability, ILastActive instance, Direction side, INBT nbt) {
            instance.setLastActiveTime(((LongNBT) nbt).getAsLong());
        }
    }
}
