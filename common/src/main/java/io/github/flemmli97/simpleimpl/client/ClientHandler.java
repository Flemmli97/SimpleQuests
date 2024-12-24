package io.github.flemmli97.simpleimpl.client;

import io.github.flemmli97.simpleimpl.network.C2SNotify;
import io.github.flemmli97.simplequests_api.SimpleQuests;

public class ClientHandler {

    public static void onLogin() {
        SimpleQuests.getHandler().sendToServer(new C2SNotify());
    }
}
