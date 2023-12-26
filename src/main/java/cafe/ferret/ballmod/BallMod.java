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
	public static BallMod instance;
	public RecipeReceiver receiver = null;

	public BallMod() throws Throwable {
		LOGGER.error("Creating BallMod...");
		BallMod.instance = this;
	}

	public static Identifier id(String key) {
		return new Identifier(MOD_ID, key);
	}

	@Override
	public void onInitializeClient(ModContainer mod) {
		LOGGER.error("Registering armor texture providers");
		CustomArmorRendering.register();

		LOGGER.error("Creating recipe receiver");
		receiver = new RecipeReceiver(LOGGER);

		LOGGER.error("All done!");
	}
}
