package me.duncanruns.themis.mixinint;

import net.minecraft.nbt.CompoundTag;

public interface ThemisTagOwner {
    CompoundTag themis$getTag();

    void themis$setTag(CompoundTag tag);
}
