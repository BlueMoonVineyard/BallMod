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
import com.google.gson.JsonParseException;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.Identifier;

public sealed interface RecipeIngredient {
	public record CustomItem(
		ItemStack stack,
		Identifier identifier
	) {}

	public record Vanilla(
		List<Identifier> oneOf
	) implements RecipeIngredient {}
	public record Custom(
		List<CustomItem> oneOf
	) implements RecipeIngredient {}
	public record Tag(
		Identifier tag
	) implements RecipeIngredient {}
}

class RecipeIngredientDeserializer implements JsonDeserializer<RecipeIngredient> {
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

	@Override
	public RecipeIngredient deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		var object = json.getAsJsonObject();
		if (object.has("vanilla")) {
			var items = object.getAsJsonObject("vanilla").getAsJsonArray("oneOf");
			var it = new ArrayList<Identifier>();
			items.forEach(item -> it.add(new Identifier(item.getAsString())));
			return new RecipeIngredient.Vanilla(it);
		} else if (object.has("custom")) {
			var items = object.getAsJsonObject("custom").getAsJsonArray("oneOf");
			var it = new ArrayList<RecipeIngredient.CustomItem>();
			items.forEach(innerEl -> {
				var inner = innerEl.getAsJsonObject();
				var stack = inner.get("item").getAsJsonArray();
				var item = deserializeItem(stack);
				var id = new Identifier(inner.get("id").getAsString());
				it.add(new RecipeIngredient.CustomItem(item, id));
			});
			return new RecipeIngredient.Custom(it);
		} else if (object.has("tagList")) {
			var inner = object.getAsJsonObject("tagList").get("tag").getAsString();
			var id = new Identifier(inner);
			return new RecipeIngredient.Tag(id);
		} else {
			throw new JsonParseException("missing case, expected one of customItem, newWorkstation");
		}
	}
}
