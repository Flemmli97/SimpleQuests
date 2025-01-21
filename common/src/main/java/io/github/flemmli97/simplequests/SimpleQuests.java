package io.github.flemmli97.simplequests;

import io.github.flemmli97.simplequests_api.quest.QuestBase;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SimpleQuests {

    public static final String MODID = "simplequests";

    public static final Logger LOGGER = LogManager.getLogger("simplequests");

    public static boolean FTB_RANKS;
    public static boolean PERMISSION_API;

    public static boolean canAcceptQuest(CommandSourceStack src, QuestBase base) {
        return (LoaderHandler.INSTANCE.hasPerm(src, QuestCommandPerms.ACCEPTADMIN, true) || base.getVisibility() != QuestBase.Visibility.NEVER)
                && base.category.matchesContext(null);
    }

    public static boolean canAcceptQuest(ServerPlayer src, QuestBase base) {
        return (LoaderHandler.INSTANCE.hasPerm(src, QuestCommandPerms.ACCEPTADMIN, true) || base.getVisibility() != QuestBase.Visibility.NEVER)
                && base.category.matchesContext(null);
    }
}