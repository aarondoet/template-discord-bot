package de.l0c4lh057.templatebot.commands.exceptions;

import de.l0c4lh057.templatebot.utils.BotUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.util.annotation.NonNull;

public class CommandException extends Exception {
	
	private static final Logger logger = LogManager.getLogger("CommandException");
	
	private final String key;
	private final Object[] args;
	
	CommandException(@NonNull String key, @NonNull Object... args){
		this.key = key;
		this.args = args;
	}
	
	/**
	 *
	 * @param language
	 * @return
	 */
	@NonNull
	public String getErrorMessage(@NonNull String language){
		return BotUtils.getLanguageString(language, key, args);
	}
	
	/**
	 *
	 * @param key
	 * @param args
	 * @return
	 */
	@NonNull
	public static InvalidArgumentException invalidArgument(@NonNull String key, @NonNull Object... args){
		return new InvalidArgumentException(key, args);
	}
	
	/**
	 *
	 * @param key
	 * @param args
	 * @return
	 */
	@NonNull
	public static MissingPermissionsException missingPermissions(@NonNull String key, @NonNull Object... args){
		return new MissingPermissionsException(key, args);
	}
	
	/**
	 *
	 * @param key
	 * @param args
	 * @return
	 */
	@NonNull
	public static NotExecutableException notExecutable(@NonNull String key, @NonNull Object... args){
		return new NotExecutableException(key, args);
	}
	
	/**
	 *
	 * @param key
	 * @param args
	 * @return
	 */
	@NonNull
	public static RatelimitedException ratelimited(@NonNull String key, @NonNull Object... args){
		return new RatelimitedException(key, args);
	}

}
