package me.duncanruns.themis.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.Dynamic;
import me.duncanruns.themis.mixinint.ThemisTagOwner;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.LevelProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelProperties.class)
public abstract class LevelPropertiesMixin implements ThemisTagOwner {
    @Unique
    private CompoundTag themisTag = new CompoundTag();

    @Inject(method = "method_29029", at = @At("RETURN"))
    private static void onReadLevelDat(CallbackInfoReturnable<LevelProperties> cir, @Local(argsOnly = true) Dynamic<Tag> dynamic) {
        CompoundTag mainTag = ((CompoundTag) dynamic.getValue());
        if (mainTag.getKeys().contains("Themis")) {
            LevelProperties lp = cir.getReturnValue();
            ((LevelPropertiesMixin) (Object) lp).themisTag = mainTag.getCompound("Themis");
        }
    }

    @Inject(method = "updateProperties", at = @At("TAIL"))
    private void onSaveLevelDat(CallbackInfo ci, @Local(argsOnly = true, ordinal = 0) CompoundTag compoundTag) {
        compoundTag.put("Themis", themisTag);
    }


    @Override
    public void themis$setTag(CompoundTag tag) {
        themisTag = tag;
    }

    @Override
    public CompoundTag themis$getTag() {
        return themisTag;
    }
}
