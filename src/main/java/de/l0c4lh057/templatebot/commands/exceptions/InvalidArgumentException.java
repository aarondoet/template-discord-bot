package de.l0c4lh057.templatebot.commands.exceptions;

import reactor.util.annotation.NonNull;

public class InvalidArgumentException extends CommandException {
	InvalidArgumentException(@NonNull String key, @NonNull Object... args) {
		super(key, args);
	}
}
