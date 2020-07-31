package de.l0c4lh057.templatebot.commands.exceptions;

public class MissingPermissionsException extends CommandException {
	public MissingPermissionsException(String key) {
		super(key);
	}
}
