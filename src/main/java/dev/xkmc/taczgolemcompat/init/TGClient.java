package dev.xkmc.taczgolemcompat.init;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = TaCZGolem.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TGClient {

	@SubscribeEvent
	public static void clientSetup(FMLClientSetupEvent event) {
	}

}
