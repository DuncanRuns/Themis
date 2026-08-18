package me.duncanruns.themis.mixin;

import me.duncanruns.themis.RNGManager;
import me.duncanruns.themis.ThemisMod;
import me.duncanruns.themis.random.CountedRandom;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;
import java.util.Optional;

@Mixin(WitherSkeletonEntity.class)
public abstract class WitherSkeletonEntityMixin extends AbstractSkeletonEntity {
    protected WitherSkeletonEntityMixin(EntityType<? extends AbstractSkeletonEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        super.dropLoot(source, causedByPlayer);
        if (!causedByPlayer) return;
        int looting = MathHelper.clamp(ThemisMod.getLooting(source.getAttacker()), 0, 3);
        Optional<List<ItemStack>> skullItemOverrideOpt = RNGManager.getSkullItemOverride(getServer(), looting);
        if (skullItemOverrideOpt.isPresent()) {
            skullItemOverrideOpt.get().forEach(this::dropStack);
            return;
        }
        CountedRandom random = RNGManager.getSkullRandom(getServer(), looting);
        if (random.nextFloat() < 0.025f + 0.01f * looting) {
            dropStack(new ItemStack(Items.WITHER_SKELETON_SKULL, 1));
        }
    }
}
