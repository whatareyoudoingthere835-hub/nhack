package dev.nhack.client.command.commands;

import dev.nhack.client.command.Command;
import dev.nhack.client.command.CommandManager;
import dev.nhack.client.util.ChatUtil;

public final class PrefixCommand extends Command {
	public PrefixCommand() {
		super("prefix", "Changes the command prefix", "prefix <symbol>");
	}

	@Override
	public void execute(String[] args) {
		if (args.length < 1 || args[0].isBlank()) {
			ChatUtil.info("Current prefix: " + CommandManager.getPrefix());
			return;
		}

		CommandManager.setPrefix(args[0]);
		ChatUtil.info("Prefix set to " + CommandManager.getPrefix());
	}
}
