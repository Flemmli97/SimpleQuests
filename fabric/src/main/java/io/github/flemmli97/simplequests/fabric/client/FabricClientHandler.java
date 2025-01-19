package io.github.flemmli97.simplequests.fabric.client;

import io.github.flemmli97.simplequests.network.SQPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;

public class FabricClientHandler {

    public static void sendToServer(SQPacket packet) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        packet.write(buf);
        ClientPlayNetworking.send(packet.getID(), buf);
    }
}
