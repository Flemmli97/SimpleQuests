package io.github.flemmli97.simplequests.fabric;

import com.google.gson.JsonElement;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.datapack.QuestsManager;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

public class Reloader extends QuestsManager implements IdentifiableResourceReloadListener {

    @Override
    public ResourceLocation getFabricId() {
        return new ResourceLocation(SimpleQuests.MODID, "reloader");
    }

    @Override
    public void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        QuestsManager.instance().apply(object, resourceManager, profiler);
    }
}
