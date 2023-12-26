package cafe.ferret.ballmod.emi;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.joml.Math;

import cafe.ferret.ballmod.RecipeIngredient;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

public class CivCubedRecipe implements EmiRecipe {
	private final Identifier _id;
	private final List<Pair<RecipeIngredient, Integer>> _inputs;
	private final List<Pair<ItemStack, Integer>> _outputs;
	private final Text _name;
	private final int _work;
	private final int _minimumPlayersRequiredToWork;
	private final EmiRecipeCategory _category;
	private final Function<Identifier, CivCubedItem> _lookup;

	public CivCubedRecipe(
		Identifier id,
		List<Pair<RecipeIngredient, Integer>> inputs,
		List<Pair<ItemStack, Integer>> outputs,
		Text name,
		int work,
		int minimumPlayersRequiredToWork,
		EmiRecipeCategory category,
		Function<Identifier, CivCubedItem> lookup
	) {
		_id = id;
		_inputs = inputs;
		_outputs = outputs;
		_name = name;
		_work = work;
		_minimumPlayersRequiredToWork = minimumPlayersRequiredToWork;
		_category = category;
		_lookup = lookup;
	}

	private Optional<CivCubedItem> lookupFrom(ItemStack stack) {
		if (!stack.hasNbt()) {
			return Optional.empty();
		}
		var base = stack.getNbt();
		if (!base.contains("PublicBukkitValues", NbtCompound.COMPOUND_TYPE)) {
			return Optional.empty();
		}
		var publicValues = base.getCompound("PublicBukkitValues");
		if (!publicValues.contains("ballcore:basic_item_registry_id", NbtCompound.STRING_TYPE)) {
			return Optional.empty();
		}
		return Optional.of(_lookup.apply(new Identifier(publicValues.getString("ballcore:basic_item_registry_id"))));
	}

	@Override
	public EmiRecipeCategory getCategory() {
		return _category;
	}

	@Override
	public Identifier getId() {
		return _id;
	}

	private EmiIngredient from(Pair<RecipeIngredient, Integer> ingredient) {
		if (ingredient.getLeft() instanceof RecipeIngredient.Vanilla vanilla) {
			var ingredients = vanilla.oneOf().stream().map(Registries.ITEM::get).toArray(Item[]::new);
			return EmiIngredient.of(Ingredient.ofItems(ingredients), ingredient.getRight());
		} else if (ingredient.getLeft() instanceof RecipeIngredient.Custom custom) {
			var ingredients = custom.oneOf().stream().map(
				x -> _lookup.apply(x.identifier()).withAmount(ingredient.getRight())
			).toList();
			return EmiIngredient.of(ingredients, ingredient.getRight());
		} else if (ingredient.getLeft() instanceof RecipeIngredient.Tag tag) {
			var key = TagKey.of(RegistryKeys.ITEM, tag.tag());
			return EmiIngredient.of(key, ingredient.getRight());
		} else {
			throw new RuntimeException("unknown ingredient type");
		}
	}

	private EmiStack fromStack(Pair<ItemStack, Integer> ingredient) {
		var custom = lookupFrom(ingredient.getLeft());
		if (custom.isPresent()) {
			return custom.get().withAmount(ingredient.getRight());
		} else {
			return EmiStack.of(ingredient.getLeft(), ingredient.getRight());
		}
	}

	@Override
	public List<EmiIngredient> getInputs() {
		return _inputs.stream().map(this::from).toList();
	}

	@Override
	public List<EmiStack> getOutputs() {
		return _outputs.stream().map(this::fromStack).toList();
	}

	@Override
	public int getDisplayWidth() {
		return 132;
	}

	@Override
	public int getDisplayHeight() {
		return 18 * Math.max(rowsForInput(), rowsForOutput());
	}

	private int rowsForInput() {
		return (int)Math.ceil(((double)_inputs.size()) / ((double)columnsPerSide()));
	}

	private int rowsForOutput() {
		return (int)Math.ceil(((double)_outputs.size()) / ((double)columnsPerSide()));
	}

	private int columnsForInput() {
		return Math.min(_inputs.size(), columnsPerSide());
	}

	private int columnsForOutput() {
		return Math.min(_inputs.size(), columnsPerSide());
	}

	private int sideWidth() {
		var remainingWidthForColumns = getDisplayWidth() - EmiTexture.EMPTY_ARROW.width;
		var sideWidth = remainingWidthForColumns / 2;
		return sideWidth;
	}

	private int columnsPerSide() {
		var columnsPerSide = sideWidth() / 18;
		return columnsPerSide;
	}

	@Override
	public void addWidgets(WidgetHolder widgets) {
		var arrowX = getDisplayWidth()/2 - EmiTexture.EMPTY_ARROW.width/2;
		var arrowY = getDisplayHeight()/2 - EmiTexture.EMPTY_ARROW.height/2;
		widgets.addTexture(EmiTexture.EMPTY_ARROW, arrowX, arrowY);

		int columnOffset = sideWidth()/2 - (columnsForInput()*18)/2;
		int rowOffset = getDisplayHeight()/2 - (rowsForInput()*18)/2;
		int line = 0;
		int column = 0;
		for (Pair<RecipeIngredient,Integer> pair : _inputs) {
			if (column >= columnsPerSide()) {
				column = 0;
				line++;
			}

			widgets.addSlot(from(pair), columnOffset + column*18, rowOffset + line*18);
			column++;
		}

		columnOffset = sideWidth()/2 - (columnsForOutput()*18)/2;
		rowOffset = getDisplayHeight()/2 - (rowsForOutput()*18)/2;
		line = 0;
		column = 0;

		for (Pair<ItemStack,Integer> pair : _outputs) {
			if (column >= columnsPerSide()) {
				column = 0;
				line++;
			}

			widgets.addSlot(fromStack(pair), arrowX + EmiTexture.EMPTY_ARROW.width + columnOffset + column*18, rowOffset + line*18).recipeContext(this);
			column++;
		}
	}
}
