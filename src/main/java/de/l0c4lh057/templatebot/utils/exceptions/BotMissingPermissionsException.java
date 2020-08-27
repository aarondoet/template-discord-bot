package de.l0c4lh057.templatebot.utils.exceptions;

public class BotMissingPermissionsException extends BotException {
	BotMissingPermissionsException(String key, Object... args) {
		super(key, args);
	}
}
