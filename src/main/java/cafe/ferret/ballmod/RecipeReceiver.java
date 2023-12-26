package cafe.ferret.ballmod;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Collection;

import org.quiltmc.qsl.networking.api.PacketSender;
import org.quiltmc.qsl.networking.api.client.ClientPlayConnectionEvents;
import org.quiltmc.qsl.networking.api.client.ClientPlayNetworking;
import org.slf4j.Logger;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import cafe.ferret.ballmod.RecipesMessage.CustomItem;
import cafe.ferret.ballmod.RecipesMessage.NewWorkstation;
import cafe.ferret.ballmod.RecipesMessage.Recipe;
import io.netty.buffer.Unpooled;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class RecipeReceiver {
	final private Logger _logger;
	final private Gson _gson;
	final private ConcurrentHashMap<Identifier, CustomItem> _customItems;
	final private ConcurrentHashMap<Identifier, NewWorkstation> _workstations;
	final private ConcurrentHashMap<Identifier, Recipe> _recipes;
	final private Multimap<Identifier, Recipe> _workstationsToRecipes;
	final private AtomicBoolean _isDone;
	final private List<Consumer<RecipeReceiver>> _callbacks;

	public RecipeReceiver(Logger logger) {
		_logger = logger;
		var builder = new GsonBuilder();
		builder.registerTypeAdapter(RecipesMessage.class, new RecipesMessageDeserializer());
		builder.registerTypeAdapter(RecipeIngredient.class, new RecipeIngredientDeserializer());
		builder.registerTypeAdapter(Text.class, new Text.Serializer());
		_gson = builder.create();
		_customItems = new ConcurrentHashMap<>();
		_workstations = new ConcurrentHashMap<>();
		_recipes = new ConcurrentHashMap<>();
		_workstationsToRecipes = Multimaps.synchronizedListMultimap(Multimaps.newListMultimap(new ConcurrentHashMap<>(), () -> new ArrayList<>()));
		_callbacks = new ArrayList<>();
		_isDone = new AtomicBoolean();

		ClientPlayConnectionEvents.INIT.register(this::doInit);
		ClientPlayConnectionEvents.JOIN.register(this::sendRegister);
	}

	public boolean isDone() {
		return _isDone.get();
	}

	public void addCallback(Consumer<RecipeReceiver> it) {
		_callbacks.add(it);
	}

	public Collection<CustomItem> customItems() {
		return _customItems.values();
	}

	public Collection<NewWorkstation> workstations() {
		return _workstations.values();
	}

	public Collection<Recipe> recipesFor(Identifier workstation) {
		return _workstationsToRecipes.get(workstation);
	}

	private void doInit(
		ClientPlayNetworkHandler handler,
		MinecraftClient client
	) {
		_logger.error("Registering receiver...");
		if (!ClientPlayNetworking.registerReceiver(
				new Identifier("civcubed", "recipes"),
				this::handleRecipeMessage)) {
			throw new RuntimeException("failed to register civcubed recipes");
		}
	}

	private void sendRegister(
		ClientPlayNetworkHandler handler,
		PacketSender sender,
		MinecraftClient client
	) {
		var buf = new PacketByteBuf(Unpooled.buffer());
		buf.writeString("civcubed:recipes");
		sender.sendPacket(new Identifier("register"), buf);
	}

	private void handleRecipeMessage(
		MinecraftClient client,
		ClientPlayNetworkHandler handler,
		PacketByteBuf buf,
		PacketSender responseSender
	) {
		var str = buf.readString();
		try {
			var it = _gson.fromJson(str, RecipesMessage.class);
			_logger.error("Received packet: " + it);
			if (!_isDone.get() && it instanceof RecipesMessage.CustomItem c) {
				_customItems.put(c.identifier(), c);
			} else if (!_isDone.get() && it instanceof RecipesMessage.NewWorkstation n) {
				_workstations.put(n.identifier(), n);
			} else if (!_isDone.get() && it instanceof RecipesMessage.NewRecipe r) {
				_workstationsToRecipes.put(r.workstation(), r.recipe());
				_recipes.put(r.recipe().id(), r.recipe());
			} else if (!_isDone.get() && it instanceof RecipesMessage.AllDone) {
				_isDone.set(true);
			} else if (it instanceof RecipesMessage.NowSending) {
				_customItems.clear();
				_workstations.clear();
				_workstationsToRecipes.clear();
				_recipes.clear();
				_isDone.set(false);
			}
		} catch (Exception e) {
			_logger.error("Failed to read recipe message", e);
		}
	}
}
