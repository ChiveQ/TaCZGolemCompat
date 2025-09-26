package dev.xkmc.taczgolemcompat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.entity.EntityKineticBullet;
import dev.xkmc.modulargolems.content.entity.common.AbstractGolemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Mixin({EntityKineticBullet.class})
public abstract class EntityKineticBulletMixin {

	@WrapOperation(method = {"onBulletTick"}, remap = false, at = @At(value = "INVOKE",
			target = "Lcom/tacz/guns/util/EntityUtil;findEntitiesOnPath(Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/List;"))
	private @NotNull List<EntityKineticBullet.EntityResult> modulargolems$skipFriendlyFire(Projectile proj, Vec3 src, Vec3 dst, Operation<List<EntityKineticBullet.EntityResult>> original) {
		var list = original.call(proj, src, dst);
		if (proj.getOwner() instanceof AbstractGolemEntity<?, ?> golem) {
			var ans = new ArrayList<EntityKineticBullet.EntityResult>();
			for (var er : list) {
				if (er.getEntity() instanceof LivingEntity le && golem.canAttack(le)) {
					ans.add(er);
				}
			}
			return ans;
		}
		return list;
	}

}
