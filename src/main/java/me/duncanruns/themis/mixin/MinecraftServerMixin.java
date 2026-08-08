package me.duncanruns.themis.mixin;

import me.duncanruns.themis.RNGManager;
import me.duncanruns.themis.SpawnerManager;
import me.duncanruns.themis.mixinint.RerollerServer;
import me.duncanruns.themis.mixinint.ThemisTagOwner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements RerollerServer {
    @Unique
    private RNGManager rngManager;

    @Unique
    private SpawnerManager spawnerManager;

    @Inject(method = "loadWorld", at = @At("RETURN"))
    private void loadRerollerData(CallbackInfo ci) {
        @SuppressWarnings("DataFlowIssue")
        MinecraftServer thisServer = (MinecraftServer) (Object) this;

        rngManager = new RNGManager((thisServer).getSaveProperties().getGeneratorOptions().getSeed());
        rngManager.load(thisServer);

        spawnerManager = new SpawnerManager(rngManager);
        spawnerManager.load(thisServer);
    }

    @Inject(method = "save(ZZZ)Z", at = @At("HEAD"))
    private void saveRerollerData(CallbackInfoReturnable<Boolean> cir) {
        MinecraftServer thisServer = (MinecraftServer) (Object) this;
        CompoundTag rerollerTag = new CompoundTag();
        rerollerTag.put("RNGManager", rngManager.getRandomsTag());
        rerollerTag.put("SkullRerollers", rngManager.getSkullsTag());
        rerollerTag.put("SpawnerManager", spawnerManager.getTag());
        ((ThemisTagOwner) thisServer.getSaveProperties()).themis$setTag(rerollerTag);
    }

    @Inject(method = "tickWorlds", at = @At("HEAD"))
    private void reportFailedRerollers(CallbackInfo ci) {
        MinecraftServer thisServer = (MinecraftServer) (Object) this;
        rngManager.tick(thisServer);
    }


    @Override
    public RNGManager themis$getRNGManager() {
        return rngManager;
    }

    @Override
    public SpawnerManager themis$getSpawnerManager() {
        return spawnerManager;
    }
}