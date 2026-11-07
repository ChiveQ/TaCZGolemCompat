package dev.xkmc.taczgolemcompat.init;

import com.tacz.guns.api.item.IGun;
import dev.xkmc.mob_weapon_api.registry.WeaponStatus;
import dev.xkmc.modulargolems.content.entity.humanoid.weapon.GolemWeaponRegistry;
import dev.xkmc.taczgolemcompat.content.TaczSmartGoal;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(TaCZGolem.MODID)
@EventBusSubscriber(modid = TaCZGolem.MODID)
public class TaCZGolem {

	public static final String MODID = "taczgolemcompat";
	public static final Logger LOGGER = LogManager.getLogger();

	public TaCZGolem() {
	}

	@SubscribeEvent
	public static void setup(final FMLCommonSetupEvent event) {
		event.enqueueWork(() -> {
			GolemWeaponRegistry.HUMANOID.register(loc("tacz"),
					(golem, stack, hand) -> WeaponStatus.RANGED.of(IGun.getIGunOrNull(stack) != null),
					(golem, melee) -> new TaczSmartGoal<>(golem, golem, melee, 1, 35)
			);
		});
	}

	public static ResourceLocation loc(String id) {
		return ResourceLocation.fromNamespaceAndPath(MODID, id);
	}

}
