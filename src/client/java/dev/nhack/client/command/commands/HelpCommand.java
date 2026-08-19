package dev.nhack.client.command.commands;

import dev.nhack.NHack;
import dev.nhack.client.command.Command;
import dev.nhack.client.command.CommandManager;
import dev.nhack.client.util.ChatUtil;
import net.minecraft.ChatFormatting;

public final class HelpCommand extends Command {
	public HelpCommand() {
		super("help", "Lists client commands", "help [command]", "h", "?");
	}

	@Override
	public void execute(String[] args) {
		if (args.length >= 1) {
			for (Command command : CommandManager.getCommands()) {
				if (command.matches(args[0])) {
					ChatUtil.info(CommandManager.getPrefix() + command.getUsage() + " — " + command.getDescription());
					return;
				}
			}
			ChatUtil.error("No such command");
			return;
		}

		ChatUtil.send(ChatFormatting.WHITE, NHack.NAME + " commands:");
		for (Command command : CommandManager.getCommands()) {
			ChatUtil.info(CommandManager.getPrefix() + command.getUsage() + " — " + command.getDescription());
		}
	}
}
