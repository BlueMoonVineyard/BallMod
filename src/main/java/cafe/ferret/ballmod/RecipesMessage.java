package cafe.ferret.ballmod;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.ArrayList;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtIo;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

public sealed interface RecipesMessage {
	public record CustomItem(
		ItemStack stack,
		Identifier identifier
	) implements RecipesMessage {}

	public record NewWorkstation(
		ItemStack workstation,
		Identifier identifier
	) implements RecipesMessage {}

	public record Recipe(
		Text name,
		Identifier id,
		List<Pair<RecipeIngredient, Integer>> inputs,
		List<Pair<ItemStack, Integer>> outputs,
		int work,
		int minimumPlayersRequiredToWork
	) {}

	public record NewRecipe(
		Identifier workstation,
		Recipe recipe
	) implements RecipesMessage {}

	public record AllDone() implements RecipesMessage {}
	public record NowSending() implements RecipesMessage {}
}

class RecipesMessageDeserializer implements JsonDeserializer<RecipesMessage> {
	private ItemStack deserializeItem(JsonArray array) throws JsonParseException {
		var buffer = ByteBuffer.allocate(array.size());
		array.forEach(element -> buffer.put(element.getAsByte()));
		try {
			var compound = NbtIo.readCompressed(new DataInputStream(new ByteArrayInputStream(buffer.array())));
			return ItemStack.fromNbt(compound);
		} catch (IOException e) {
			throw new JsonParseException(e);
		}
	}
	private RecipesMessage.Recipe deserializeRecipe(JsonObject object, JsonDeserializationContext context) throws JsonParseException {
		var name = (Text)context.deserialize(object.get("name"), Text.class);
		var id = new Identifier(object.get("id").getAsString());
		var work = object.get("work").getAsInt();
		var minimumPlayersRequiredToWork = object.get("minimumPlayersRequiredToWork").getAsInt();

		var inputs = new ArrayList<Pair<RecipeIngredient, Integer>>();
		object.getAsJsonArray("inputs").forEach(input -> {
			var pair = input.getAsJsonArray();
			var first = (RecipeIngredient)context.deserialize(pair.get(0), RecipeIngredient.class);
			var second = pair.get(1).getAsInt();
			inputs.add(new Pair<>(first, second));
		});

		var outputs = new ArrayList<Pair<ItemStack, Integer>>();
		object.getAsJsonArray("outputs").forEach(input -> {
			var pair = input.getAsJsonArray();
			var first = deserializeItem(pair.get(0).getAsJsonArray());
			var second = pair.get(1).getAsInt();
			outputs.add(new Pair<>(first, second));
		});

		return new RecipesMessage.Recipe(name, id, inputs, outputs, work, minimumPlayersRequiredToWork);
	}
	@Override
	public RecipesMessage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		var obj = json.getAsJsonObject();
		if (obj.has("customItem")) {
			var inner = obj.getAsJsonObject("customItem");
			var stack = inner.get("item").getAsJsonArray();
			var item = deserializeItem(stack);
			var id = new Identifier(inner.get("id").getAsString());
			return new RecipesMessage.CustomItem(item, id);
		} else if (obj.has("newWorkstation")) {
			var inner = obj.getAsJsonObject("newWorkstation");
			var stack = inner.get("workstation").getAsJsonArray();
			var item = deserializeItem(stack);
			var id = new Identifier(inner.get("id").getAsString());
			return new RecipesMessage.NewWorkstation(item, id);
		} else if (obj.has("newRecipe")) {
			var inner = obj.getAsJsonObject("newRecipe");
			var workstation = new Identifier(inner.get("workstation").getAsString());
			var recipe = deserializeRecipe(inner.getAsJsonObject("recipe"), context);
			return new RecipesMessage.NewRecipe(workstation, recipe);
		} else if (obj.has("allDone")) {
			return new RecipesMessage.AllDone();
		} else if (obj.has("nowSending")) {
			return new RecipesMessage.NowSending();
		} else {
			throw new JsonParseException("missing case, expected one of customItem, newWorkstation, newRecipe, allDone");
		}
	}
}
