package io.github.flemmli97.simplequests_api.impls.progression;

import io.github.flemmli97.simplequests_api.SimpleQuestsAPI;
import io.github.flemmli97.simplequests_api.impls.tasks.EntityInteractTask;
import io.github.flemmli97.simplequests_api.player.ProgressionTrackerKey;
import io.github.flemmli97.simplequests_api.player.QuestProgress;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class EntityTracker extends ProgressionTrackerBase<Entity, EntityInteractTask.EntityInteractTaskResolved> {

    public static final String ENTITY_INTERACT_PROGRESS = EntityInteractTask.ID + ".progress";
    public static final ProgressionTrackerKey<Entity, EntityInteractTask.EntityInteractTaskResolved> KEY = new ProgressionTrackerKey<>(SimpleQuestsAPI.MODID, "entity_tracker",
            EntityInteractTask.ID);

    private final Set<UUID> entities = new HashSet<>();

    public EntityTracker(EntityInteractTask.EntityInteractTaskResolved questEntry) {
        super(questEntry);
    }

    @Override
    public boolean progress(ServerPlayer player, QuestProgress prog, Entity with) {
        if (this.questEntry().check(player, with)) {
            if (this.entities.contains(with.getUUID())) {
                if (!prog.getQuest().category.isSilent)
                    player.sendMessage(new TranslatableComponent("simplequests.interaction.dupe").withStyle(ChatFormatting.DARK_RED), Util.NIL_UUID);
                return false;
            }
            this.entities.add(with.getUUID());
            return this.entities.size() >= this.questEntry().amount();
        }
        return false;
    }

    @Override
    public MutableComponent formattedProgress(ServerPlayer player, QuestProgress progress) {
        float perc = this.entities.size() / (float) this.questEntry().amount();
        ChatFormatting form = ChatFormatting.DARK_GREEN;
        if (perc <= 0.35) {
            form = ChatFormatting.DARK_RED;
        } else if (perc <= 0.7) {
            form = ChatFormatting.GOLD;
        }
        return new TranslatableComponent(ENTITY_INTERACT_PROGRESS, this.entities.size(), this.questEntry().amount()).withStyle(form);
    }

    @Override
    public Tag save() {
        ListTag list = new ListTag();
        this.entities.forEach(uuid -> list.add(NbtUtils.createUUID(uuid)));
        return list;
    }

    @Override
    public void load(Tag tag) {
        try {
            ListTag list = (ListTag) tag;
            list.forEach(t -> this.entities.add(NbtUtils.loadUUID(t)));
        } catch (ClassCastException ignored) {
        }
    }
}
