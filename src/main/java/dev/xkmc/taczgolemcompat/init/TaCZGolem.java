package dev.xkmc.taczgolemcompat.init;

import com.tacz.guns.api.item.IGun;
import dev.xkmc.mob_weapon_api.registry.WeaponStatus;
import dev.xkmc.modulargolems.content.entity.humanoid.weapon.GolemWeaponRegistry;
import dev.xkmc.taczgolemcompat.content.TaczSmartGoal;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(TaCZGolem.MODID)
@Mod.EventBusSubscriber(modid = TaCZGolem.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TaCZGolem {

	public static final String MODID = "taczgolemcompat";
	public static final Logger LOGGER = LogManager.getLogger();
	public static final IEventBus MOD_BUS = FMLJavaModLoadingContext.get().getModEventBus();

	public TaCZGolem() {
	}

	@SubscribeEvent
	public static void modifyAttributes(EntityAttributeModificationEvent event) {
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

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void gatherData(GatherDataEvent event) {
	}


	public static ResourceLocation loc(String id) {
		return new ResourceLocation(MODID, id);
	}

}
