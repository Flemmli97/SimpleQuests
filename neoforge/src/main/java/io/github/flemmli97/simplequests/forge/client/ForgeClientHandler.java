package io.github.flemmli97.simplequests.forge.client;

import io.github.flemmli97.simplequests.client.ClientHandler;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;

public class ForgeClientHandler {

    public static void login(ClientPlayerNetworkEvent.LoggingIn event) {
        ClientHandler.onLogin();
    }
}
