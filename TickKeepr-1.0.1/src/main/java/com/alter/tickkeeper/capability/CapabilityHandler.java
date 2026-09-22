package com.alter.tickkeeper.capability;

import com.alter.tickkeeper.TickKeepr;
import net.minecraft.nbt.INBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Mod.EventBusSubscriber(modid = TickKeepr.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CapabilityHandler {

    @CapabilityInject(ILastActive.class)
    public static Capability<ILastActive> LAST_ACTIVE = null;

    private static final ResourceLocation ID = new ResourceLocation(TickKeepr.MOD_ID, "last_active");

    /** Called once from TickKeepr's FMLCommonSetupEvent handler, before anything can ask for the capability. */
    public static void register() {
        CapabilityManager.INSTANCE.register(ILastActive.class, new LastActiveCapability.Storage(), LastActiveCapability::new);
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<TileEntity> event) {
        // Only tickable block entities can ever be fast-forwarded, so only
        // they need this capability attached at all.
        if (event.getObject() instanceof ITickableTileEntity) {
            event.addCapability(ID, new Provider());
        }
    }

    private static class Provider implements ICapabilitySerializable<INBT> {

        private final LastActiveCapability instance = new LastActiveCapability();
        private final LazyOptional<ILastActive> holder = LazyOptional.of(() -> instance);

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction side) {
            return capability == LAST_ACTIVE ? holder.cast() : LazyOptional.empty();
        }

        @Override
        public INBT serializeNBT() {
            return LAST_ACTIVE.writeNBT(instance, null);
        }

        @Override
        public void deserializeNBT(INBT nbt) {
            LAST_ACTIVE.readNBT(instance, null, nbt);
        }
    }
}
