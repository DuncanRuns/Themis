package me.duncanruns.themis.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.duncanruns.themis.RNGManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextType;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    // Implementation warning: injecting at the invoke of builder.build and LocalRef replacing the builder works in dev
    // but not in prod, so things are a little funky around here.
    @WrapOperation(method = "dropLoot", at = @At(value = "INVOKE", target = "Lnet/minecraft/loot/context/LootContext$Builder;build(Lnet/minecraft/loot/context/LootContextType;)Lnet/minecraft/loot/context/LootContext;"))
    private LootContext replaceRandom(LootContext.Builder instance, LootContextType type, Operation<LootContext> original, @Local Identifier lootTable) {
        return original.call(instance.random(RNGManager.getRandom(getServer(), lootTable.toString())), type);
    }

    @Inject(method = "dropLoot", at = @At(value = "INVOKE", target = "Lnet/minecraft/loot/LootManager;getTable(Lnet/minecraft/util/Identifier;)Lnet/minecraft/loot/LootTable;"), cancellable = true)
    private void override(DamageSource source, boolean causedByPlayer, CallbackInfo ci, @Local Identifier lootTable) {
        RNGManager.getItemOverride(getServer(), lootTable.toString()).ifPresent(stacks -> {
            stacks.forEach(this::dropStack);
            ci.cancel();
        });
    }
}
