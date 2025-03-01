package io.github.flemmli97.simplequests_api;

import io.github.flemmli97.simplequests_api.player.PlayerQuestData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

public class SimpleQuestsAPI {

    public static final String MODID = "simplequests_api";

    public static final Logger LOGGER = LogManager.getLogger("simplequests_api");

    public static LootContext createContext(PlayerQuestData data, @Nullable ResourceLocation quest) {
        ServerPlayer player = data.getPlayer();
        LootParams params = new LootParams.Builder(player.serverLevel())
                .withParameter(LootContextParams.ORIGIN, player.position())
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .create(LootContextParamSets.ADVANCEMENT_ENTITY);
        return new LootContext.Builder(params).withOptionalRandomSeed(data.getRandomSeed(quest)).create(null);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getPlatformInstance(Class<T> abstractClss, String... impls) {
        if (impls == null || impls.length == 0)
            throw new IllegalStateException("Couldn't create an instance of " + abstractClss + ". No implementations provided!");
        Class<?> clss = null;
        int i = 0;
        while (clss == null && i < impls.length) {
            try {
                clss = Class.forName(impls[i]);
            } catch (ClassNotFoundException ignored) {
            }
            i++;
        }
        if (clss == null)
            SimpleQuestsAPI.LOGGER.fatal("No Implementation of {} found with given paths {}", abstractClss, Arrays.toString(impls));
        else if (abstractClss.isAssignableFrom(clss)) {
            try {
                Constructor<T> constructor = (Constructor<T>) clss.getDeclaredConstructor();
                return constructor.newInstance();
            } catch (NoSuchMethodException e) {
                SimpleQuestsAPI.LOGGER.fatal("Implementation of {} needs to provide an no arg constructor", clss);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                SimpleQuestsAPI.LOGGER.error(e);
            }
        }
        throw new IllegalStateException("Couldn't create an instance of " + abstractClss);
    }
}
