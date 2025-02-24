package io.github.flemmli97.simplequests_api.fabric;

import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.datapack.QuestsManager;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

public class Reloader extends QuestsManager implements IdentifiableResourceReloadListener {

    @Override
    public ResourceLocation getFabricId() {
        return new ResourceLocation(SimpleQuestsAPI.MODID, "reloader");
    }

    @Override
    public void apply(ResourceResult object, ResourceManager resourceManager, ProfilerFiller profiler) {
        QuestsManager.instance().apply(object, resourceManager, profiler);
    }
}
