/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package cafe.ferret.ballmod.armor;

import cafe.ferret.ballmod.BallMod;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.quiltmc.qsl.rendering.entity.api.client.ArmorRenderingRegistry;

public class CustomArmorRendering {
	private static final String TEXTURE_PREFIX = "textures/models/armor/";

	private static final ArmorRenderingRegistry.TextureProvider TEXTURE_PROVIDER = (texture, entity, stack, slot, useSecondLayer, suffix) -> {
		NbtCompound nbt = stack.getNbt();

		if (nbt == null) {
			return texture;
		}

		int customModelData = nbt.getInt("CustomModelData");

		// Check if the item is iron armor
		if (stack.isOf(Items.IRON_HELMET) || stack.isOf(Items.IRON_CHESTPLATE) || stack.isOf(Items.IRON_LEGGINGS) || stack.isOf(Items.IRON_BOOTS)) {
			return switch (customModelData) {
				// iron
				case 1 ->
					new Identifier("minecraft", TEXTURE_PREFIX + "iron_layer_" + (useSecondLayer ? 2 : 1) + ".png");
				// copper
				case 2 -> BallMod.id(TEXTURE_PREFIX + "copper_layer_" + (useSecondLayer ? 2 : 1) + ".png");
				// tin
				case 3 -> BallMod.id(TEXTURE_PREFIX + "tin_layer_" + (useSecondLayer ? 2 : 1) + ".png");
				// orichalcum
				case 4 -> BallMod.id(TEXTURE_PREFIX + "orichalcum_layer_" + (useSecondLayer ? 2 : 1) + ".png");
				// aluminum
				case 5 -> BallMod.id(TEXTURE_PREFIX + "aluminum_layer_" + (useSecondLayer ? 2 : 1) + ".png");
				// hihiirogane
				case 6 -> BallMod.id(TEXTURE_PREFIX + "hihiirogane_layer_" + (useSecondLayer ? 2 : 1) + ".png");
				// zinc
				case 7 -> BallMod.id(TEXTURE_PREFIX + "zinc_layer_" + (useSecondLayer ? 2 : 1) + ".png");
				// meteorite
				case 8 -> BallMod.id(TEXTURE_PREFIX + "meteorite_layer_" + (useSecondLayer ? 2 : 1) + ".png");

				default -> texture;
			};
		}

		// Check if the item is gold armor
		if (stack.isOf(Items.GOLDEN_HELMET) || stack.isOf(Items.GOLDEN_CHESTPLATE) || stack.isOf(Items.GOLDEN_LEGGINGS) || stack.isOf(Items.GOLDEN_BOOTS)) {
			return switch (customModelData) {
				// gold
				case 1 ->
					new Identifier("minecraft", TEXTURE_PREFIX + "gold_layer_" + (useSecondLayer ? 2 : 1) + ".png");
				// silver
				case 2 -> BallMod.id(TEXTURE_PREFIX + "silver_layer_" + (useSecondLayer ? 2 : 1) + ".png");
				// palladium
				case 3 -> BallMod.id(TEXTURE_PREFIX + "palladium_layer_" + (useSecondLayer ? 2 : 1) + ".png");
				// magnesium
				case 4 -> BallMod.id(TEXTURE_PREFIX + "magnesium_layer_" + (useSecondLayer ? 2 : 1) + ".png");

				default -> texture;
			};
		}

		return texture;
	};

	public static void register() {
		ArmorRenderingRegistry.registerTextureProvider(
			TEXTURE_PROVIDER,
			Items.IRON_HELMET,
			Items.IRON_CHESTPLATE,
			Items.IRON_LEGGINGS,
			Items.IRON_BOOTS,
			Items.GOLDEN_HELMET,
			Items.GOLDEN_CHESTPLATE,
			Items.GOLDEN_LEGGINGS,
			Items.GOLDEN_BOOTS
		);
	}
}
