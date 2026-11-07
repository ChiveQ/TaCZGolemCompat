package dev.xkmc.taczgolemcompat.content;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.util.AttachmentDataUtils;
import com.tacz.guns.util.time.GunTime;
import dev.xkmc.mob_weapon_api.api.ai.IWeaponHolder;
import dev.xkmc.mob_weapon_api.api.goals.IMeleeGoal;
import dev.xkmc.mob_weapon_api.example.goal.SmartRangedAttackGoal;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import static dev.xkmc.taczgolemcompat.content.TaczSmartGoal.GunState.*;

/**
 * Adapted from Pillager's Gun
 */
public class TaczSmartGoal<T extends Mob> extends SmartRangedAttackGoal<T> {

	private GunState gunState;
	private int attackDelay;
	private double attackCount;
	private int ammoCount;

	public TaczSmartGoal(T mob, IWeaponHolder holder, IMeleeGoal melee, double speed, double radius) {
		super(mob, holder, melee, speed, radius);
		this.gunState = UNCHARGED;
	}

	public boolean canUse() {
		return isHoldingGun() && (isValidTarget() || !hasAmmo() && canReload() || gunState == CHARGING);
	}

	public boolean canContinueToUse() {
		return isHoldingGun() && (isValidTarget() || !hasAmmo() && canReload() || gunState == CHARGING);
	}

	public void start() {
		super.start();
		this.gunState = UNCHARGED;
		var operator = IGunOperator.fromLivingEntity(mob);
		operator.draw(mob::getMainHandItem);
		operator.getDataHolder().drawTimestamp = GunTime.nowMillis() - 10000L;
		syncGunId(operator);
	}

	/**
	 * TaCZ 1.2.0+ gates every state-changing gun action behind {@code GunScriptGuard}, which
	 * silently returns without doing anything when {@code ShooterDataHolder.currentGunId} is
	 * null or does not match the held gun:
	 * <pre>
	 *   if (id == null || !id.equals(data.currentGunId)) return;
	 * </pre>
	 * {@code currentGunId} is only assigned by {@code setCurrentGunItem}, which is called from
	 * {@code LivingEntityDrawGun#draw} — but {@code draw} first runs {@code initialData()},
	 * which resets {@code currentGunId} to null, so the two must be kept in lockstep. Since
	 * {@code draw()} costs a full draw animation, we also reset only the id here so a mob can
	 * keep firing without re-playing the draw every time.
	 */
	private void syncGunId(IGunOperator operator) {
		ItemStack stack = mob.getMainHandItem();
		ShooterDataHolder data = operator.getDataHolder();
		IGun gun = IGun.getIGunOrNull(stack);
		data.currentGunId = gun == null ? null : gun.getGunId(stack);
		data.gunScriptFailed = false;
	}

	public void stop() {
		super.stop();
		attackDelay = 0;
		attackCount = 0;
		mob.getNavigation().stop();
		var gun = getGun();
		if (gun != null && isHoldingGun()) {
			IGunOperator operator = IGunOperator.fromLivingEntity(mob);
			syncGunId(operator);
			operator.cancelReload();
			operator.aim(false);
			GunData gunData = TimelessAPI.getCommonGunIndex(gun.getGunId(mob.getMainHandItem())).map(CommonGunIndex::getGunData).orElse(null);
			if (gunData != null && gunData.getBolt() == Bolt.MANUAL_ACTION) {
				operator.bolt();
			}
		}

	}

	public void tick() {
		LivingEntity target = mob.getTarget();
		var gun = getGun();
		ItemStack stack = mob.getMainHandItem();
		IGunOperator operator = IGunOperator.fromLivingEntity(mob);
		boolean preloadOnly = false;
		if (gun == null || !isHoldingGun()) return;
		// Refreshed every tick: TaCZ's server tick path and any draw() reset this back to null,
		// which makes reload()/bolt()/melee() silently no-op through GunScriptGuard.
		syncGunId(operator);
		if (target != null) {
			double range = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
			double dist = target.position().subtract(mob.position()).length();
			preloadOnly = isValidTarget() && (dist > range || seeTime < 5) && attackDelay == 0;
			if (isValidTarget() && canMeleeAttack(operator, target)) {
				operator.melee();
				return;
			}
			strafing();
			mob.getLookControl().setLookAt(target, 30, 90);
		}
		if (gun.isOverheatLocked(stack)) {
			gunState = UNCHARGED;
			return;
		}
		if (gunState == UNCHARGED && hasAmmo()) {
			gunState = CHARGED;
			ammoCount = getAmmoCount(stack);
		} else if (gunState == CHARGED && !hasAmmo()) {
			gunState = UNCHARGED;
		}
		if (gunState == UNCHARGED) {
			// Top the dummy clip up first: canReload() reads the dummy amount directly in 1.21.1,
			// so refilling afterwards would leave the goal stuck in UNCHARGED forever.
			GunData data = TimelessAPI.getCommonGunIndex(gun.getGunId(stack)).map(CommonGunIndex::getGunData).orElse(null);
			if (data != null) refillDummyAmmo(stack, data);
			if (!preloadOnly && canReload()) {
				operator.reload();
				gunState = CHARGING;
				if (mob instanceof CrossbowAttackMob cs) {
					cs.setChargingCrossbow(true);
				}
			}
		} else if (gunState == CHARGING) {
			if (!isHoldingGun()) {
				gunState = UNCHARGED;
				return;
			}
			if (!operator.getDataHolder().reloadStateType.isReloading()) {
				if (hasAmmo()) {
					gunState = CHARGED;
					attackDelay = 10 + mob.getRandom().nextInt(20);
					ammoCount = getAmmoCount(stack);
				} else {
					gunState = UNCHARGED;
				}
				if (mob instanceof CrossbowAttackMob cs) {
					cs.setChargingCrossbow(false);
				}
			}
		} else if (gunState == CHARGED) {
			if (--attackDelay <= 0) {
				operator.aim(true);
				gunState = READY_TO_ATTACK;
				attackDelay = 0;
			}
		} else if (gunState == READY_TO_ATTACK && seeTime > 0 && isRightAngle()) {
			GunData data = TimelessAPI.getCommonGunIndex(gun.getGunId(stack)).map(CommonGunIndex::getGunData).orElse(null);
			if (data != null) {
				for (attackCount += gun.getFireMode(stack) == FireMode.AUTO ? gun.getRPM(stack) / 1200d : gun.getRPM(stack) / (Math.max(1200 * (target.distanceTo(mob) / 8), 2400)); attackCount >= 1; --attackCount) {
					if (isHoldingGun()) {
						// In 1.21.1 shoot() can legitimately fail (COOL_DOWN / IS_DRAWING / IS_BOLTING),
						// and the tactical bolt is now part of the gun's own state machine, so only
						// subtract from our local count and re-bolt when the shot actually happened.
						ShootResult result = operator.shoot(mob::getXRot, mob::getYHeadRot);
						if (result != ShootResult.SUCCESS) {
							attackCount = 0;
							break;
						}
						if (data.getBolt() == Bolt.MANUAL_ACTION && !operator.getDataHolder().isBolting) {
							operator.bolt();
						}

						if (ammoCount > 0) {
							--ammoCount;
						}

						if (ammoCount == 0) {
							operator.aim(false);
							attackCount = 0;
							gunState = UNCHARGED;
							break;
						}
					}
				}
			}
		}

	}

	@Override
	public void performRangedAttack(LivingEntity livingEntity, float v, ItemStack itemStack, InteractionHand interactionHand) {
	}

	private boolean isHoldingGun() {
		return getGun() != null;
	}

	private boolean canReload() {
		if (holder.toUser().bypassAllConsumption()) return true;
		ItemStack stack = mob.getItemInHand(InteractionHand.MAIN_HAND);
		IGun gun = IGun.getIGunOrNull(stack);
		if (!(gun instanceof AbstractGunItem abstractGun)) return false;
		// TaCZ 1.21.1's AbstractGunItem#canReload already resolves dummy ammo itself:
		//   reloadData.isInfinite() -> true
		//   useDummyAmmo(stack)     -> getDummyAmmoAmount(stack) > 0
		//   otherwise               -> entity capability Capabilities.ItemHandler.ENTITY
		// It no longer falls through from dummy ammo to the inventory, so clearing the
		// DummyAmmo tag (what the 1.20.1 build did) can only ever make this return false.
		return abstractGun.canReload(mob, stack);
	}

	/**
	 * Give a gun with {@code useDummyAmmo} a fresh clip so that {@code canReload} passes and
	 * {@code tickReload} can actually finish. Mobs have no {@code Capabilities.ItemHandler.ENTITY},
	 * so dummy ammo is the only reload source available to them.
	 * <p>
	 * Note that {@code useDummyAmmo()} is {@code getUnsafe().contains("DummyAmmo", 3)}: the key has
	 * to exist, so a gun that never had a clip stays "no dummy ammo" until we write one here.
	 */
	private void refillDummyAmmo(ItemStack stack, GunData data) {
		IGun gun = IGun.getIGunOrNull(stack);
		if (gun == null) return;
		// A gun that reloads from the inventory instead never needs (and ignores) dummy ammo.
		if (gun.useInventoryAmmo(stack)) return;
		int capacity = Math.max(AttachmentDataUtils.getAmmoCountWithAttachment(stack, data), 1);
		int target = Math.max(gun.getMaxDummyAmmoAmount(stack), capacity);
		if (gun.getDummyAmmoAmount(stack) < target) {
			gun.setDummyAmmoAmount(stack, target);
		}
	}

	@Nullable
	private IGun getGun() {
		return IGun.getIGunOrNull(mob.getItemInHand(InteractionHand.MAIN_HAND));
	}

	private boolean isValidTarget() {
		return mob.getTarget() != null && mob.getTarget().isAlive();
	}


	private boolean hasAmmo() {
		return getAmmoCount(mob.getItemInHand(InteractionHand.MAIN_HAND)) != 0;
	}

	private boolean isRightAngle() {
		LivingEntity target = mob.getTarget();
		if (target != null && isValidTarget()) {
			var diff = target.getEyePosition().subtract(mob.getEyePosition());
			var deg = vectorDegreeCalculate(mob.getViewVector(1), diff);
			return deg < 10 + Math.max(0, 64 - mob.distanceToSqr(target));
		} else {
			return false;
		}
	}

	private int getAmmoCount(ItemStack stack) {
		IGun gun = IGun.getIGunOrNull(stack);
		if (gun != null) {
			GunData gunData = TimelessAPI.getCommonGunIndex(gun.getGunId(stack)).map(CommonGunIndex::getGunData).orElse(null);
			if (gunData != null) {
				if (gun.useInventoryAmmo(stack)) return -1;
				int chambered = gun.hasBulletInBarrel(stack) && gunData.getBolt() != Bolt.OPEN_BOLT ? 1 : 0;
				// Mobs reload from dummy ammo, which is stored outside GunCurrentAmmoCount.
				// Counting it here keeps the mob firing across clips instead of idling early.
				if (gun.useDummyAmmo(stack) && gun.getDummyAmmoAmount(stack) > 0) {
					return gun.getCurrentAmmoCount(stack) + chambered + gun.getDummyAmmoAmount(stack);
				}
				return gun.getCurrentAmmoCount(stack) + chambered;
			}
		}

		return 0;
	}

	public double vectorDegreeCalculate(Vec3 dir, Vec3 diff) {
		double cos = dir.dot(diff) / dir.length() / diff.length();
		return Math.toDegrees(Math.acos(cos));
	}

	public boolean canMeleeAttack(IGunOperator gunOperator, LivingEntity target) {
		return GunTime.nowMillis() - gunOperator.getDataHolder().meleeTimestamp > 3000L && isValidTarget() && getAttackReachSqr(target) >= mob.distanceToSqr(target);
	}

	public double getAttackReachSqr(LivingEntity target) {
		return mob.getBbWidth() * 2 * mob.getBbWidth() * 2 + target.getBbWidth();
	}

	enum GunState {
		UNCHARGED,
		CHARGING,
		CHARGED,
		READY_TO_ATTACK;
	}

}
