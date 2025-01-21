package io.github.flemmli97.simplequests_api.fabric;

import io.github.flemmli97.simplequests_api.registry.ProgressionTrackerRegistry;
import io.github.flemmli97.simplequests_api.registry.QuestBaseRegistry;
import io.github.flemmli97.simplequests_api.registry.QuestEntryRegistry;
import io.github.flemmli97.simplequests_api.util.QuestNumberProvider;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;

public class SimpleQuestsAPIFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new Reloader());
        QuestBaseRegistry.register();
        QuestEntryRegistry.register();
        ProgressionTrackerRegistry.register();
        QuestNumberProvider.init();
    }
}
