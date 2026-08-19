package dev.nhack.client.command;

import java.util.List;
import java.util.Locale;

public abstract class Command {
	private final String name;
	private final String description;
	private final String usage;
	private final List<String> aliases;

	protected Command(String name, String description, String usage, String... aliases) {
		this.name = name;
		this.description = description;
		this.usage = usage;
		this.aliases = List.of(aliases);
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getUsage() {
		return usage;
	}

	public List<String> getAliases() {
		return aliases;
	}

	public boolean matches(String input) {
		String needle = input.toLowerCase(Locale.ROOT);
		if (name.equalsIgnoreCase(needle)) {
			return true;
		}
		for (String alias : aliases) {
			if (alias.equalsIgnoreCase(needle)) {
				return true;
			}
		}
		return false;
	}

	public abstract void execute(String[] args);
}
