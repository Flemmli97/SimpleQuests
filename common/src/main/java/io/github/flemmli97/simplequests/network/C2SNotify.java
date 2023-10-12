package io.github.flemmli97.simplequests.network;

import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.player.PlayerData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Packet to notify the server this player has SimpleQuests on the client
 */
public record C2SNotify() implements SQPacket {

    public static final ResourceLocation ID = new ResourceLocation(SimpleQuests.MODID, "c2s_notify");

    public static C2SNotify read(FriendlyByteBuf buf) {
        return new C2SNotify();
    }

    public static void handle(C2SNotify pkt, ServerPlayer sender) {
        if (sender != null) {
            PlayerData.get(sender).hasClient = true;
        }
    }

    @Override
    public void write(FriendlyByteBuf buf) {
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }
}
