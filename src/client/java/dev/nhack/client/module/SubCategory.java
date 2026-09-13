package dev.nhack.client.module;

/**
 * Second level tabs that live inside a {@link Category} (see {@code Category.SKYGAMES}).
 * A module without a sub category shows up in every sub tab of its category.
 */
public enum SubCategory {
	COMBAT("Combat"),
	MOVEMENT("Movement"),
	MISC("Misc"),
	TESTING("Testing"),
	DETECTED("!Detected!");

	private final String displayName;

	SubCategory(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
