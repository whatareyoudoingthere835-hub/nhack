package dev.nhack.client.command.commands;

import dev.nhack.client.command.Command;
import dev.nhack.client.command.CommandManager;
import dev.nhack.client.util.ChatUtil;
import dev.nhack.client.util.FriendManager;

public final class FriendCommand extends Command {
	public FriendCommand() {
		super("friend", "Add or remove aura friends", "friend <add|del|list> [name]", "f");
	}

	@Override
	public void execute(String[] args) {
		if (args.length < 1) {
			ChatUtil.error("Usage: " + CommandManager.getPrefix() + getUsage());
			return;
		}

		switch (args[0].toLowerCase()) {
			case "list" -> {
				if (FriendManager.all().isEmpty()) {
					ChatUtil.info("Friend list is empty");
					return;
				}
				ChatUtil.info("Friends: " + String.join(", ", FriendManager.all()));
			}
			case "add" -> {
				if (args.length < 2) {
					ChatUtil.error("Usage: " + CommandManager.getPrefix() + "friend add <name>");
					return;
				}
				ChatUtil.info(FriendManager.add(args[1]) ? "Added " + args[1] : args[1] + " is already a friend");
			}
			case "del", "remove" -> {
				if (args.length < 2) {
					ChatUtil.error("Usage: " + CommandManager.getPrefix() + "friend del <name>");
					return;
				}
				ChatUtil.info(FriendManager.remove(args[1]) ? "Removed " + args[1] : args[1] + " is not a friend");
			}
			default -> ChatUtil.error("Usage: " + CommandManager.getPrefix() + getUsage());
		}
	}
}
