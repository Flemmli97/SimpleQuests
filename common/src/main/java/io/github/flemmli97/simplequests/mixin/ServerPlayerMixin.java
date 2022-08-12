package io.github.flemmli97.simplequests.mixin;

import com.mojang.authlib.GameProfile;
import io.github.flemmli97.simplequests.gui.QuestGui;
import io.github.flemmli97.simplequests.player.PlayerData;
import io.github.flemmli97.simplequests.player.SimpleQuestDataGet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.ProfilePublicKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player implements SimpleQuestDataGet {

    @Unique
    private PlayerData simplequestData;

    private ServerPlayerMixin(Level level, BlockPos blockPos, float f, GameProfile gameProfile, @Nullable ProfilePublicKey profilePublicKey) {
        super(level, blockPos, f, gameProfile, profilePublicKey);
    }

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

    @Inject(method = "tick", at = @At("RETURN"))
    private void guiUpdate(CallbackInfo info) {
        if (this.containerMenu instanceof QuestGui gui && this.tickCount % 20 == 0) {
            gui.update();
        }
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
