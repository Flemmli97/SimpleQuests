package io.github.flemmli97.simplequests.player;

import com.google.common.collect.ImmutableList;
import io.github.flemmli97.simplequests.SimpleQuests;
import io.github.flemmli97.simplequests.datapack.QuestsManager;
import io.github.flemmli97.simplequests.quest.Quest;
import io.github.flemmli97.simplequests.quest.QuestEntry;
import io.github.flemmli97.simplequests.quest.QuestEntryImpls;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QuestProgress {

    private final List<String> entries = new ArrayList<>();

    private final Map<ResourceLocation, Integer> killCounter = new HashMap<>();

    private Quest quest;

    public QuestProgress(Quest quest) {
        this.quest = quest;
    }

    public QuestProgress(CompoundTag tag) {
        this.load(tag);
    }

    public Quest getQuest() {
        return this.quest;
    }

    public SubmitType submit(ServerPlayer player) {
        boolean any = false;
        for (Map.Entry<String, QuestEntry> entry : this.quest.entries.entrySet()) {
            if (this.entries.contains(entry.getKey()))
                continue;
            if (entry.getValue().submit(player)) {
                this.entries.add(entry.getKey());
                any = true;
            }
        }
        boolean b = this.isCompleted();
        return b ? SubmitType.COMPLETE : any ? SubmitType.PARTIAL : SubmitType.NOTHING;
    }

    public Set<QuestEntry> onKill(ServerPlayer player, LivingEntity entity) {
        Set<QuestEntry> fullfilled = new HashSet<>();
        for (Map.Entry<String, QuestEntry> e : this.quest.entries.entrySet()) {
            if (e.getValue() instanceof QuestEntryImpls.KillEntry killEntry) {
                if(this.entries.contains(e.getKey()))
                    continue;
                if (killEntry.entity().equals(SimpleQuests.getHandler().fromEntity(entity))) {
                    int killed = this.killCounter.compute(killEntry.entity(), (res, i) -> i == null ? 1 : ++i);
                    if (killed >= killEntry.amount()) {
                        fullfilled.add(killEntry);
                        this.entries.add(e.getKey());
                    }
                }
            }
        }
        return fullfilled;
    }

    public boolean isCompleted() {
        return this.entries.containsAll(this.quest.entries.keySet());
    }

    public int killedOf(ResourceLocation loc) {
        return this.killCounter.getOrDefault(loc, 0);
    }

    public List<String> finishedTasks() {
        return ImmutableList.copyOf(this.entries);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Quest", this.quest.id.toString());
        ListTag list = new ListTag();
        this.entries.forEach(res -> list.add(StringTag.valueOf(res)));
        tag.put("FinishedEntries", list);
        CompoundTag kills = new CompoundTag();
        this.killCounter.forEach((res, i) -> kills.putInt(res.toString(), i));
        tag.put("KillCounter", kills);
        return tag;
    }

    public void load(CompoundTag tag) {
        this.quest = QuestsManager.instance().getQuests().get(new ResourceLocation(tag.getString("Quest")));
        if (this.quest == null)
            SimpleQuests.logger.error("Cant find quest with id " + tag.getString("Quest"));
        ListTag list = tag.getList("FinishedEntries", Tag.TAG_STRING);
        list.forEach(t -> this.entries.add(t.getAsString()));
        CompoundTag kills = tag.getCompound("KillCounter");
        kills.getAllKeys().forEach(key -> this.killCounter.put(new ResourceLocation(key), kills.getInt(key)));
    }

    enum SubmitType {
        COMPLETE,
        PARTIAL,
        NOTHING
    }
}
