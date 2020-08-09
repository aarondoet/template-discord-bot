package de.l0c4lh057.templatebot.utils.exceptions;

import reactor.util.annotation.NonNull;

/**
 * A {@link BotException} emitted by {@link reactor.core.publisher.Mono}s to indicate that the
 * {@link de.l0c4lh057.templatebot.commands.Command} can not be executed for any reason.
 */
public class NotExecutableException extends BotException {
	NotExecutableException(@NonNull String key, @NonNull Object... args) {
		super(key, args);
	}
}
