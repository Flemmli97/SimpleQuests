package io.github.flemmli97.simplequests_api.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.flemmli97.simplequests_api.util.PredicateTranslation;
import io.github.flemmli97.simplequests_api.util.QuestUtils;
import net.minecraft.Util;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.EntityTypePredicate;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;

@Mixin(EntityPredicate.class)
public abstract class EntityPredicateMixin implements PredicateTranslation {

    @Shadow
    @Final
    private EntityTypePredicate entityType;

    @Shadow
    public abstract JsonElement serializeToJson();

    @Unique
    private List<MutableComponent> simplequests_api$computedTranslation;
    @Unique
    private boolean simplequests_api$computed;

    @Override
    public List<MutableComponent> translation() {
        if ((Object) this == EntityTypePredicate.ANY)
            return List.of(new TextComponent(""));
        if (this.entityType == EntityTypePredicate.ANY)
            return null;
        if (this.simplequests_api$computedTranslation != null || this.simplequests_api$computed)
            return this.simplequests_api$computedTranslation;
        this.simplequests_api$computed = true;
        JsonElement element = this.serializeToJson();
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            boolean nonTrivial = obj.keySet().stream().anyMatch(s -> !s.equals("type") && !obj.get(s).isJsonNull());
            if (!nonTrivial) {
                List<MutableComponent> formattedItems = new ArrayList<>();
                String s = this.entityType.serializeToJson().getAsString();
                if (s.startsWith("#")) {
                    formattedItems.add(QuestUtils.tagsComponent(TagKey.create(Registry.ENTITY_TYPE_REGISTRY, new ResourceLocation(s.substring(1))),
                            Registry.ENTITY_TYPE, e -> new TranslatableComponent(e.getDescriptionId())));
                } else {
                    formattedItems.add(new TranslatableComponent(Util.makeDescriptionId("entity", new ResourceLocation(s))));
                }
                this.simplequests_api$computedTranslation = formattedItems;
            }
        }
        return this.simplequests_api$computedTranslation;
    }
}
