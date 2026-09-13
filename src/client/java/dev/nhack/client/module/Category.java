package dev.nhack.client.module;

import java.util.List;

public enum Category {
	COMBAT("Combat"),
	MOVEMENT("Movement"),
	RENDER("Render"),
	PLAYER("Player"),
	MISC("Misc"),
	CLIENT("Client"),
	SKYGAMES(
		"SkyEgames",
		SubCategory.COMBAT,
		SubCategory.MOVEMENT,
		SubCategory.MISC,
		SubCategory.TESTING,
		SubCategory.DETECTED
	);

	private final String displayName;
	private final List<SubCategory> subCategories;

	Category(String displayName, SubCategory... subCategories) {
		this.displayName = displayName;
		this.subCategories = List.of(subCategories);
	}

	public String getDisplayName() {
		return displayName;
	}

	public List<SubCategory> getSubCategories() {
		return subCategories;
	}

	public boolean hasSubCategories() {
		return !subCategories.isEmpty();
	}
}
