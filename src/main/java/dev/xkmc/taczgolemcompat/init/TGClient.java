package dev.xkmc.taczgolemcompat.init;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(value = TaCZGolem.MODID, dist = Dist.CLIENT)
public class TGClient {

	public TGClient(IEventBus bus){
		bus.addListener(this::clientSetup);
	}

	private void clientSetup(FMLClientSetupEvent event) {
	}

}
