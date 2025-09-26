package dev.xkmc.taczgolemcompat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.util.EntityUtil;
import dev.xkmc.modulargolems.content.entity.common.AbstractGolemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({EntityUtil.class})
public abstract class EntityUtilMixin {

	@WrapOperation(method = {"findEntityOnPath"}, remap = false, at = {@At(value = "INVOKE", target =
			"Lnet/minecraft/world/entity/Entity;equals(Ljava/lang/Object;)Z")})
	private static boolean modulargolems$skipFriendlyFire(Entity target, Object owner, Operation<Boolean> original) {
		if (owner instanceof AbstractGolemEntity<?, ?> golem && target instanceof LivingEntity le) {
			return original.call(target, owner) || !golem.canAttack(le);
		}
		return original.call(target, owner);
	}
}
