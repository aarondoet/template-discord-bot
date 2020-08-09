package de.l0c4lh057.templatebot.commands.exceptions;

import de.l0c4lh057.templatebot.utils.BotUtils;
import reactor.util.annotation.NonNull;

public class CommandException extends Exception {
	
	private final String key;
	private final Object[] args;
	
	CommandException(@NonNull String key, @NonNull Object... args){
		this.key = key;
		this.args = args;
	}
	
	@NonNull
	public String getErrorMessage(@NonNull String language){
		return BotUtils.getLanguageString(language, key, args);
	}
	
	/**
	 * @param key  The key which should be used to get the language string from the {@link java.util.ResourceBundle}
	 * @param args The arguments used to format the plain language string
	 * @return The new {@link InvalidArgumentException}
	 */
	@NonNull
	public static InvalidArgumentException invalidArgument(@NonNull String key, @NonNull Object... args){
		return new InvalidArgumentException(key, args);
	}
	
	/**
	 * @param key  The key which should be used to get the language string from the {@link java.util.ResourceBundle}
	 * @param args The arguments used to format the plain language string
	 * @return The new {@link MissingPermissionsException}
	 */
	@NonNull
	public static MissingPermissionsException missingPermissions(@NonNull String key, @NonNull Object... args){
		return new MissingPermissionsException(key, args);
	}
	
	/**
	 * @param key  The key which should be used to get the language string from the {@link java.util.ResourceBundle}
	 * @param args The arguments used to format the plain language string
	 * @return The new {@link NotExecutableException}
	 */
	@NonNull
	public static NotExecutableException notExecutable(@NonNull String key, @NonNull Object... args){
		return new NotExecutableException(key, args);
	}
	
	/**
	 * @param key  The key which should be used to get the language string from the {@link java.util.ResourceBundle}
	 * @param args The arguments used to format the plain language string
	 * @return The new {@link RatelimitedException}
	 */
	@NonNull
	public static RatelimitedException ratelimited(@NonNull String key, @NonNull Object... args){
		return new RatelimitedException(key, args);
	}

}
