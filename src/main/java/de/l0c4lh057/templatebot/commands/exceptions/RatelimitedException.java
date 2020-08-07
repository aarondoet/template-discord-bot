package de.l0c4lh057.templatebot.commands.exceptions;

import reactor.util.annotation.NonNull;

/**
 * A {@link CommandException} emitted by {@link reactor.core.publisher.Mono}s to indicate that the action the user
 * wanted to perform has a ratelimit that they already hit
 */
public class RatelimitedException extends CommandException {
	RatelimitedException(@NonNull String key, @NonNull Object... args) {
		super(key, args);
	}
}
