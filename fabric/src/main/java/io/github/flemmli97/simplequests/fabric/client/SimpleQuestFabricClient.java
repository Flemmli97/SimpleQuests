package io.github.flemmli97.simplequests.fabric.client;

import io.github.flemmli97.simplequests.client.ClientHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class SimpleQuestFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.JOIN.register(((handler, sender, client) -> ClientHandler.onLogin()));
    }
}
