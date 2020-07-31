package de.l0c4lh057.templatebot.commands.exceptions;

public class InvalidArgumentException extends CommandException {
	public InvalidArgumentException(String key, Object... args) {
		super(key, args);
	}
}
