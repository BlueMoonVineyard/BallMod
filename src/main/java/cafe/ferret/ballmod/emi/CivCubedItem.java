package cafe.ferret.ballmod.emi;

import java.util.List;

import com.google.common.collect.Lists;

import cafe.ferret.ballmod.RecipesMessage.CustomItem;
import cafe.ferret.ballmod.RecipesMessage.NewWorkstation;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.ItemEmiStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CivCubedItem extends ItemEmiStack {
	final private Identifier _actualID;
	final private OrderedText _tooltip;
	final private static Comparison _comparison =
		Comparison.compareData(stack -> {
			if (!stack.hasNbt()) {
				return null;
			}
			var base = stack.getNbt();
			if (!base.contains("PublicBukkitValues", NbtCompound.COMPOUND_TYPE)) {
				return null;
			}
			var publicValues = base.getCompound("PublicBukkitValues");
			if (!publicValues.contains("ballcore:basic_item_registry_id", NbtCompound.STRING_TYPE)) {
				return null;
			}
			return new Identifier(publicValues.getString("ballcore:basic_item_registry_id"));
		});

	public CivCubedItem(OrderedText tooltip, CustomItem it) {
		super(it.stack());
		_actualID = it.identifier();
		_tooltip = tooltip;
		comparison = _comparison;
	}

	public CivCubedItem(OrderedText tooltip, NewWorkstation it) {
		super(it.workstation());
		_actualID = it.identifier();
		_tooltip = tooltip;
		comparison = _comparison;
	}

	private CivCubedItem(ItemStack stack, Identifier id, OrderedText tooltip) {
		super(stack);
		_actualID = id;
		_tooltip = tooltip;
		comparison = _comparison;
	}

	public CivCubedItem withAmount(int newAmount) {
		var stack = this.getItemStack().copy();
		stack.setCount(newAmount);
		var nieuw = new CivCubedItem(stack, _actualID, _tooltip);
		nieuw.amount = newAmount;
		return nieuw;
	}

	@Override
	public Identifier getId() {
		return _actualID;
	}

	@Override
	public List<Text> getTooltipText() {
		return getItemStack().getTooltip(
			MinecraftClient.getInstance().player,
			TooltipContext.HIDE_ADVANCED_DETAILS
		);
	}

	@Override
	public List<TooltipComponent> getTooltip() {
		List<TooltipComponent> list = Lists.newArrayList();
		getTooltipText().forEach(text -> {
			list.add(TooltipComponent.of(text.asOrderedText()));
		});
		list.remove(list.size() - 1);
		list.add(TooltipComponent.of(_tooltip));
		return list;
	}
}
