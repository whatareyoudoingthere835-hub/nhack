package dev.nhack.client.command;

import dev.nhack.client.command.commands.BindCommand;
import dev.nhack.client.command.commands.HelpCommand;
import dev.nhack.client.command.commands.PrefixCommand;
import dev.nhack.client.command.commands.ToggleCommand;
import dev.nhack.client.util.ChatUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class CommandManager {
	private static final List<Command> COMMANDS = new ArrayList<>();
	private static String prefix = ".";

	private CommandManager() {
	}

	public static void init() {
		register(new HelpCommand());
		register(new ToggleCommand());
		register(new BindCommand());
		register(new PrefixCommand());
	}

	private static void register(Command command) {
		COMMANDS.add(command);
	}

	public static List<Command> getCommands() {
		return Collections.unmodifiableList(COMMANDS);
	}

	public static String getPrefix() {
		return prefix;
	}

	public static void setPrefix(String prefix) {
		if (prefix != null && !prefix.isBlank()) {
			CommandManager.prefix = prefix;
		}
	}

	public static boolean handle(String message) {
		if (!message.startsWith(prefix)) {
			return false;
		}

		String body = message.substring(prefix.length()).trim();
		if (body.isEmpty()) {
			return true;
		}

		String[] split = body.split("\\s+");
		String name = split[0].toLowerCase(Locale.ROOT);
		String[] args = new String[split.length - 1];
		System.arraycopy(split, 1, args, 0, args.length);

		for (Command command : COMMANDS) {
			if (command.matches(name)) {
				try {
					command.execute(args);
				} catch (Exception exception) {
					ChatUtil.error("Command failed: " + exception.getMessage());
				}
				return true;
			}
		}

		ChatUtil.error("Unknown command. Try " + prefix + "help");
		return true;
	}
}
