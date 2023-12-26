package cafe.ferret.ballmod.emi;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.google.common.collect.Maps;

import cafe.ferret.ballmod.BallMod;
import cafe.ferret.ballmod.RecipeReceiver;
import dev.emi.emi.EmiPort;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.recipe.EmiShapedRecipe;
import dev.emi.emi.recipe.EmiShapelessRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.BlastingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class Plugin implements EmiPlugin {
	final private RecipeReceiver _receiver;
	final private Logger _logger;
	final private OrderedText _civcubedText;

	public Plugin() {
		_logger = BallMod.LOGGER;
		_receiver = BallMod.instance.receiver;
		var txt = Text.of("CivCubed");
		_civcubedText = OrderedText.composite(
				txt.setStyle(
						Style.EMPTY
								.withItalic(true)
								.withColor(Formatting.BLUE))
						.stream()
						.map(t -> t.asOrderedText())
						.toList());
	}

	@Override
	public void register(EmiRegistry registry) {
		_logger.error("Receiver is done? " + Boolean.valueOf(_receiver.isDone()).toString());
		assert _receiver.isDone() : "receiver is done";
		registry.removeRecipes(recipe -> {
			var namespace = recipe.getId().getNamespace();
			return (recipe instanceof EmiCraftingRecipe
					|| recipe instanceof EmiCookingRecipe) &&
					(namespace.equals("ballcore") || namespace.equals("bc"));
		});

		var map = Maps.<Identifier, CivCubedItem>newHashMap();

		_receiver.customItems().forEach(item -> {
			var stack = new cafe.ferret.ballmod.emi.CivCubedItem(_civcubedText, item);
			registry.addEmiStack(stack);
			map.put(stack.getId(), stack);
		});

		Function<ItemStack, Stream<CivCubedItem>> lookup = (ItemStack stack) -> {
			if (!stack.hasNbt()) {
				return Stream.empty();
			}
			var base = stack.getNbt();
			if (!base.contains("PublicBukkitValues", NbtCompound.COMPOUND_TYPE)) {
				return Stream.empty();
			}
			var publicValues = base.getCompound("PublicBukkitValues");
			if (!publicValues.contains("ballcore:basic_item_registry_id", NbtCompound.STRING_TYPE)) {
				return Stream.empty();
			}
			return Stream.of(map.get(new Identifier(publicValues.getString("ballcore:basic_item_registry_id"))));
		};

		Function<Recipe<?>, List<EmiIngredient>> ingredientsOf = (Recipe<?> recipe) -> {
			return recipe.getIngredients().stream().<EmiIngredient>map(ingredient -> {
				var ingredients = List.of(ingredient.getMatchingStacks()).stream().<EmiIngredient>map(ingr -> {
					var civcubed = lookup.apply(ingr).findFirst();
					if (civcubed.isPresent()) {
						return civcubed.get();
					} else {
						return EmiIngredient.of(ingredient);
					}
				});
				return EmiIngredient.of(ingredients.toList());
			}).toList();
		};

		Function<Recipe<?>, EmiStack> outputOf = (Recipe<?> recipe) -> {
			var it = lookup.apply(EmiPort.getOutput(recipe)).findFirst().map(x -> x.withAmount(EmiPort.getOutput(recipe).getCount()));
			return it.isPresent() ? it.get() : EmiStack.of(EmiPort.getOutput(recipe));
		};

		registry.getRecipeManager().values().forEach(recipe -> {
			var namespace = recipe.getId().getNamespace();
			if (!namespace.equals("ballcore") && !namespace.equals("bc")) {
				return;
			}

			if (recipe instanceof ShapedRecipe r) {
				var inputs = ingredientsOf.apply(r);
				var output = outputOf.apply(r);
				var newID = new Identifier("civcubed", recipe.getId().getPath());
				registry.addRecipe(new EmiCraftingRecipe(inputs, output, newID, false));
			} else if (recipe instanceof ShapelessRecipe r) {
				var inputs = ingredientsOf.apply(r);
				var output = outputOf.apply(r);
				var newID = new Identifier("civcubed", recipe.getId().getPath());
				registry.addRecipe(new EmiCraftingRecipe(inputs, output, newID, true));
			} else if (recipe instanceof SmeltingRecipe r) {
				var inputs = ingredientsOf.apply(r).get(0);
				var output = outputOf.apply(r);
				var newID = new Identifier("civcubed", recipe.getId().getPath());
				registry.addRecipe(new EmiCookingRecipe(newID, inputs, output, VanillaEmiRecipeCategories.SMELTING,
						r.getCookTime(), r.getExperience(), 1, false));
			} else if (recipe instanceof BlastingRecipe r) {
				var inputs = ingredientsOf.apply(r).get(0);
				var output = outputOf.apply(r);
				var newID = new Identifier("civcubed", recipe.getId().getPath());
				registry.addRecipe(new EmiCookingRecipe(newID, inputs, output, VanillaEmiRecipeCategories.BLASTING,
						r.getCookTime(), r.getExperience(), 1, false));
			}
		});

		_receiver.workstations().forEach(workstation -> {
			var station = new cafe.ferret.ballmod.emi.CivCubedItem(_civcubedText, workstation);
			var category = new EmiRecipeCategory(workstation.identifier(), EmiStack.of(workstation.workstation()));
			registry.addCategory(category);
			registry.addWorkstation(category, station);
			_receiver.recipesFor(workstation.identifier()).forEach(recipe -> {
				registry.addRecipe(new CivCubedRecipe(
						recipe.id(),
						recipe.inputs(),
						recipe.outputs(),
						recipe.name(),
						recipe.work(),
						recipe.minimumPlayersRequiredToWork(),
						category,
						map::get));
			});
		});
	}
}
