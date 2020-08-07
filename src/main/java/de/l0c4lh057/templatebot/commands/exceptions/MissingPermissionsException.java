package de.l0c4lh057.templatebot.commands.exceptions;

import reactor.util.annotation.NonNull;

/**
 * A {@link CommandException} emitted by {@link reactor.core.publisher.Mono}s to state that the user does not have
 * the permissions to perform a certain action
 */
public class MissingPermissionsException extends CommandException {
	MissingPermissionsException(@NonNull String key, @NonNull Object... args) {
		super(key, args);
	}
}
