package io.github.flemmli97.simplequests.mixin;

import io.github.flemmli97.simplequests.player.PlayerData;
import io.github.flemmli97.simplequests.player.SimpleQuestDataGet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin implements SimpleQuestDataGet {

    @Unique
    private PlayerData simplequestData;

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void initData(CallbackInfo info) {
        this.simplequestData = new PlayerData((ServerPlayer) (Object) this);
    }

    @Inject(method = "restoreFrom", at = @At("RETURN"))
    private void copyOld(ServerPlayer oldPlayer, boolean alive, CallbackInfo info) {
        this.simplequestData.clone(PlayerData.get(oldPlayer));
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void save(CompoundTag compound, CallbackInfo info) {
        this.simplequestData.load(compound.getCompound("SimpleQuestData"));
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void read(CompoundTag compound, CallbackInfo info) {
        compound.put("SimpleQuestData", this.simplequestData.save());
    }

    @Override
    public PlayerData simpleQuestPlayerData() {
        return this.simplequestData;
    }
}
