package com.kromvia.client.platform;

import com.kromvia.client.KromviaClient;

@net.neoforged.fml.common.Mod("kromvia")
@net.minecraftforge.fml.common.Mod("kromvia")
public class KromviaMod {
	public KromviaMod() {
		new KromviaClient().onInitializeClient();
	}
}
