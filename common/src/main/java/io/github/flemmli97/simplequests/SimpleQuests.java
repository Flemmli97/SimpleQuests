package io.github.flemmli97.simplequests;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SimpleQuests {

    public static final String MODID = "simplequests";

    public static final Logger logger = LogManager.getLogger("simplequests");

    private static LoaderHandler handler;

    public static boolean ftbRanks;
    public static boolean permissionAPI;

    public static void updateLoaderImpl(LoaderHandler impl) {
        handler = impl;
    }

    public static LoaderHandler getHandler() {
        return handler;
    }
}
