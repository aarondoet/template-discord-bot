package de.l0c4lh057.templatebot.commands.exceptions;

import reactor.util.annotation.NonNull;

/**
 * A {@link CommandException} emitted by {@link reactor.core.publisher.Mono}s to indicate that the user did not provide
 * correct arguments when executing a {@link de.l0c4lh057.templatebot.commands.Command}
 */
public class InvalidArgumentException extends CommandException {
	InvalidArgumentException(@NonNull String key, @NonNull Object... args) {
		super(key, args);
	}
}
