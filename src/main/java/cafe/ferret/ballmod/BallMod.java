/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package cafe.ferret.ballmod;

import cafe.ferret.ballmod.armor.CustomArmorRendering;
import net.minecraft.util.Identifier;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.client.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BallMod implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("BallMod");
	public static final String MOD_ID = "ballmod";

	public static Identifier id(String key) {
		return new Identifier(MOD_ID, key);
	}

	@Override
	public void onInitializeClient(ModContainer mod) {
		LOGGER.info("Registering armor texture providers");
		CustomArmorRendering.register();

		LOGGER.info("All done!");
	}
}
