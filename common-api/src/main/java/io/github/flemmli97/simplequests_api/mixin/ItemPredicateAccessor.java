package io.github.flemmli97.simplequests_api.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.flemmli97.simplequests_api.util.PredicateTranslation;
import io.github.flemmli97.simplequests_api.util.QuestUtils;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mixin(ItemPredicate.class)
public abstract class ItemPredicateAccessor implements PredicateTranslation {

    @Shadow
    @Final
    @Nullable
    private Set<Item> items;
    @Shadow
    @Final
    @Nullable
    private TagKey<Item> tag;

    @Shadow
    public abstract JsonElement serializeToJson();

    @Unique
    private List<MutableComponent> simplequests_api$computedTranslation;
    @Unique
    private boolean simplequests_api$computed;

    @Override
    public List<MutableComponent> translation(boolean cache) {
        if ((Object) this == ItemPredicate.ANY)
            return List.of(new TextComponent(""));
        if (this.items == null && this.tag == null)
            return null;
        if (this.simplequests_api$computedTranslation != null || this.simplequests_api$computed)
            return this.simplequests_api$computedTranslation;
        this.simplequests_api$computed = cache;
        JsonElement element = this.serializeToJson();
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            boolean nonTrivial = obj.keySet().stream().anyMatch(s -> !s.equals("items") && !s.equals("tag") && !obj.get(s).isJsonNull());
            if (!nonTrivial) {
                List<MutableComponent> formattedItems = new ArrayList<>();
                if (this.items != null)
                    this.items.forEach(i -> formattedItems.add(new TranslatableComponent(i.getDescriptionId())));
                if (this.tag != null) {
                    formattedItems.add(QuestUtils.tagsComponent(this.tag, Registry.ITEM, i -> new TranslatableComponent(i.getDescriptionId())));
                }
                if (!cache)
                    return formattedItems;
                this.simplequests_api$computedTranslation = formattedItems;
            }
        }
        return this.simplequests_api$computedTranslation;
    }
}
