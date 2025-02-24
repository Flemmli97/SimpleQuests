package io.github.flemmli97.simplequests.forge.data;

import io.github.flemmli97.simplequests.SimpleQuests;
import net.minecraft.data.DataGenerator;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = SimpleQuests.MODID, bus = EventBusSubscriber.Bus.MOD)
public class DataGen {

    @SubscribeEvent
    public static void data(GatherDataEvent event) {
        DataGenerator data = event.getGenerator();
        data.addProvider(event.includeClient(), new LangGen(data));
    }
}
