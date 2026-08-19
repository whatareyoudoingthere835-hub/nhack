package dev.nhack.client.util;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class InventoryUtil {
	private InventoryUtil() {
	}

	public static boolean isWeapon(ItemStack stack) {
		return !stack.isEmpty() && (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.is(Items.TRIDENT));
	}

	public static boolean isSword(ItemStack stack) {
		return !stack.isEmpty() && stack.is(ItemTags.SWORDS);
	}

	public static boolean isAxe(ItemStack stack) {
		return !stack.isEmpty() && stack.is(ItemTags.AXES);
	}

	public static int findSwordHotbar(LocalPlayer player) {
		return findInHotbar(player, InventoryUtil::isSword);
	}

	public static int findAxeHotbar(LocalPlayer player) {
		return findInHotbar(player, InventoryUtil::isAxe);
	}

	public static int findAxe(LocalPlayer player) {
		Inventory inventory = player.getInventory();
		int hotbar = findAxeHotbar(player);
		if (hotbar != -1) {
			return hotbar;
		}
		for (int slot = 9; slot < inventory.getContainerSize(); slot++) {
			if (isAxe(inventory.getItem(slot))) {
				return slot;
			}
		}
		return -1;
	}

	public static int findInHotbar(LocalPlayer player, java.util.function.Predicate<ItemStack> test) {
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < 9; slot++) {
			if (test.test(inventory.getItem(slot))) {
				return slot;
			}
		}
		return -1;
	}

	public static int toMenuSlot(int inventorySlot) {
		return inventorySlot < 9 ? 36 + inventorySlot : inventorySlot;
	}
}
