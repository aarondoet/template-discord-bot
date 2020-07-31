package de.l0c4lh057.templatebot.commands.exceptions;

import de.l0c4lh057.templatebot.utils.BotUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommandException extends Exception {
	
	private static final Logger logger = LogManager.getLogger("CommandException");
	
	private final String key;
	private final Object[] args;
	public CommandException(String key, Object... args){
		this.key = key;
		this.args = args;
		// Exception added for stacktrace
		if(key == null) logger.warn("No message key passed to CommandException " + this.getClass().getName(), new Exception());
	}
	public String getErrorMessage(String language){
		if(key == null) return "No error message available";
		return BotUtils.getLanguageString(language, key, args);
	}

}
