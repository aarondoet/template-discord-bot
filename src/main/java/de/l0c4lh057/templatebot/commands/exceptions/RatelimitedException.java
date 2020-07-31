package de.l0c4lh057.templatebot.commands.exceptions;

public class RatelimitedException extends CommandException {
	public RatelimitedException(String key, Object... args) {
		super(key, args);
	}
}
