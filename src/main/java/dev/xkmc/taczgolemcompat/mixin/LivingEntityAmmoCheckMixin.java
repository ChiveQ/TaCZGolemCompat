package dev.xkmc.taczgolemcompat.mixin;

import com.tacz.guns.entity.shooter.LivingEntityAmmoCheck;
import dev.xkmc.modulargolems.content.entity.common.AbstractGolemEntity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LivingEntityAmmoCheck.class})
public abstract class LivingEntityAmmoCheckMixin {

	@Shadow
	@Final
	private LivingEntity shooter;

	@Inject(method = {"needCheckAmmo", "consumesAmmoOrNot"}, remap = false, cancellable = true, at = {@At("HEAD")})
	private void modulargolems$checkAmmo(CallbackInfoReturnable<Boolean> cir) {
		if (this.shooter instanceof AbstractGolemEntity<?, ?> golem) {
			cir.setReturnValue(!golem.isHostile());
		}
	}
	
}
