package io.github.flemmli97.simplequests.client;

import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.network.C2SNotify;

public class ClientHandler {

    public static void onLogin() {
        SimpleQuests.getHandler().sendToServer(new C2SNotify());
    }
}
