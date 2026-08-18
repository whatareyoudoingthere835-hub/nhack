package dev.nhack.client.command.commands;

import dev.nhack.client.command.Command;
import dev.nhack.client.command.CommandManager;
import dev.nhack.client.module.Module;
import dev.nhack.client.module.ModuleManager;
import dev.nhack.client.util.ChatUtil;
import dev.nhack.client.util.KeyUtil;
import org.lwjgl.glfw.GLFW;

public final class BindCommand extends Command {
	public BindCommand() {
		super("bind", "Binds a module to a key", "bind <module> <key|none>", "b");
	}

	@Override
	public void execute(String[] args) {
		if (args.length < 2) {
			ChatUtil.error("Usage: " + CommandManager.getPrefix() + getUsage());
			return;
		}

		Module module = ModuleManager.get(args[0]).orElse(null);
		if (module == null) {
			ChatUtil.error("Unknown module");
			return;
		}

		int key = KeyUtil.fromName(args[1]);
		if (key == GLFW.GLFW_KEY_UNKNOWN && !args[1].equalsIgnoreCase("none") && !args[1].equalsIgnoreCase("unbind")) {
			ChatUtil.error("Unknown key");
			return;
		}

		module.setBind(key);
		ChatUtil.info(module.getName() + " bound to " + KeyUtil.name(key));
	}
}
