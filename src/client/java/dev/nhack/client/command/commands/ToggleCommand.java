package dev.nhack.client.command.commands;

import dev.nhack.client.command.Command;
import dev.nhack.client.command.CommandManager;
import dev.nhack.client.module.Module;
import dev.nhack.client.module.ModuleManager;
import dev.nhack.client.util.ChatUtil;

public final class ToggleCommand extends Command {
	public ToggleCommand() {
		super("toggle", "Toggles a module", "toggle <module>", "t");
	}

	@Override
	public void execute(String[] args) {
		if (args.length < 1) {
			ChatUtil.error("Usage: " + CommandManager.getPrefix() + getUsage());
			return;
		}

		ModuleManager.get(args[0]).ifPresentOrElse(Module::toggle, () -> ChatUtil.error("Unknown module"));
	}
}
