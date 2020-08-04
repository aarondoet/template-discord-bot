package de.l0c4lh057.templatebot.commands.exceptions;

import reactor.util.annotation.NonNull;

public class NotExecutableException extends CommandException {
	NotExecutableException(@NonNull String key, @NonNull Object... args) {
		super(key, args);
	}
}
