package io.github.flemmli97.simplequests_api.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.impls.quests.CompositeQuest;
import io.github.flemmli97.simplequests_api.impls.quests.Quest;
import io.github.flemmli97.simplequests_api.impls.quests.SequentialQuest;
import io.github.flemmli97.simplequests_api.quest.QuestBase;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class QuestBaseRegistry {

    private static final Map<ResourceLocation, QuestCodec<? extends QuestBase>> MAP = new HashMap<>();

    public static final CodecContext DEFAULT = new CodecContext(ResourceLocation.fromNamespaceAndPath(SimpleQuestsAPI.MODID, "default"), false, false);
    public static final CodecContext WITH_ID = new CodecContext(ResourceLocation.fromNamespaceAndPath(SimpleQuestsAPI.MODID, "with_id"), true, false);
    public static final CodecContext DATA_LOAD = new CodecContext(ResourceLocation.fromNamespaceAndPath(SimpleQuestsAPI.MODID, "data_load"), true, false);
    public static final CodecContext DATAGEN = new CodecContext(ResourceLocation.fromNamespaceAndPath(SimpleQuestsAPI.MODID, "full"), false, true);

    public static final Function<CodecContext, Codec<QuestBase>> CODEC = Util.memoize(codecType ->
            ResourceLocation.CODEC.dispatch(QuestBase.TYPE_ID, QuestBase::getTypeId, type -> getCodec(type, codecType)));

    public static void register() {
        registerSerializer(Quest.ID, Quest.CODEC);
        registerSerializer(CompositeQuest.ID, CompositeQuest.CODEC);
        registerSerializer(SequentialQuest.ID, SequentialQuest.CODEC);
    }

    /**
     * Register a deserializer for a {@link QuestBase}
     * The context allows to conditionally serialize data
     */
    public static synchronized <T extends QuestBase> void registerSerializer(ResourceLocation id, Function<CodecContext, MapCodec<T>> codec) {
        if (MAP.containsKey(id))
            throw new IllegalStateException("Deserializer for " + id + " already registered");
        MAP.put(id, new QuestCodec<>(codec));
    }

    private static MapCodec<? extends QuestBase> getCodec(ResourceLocation res, CodecContext type) {
        QuestCodec<? extends QuestBase> d = MAP.get(res);
        // Legacy
        if (d == null && res.getNamespace().equals("simplequests"))
            d = MAP.get(ResourceLocation.fromNamespaceAndPath(SimpleQuestsAPI.MODID, res.getPath()));
        if (d != null)
            return d.get(type);
        throw new IllegalStateException("Missing entry for key " + res);
    }

    private static class QuestCodec<T extends QuestBase> {

        private final Map<CodecContext, MapCodec<T>> codecCache = new HashMap<>();
        private final Function<CodecContext, MapCodec<T>> codecs;

        private QuestCodec(Function<CodecContext, MapCodec<T>> codecs) {
            this.codecs = codecs;
        }

        protected MapCodec<T> get(CodecContext type) {
            return this.codecCache.computeIfAbsent(type, this.codecs);
        }
    }

    public static final class CodecContext {

        private final ResourceLocation id;
        private final boolean withId, full;

        public CodecContext(ResourceLocation id, boolean withId, boolean full) {
            this.id = id;
            this.withId = withId;
            this.full = full;
        }

        public boolean withId() {
            return this.withId;
        }

        public boolean full() {
            return this.full;
        }

        @Override
        public int hashCode() {
            return this.id.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this)
                return true;
            if (obj instanceof CodecContext ctx)
                return ctx.id.equals(this.id);
            return false;
        }
    }
}
