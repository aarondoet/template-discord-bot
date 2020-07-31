package de.l0c4lh057.templatebot.commands.exceptions;

public class NotExecutableException extends CommandException {
	public NotExecutableException(String key, Object... args) {
		super(key, args);
	}
}
