package io.github.flemmli97.simplequests.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public interface SQPacket {

    void write(FriendlyByteBuf buf);

    ResourceLocation getID();
}
