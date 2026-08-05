package me.duncanruns.themis.mixinint;

import me.duncanruns.themis.RNGManager;
import me.duncanruns.themis.SpawnerManager;

public interface RerollerServer {
    RNGManager themis$getRNGManager();

    SpawnerManager themis$getSpawnerManager();
}
